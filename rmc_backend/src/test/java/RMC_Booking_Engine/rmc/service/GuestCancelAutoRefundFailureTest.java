package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.service.MayaRefundService.MayaRefundTiming;
import RMC_Booking_Engine.rmc.service.MayaRefundService.RefundExecution;
import RMC_Booking_Engine.rmc.service.MayaRefundService.ReversalMethod;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.CancellationEvaluation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class GuestCancelAutoRefundFailureTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingLedgerRepository bookingLedgerRepository;
    @Mock
    private BookingHoldService bookingHoldService;
    @Mock
    private RefundPolicyService refundPolicyService;
    @Mock
    private MayaRefundService mayaRefundService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PromoCodeService promoCodeService;
    @Mock
    private EmailOutboxService emailOutboxService;

    private BookingCancellationService cancellationService;

    @BeforeEach
    void setUp() {
        cancellationService = new BookingCancellationService(
                bookingRepository,
                bookingLedgerRepository,
                bookingHoldService,
                refundPolicyService,
                mayaRefundService,
                eventPublisher,
                promoCodeService,
                emailOutboxService);
    }

    @Test
    void guestCancel_mayaHardFailure_staysCancelledAndFailed() {
        Booking booking = paidMayaBooking();
        stubPaidPolicy();
        when(mayaRefundService.isSamePaymentDay(any())).thenReturn(true);
        when(mayaRefundService.sumManualRefunds(any())).thenReturn(BigDecimal.ZERO);
        when(mayaRefundService.reversePayment(any(), any(), anyString(), eq(true)))
                .thenThrow(new BusinessException("Maya checkout not found"));

        var result = cancellationService.cancelBooking(
                booking, null, null, "GUEST_CANCEL", "GUEST_CANCEL_REFUND_PENDING");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.FAILED);
        assertThat(result.auditTrigger()).isEqualTo("GUEST_CANCEL_REFUND_FAILED");
        assertThat(result.refundPending()).isTrue();
        verify(bookingHoldService).writeAuditLog(
                eq(booking),
                eq("CONFIRMED"),
                eq("CANCELLED"),
                eq("GUEST_CANCEL_REFUND_FAILED"),
                eq(null),
                eq("Maya checkout not found"));
    }

    @Test
    void guestCancel_sameDayCutoff_staysPendingForRetry() {
        Booking booking = paidMayaBooking();
        stubPaidPolicy();
        when(mayaRefundService.isSamePaymentDay(any())).thenReturn(true);
        when(mayaRefundService.sumManualRefunds(any())).thenReturn(BigDecimal.ZERO);
        when(mayaRefundService.reversePayment(any(), any(), anyString(), eq(true)))
                .thenThrow(new BusinessException(
                        "Maya void is not available for this payment, and API refunds are only allowed after "
                                + "Aug 15, 2026 12:00 AM (Asia/Manila). Leave the booking as PENDING and retry then."));

        var result = cancellationService.cancelBooking(
                booking, null, null, "GUEST_CANCEL", "GUEST_CANCEL_REFUND_PENDING");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(result.auditTrigger()).isEqualTo("GUEST_CANCEL_REFUND_PENDING");
    }

    @Test
    void retryQueuedMayaRefund_completesAfterCutoff() {
        Booking booking = paidMayaBooking();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setRefundStatus(RefundStatus.PENDING);
        booking.setRefundEligibleAmount(new BigDecimal("1500.00"));
        booking.setCancellationTier(CancellationTier.FULL);
        BookingLedger credit = credit();
        when(bookingRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(booking));
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(11L)).thenReturn(List.of(credit));
        when(mayaRefundService.remainingRefundDue(eq(booking), any())).thenReturn(new BigDecimal("1500.00"), BigDecimal.ZERO);
        when(mayaRefundService.mayaRefundableAmount(any())).thenReturn(new BigDecimal("1500.00"));
        when(mayaRefundService.assessRefundTiming(any(), any(), any())).thenReturn(
                new MayaRefundTiming(true, null, null, ReversalMethod.REFUND));
        when(mayaRefundService.isSamePaymentDay(any())).thenReturn(false);
        when(mayaRefundService.reversePayment(eq(booking), eq(new BigDecimal("1500.00")), anyString(), eq(false)))
                .thenReturn(new RefundExecution(new BigDecimal("1500.00"), "rf-1", ReversalMethod.REFUND));

        assertThat(cancellationService.retryQueuedMayaRefund(11L)).isTrue();
        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        verify(emailOutboxService).enqueueRefundProcessed(11L, new BigDecimal("1500.00"));
    }

    private void stubPaidPolicy() {
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(11L)).thenReturn(List.of(credit()));
        when(mayaRefundService.mayaRefundableAmount(any())).thenReturn(new BigDecimal("1500.00"));
        when(refundPolicyService.evaluateCancellation(any(), any())).thenReturn(
                CancellationEvaluation.allowed(
                        CancellationTier.FULL,
                        new BigDecimal("1500.00"),
                        BigDecimal.ZERO,
                        100,
                        "policy",
                        Instant.now(),
                        Instant.now()));
    }

    private static BookingLedger credit() {
        BookingLedger credit = new BookingLedger();
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(new BigDecimal("1500.00"));
        credit.setCreatedAt(Instant.now());
        return credit;
    }

    private static Booking paidMayaBooking() {
        Booking booking = new Booking();
        booking.setId(11L);
        booking.setReference("RMC-20260814-1001");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        booking.setMayaCheckoutId("chk-1");
        booking.setCurrency("PHP");
        return booking;
    }
}
