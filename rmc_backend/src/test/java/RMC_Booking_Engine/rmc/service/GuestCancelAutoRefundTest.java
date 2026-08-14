package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.service.MayaRefundService.RefundExecution;
import RMC_Booking_Engine.rmc.service.MayaRefundService.ReversalMethod;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.CancellationEvaluation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class GuestCancelAutoRefundTest {

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
    void guestCancel_paidMaya_initiatesReverseAndCompletes() {
        Booking booking = paidMayaBooking();
        stubPaidLedgerAndPolicy(new BigDecimal("1500.00"));
        when(mayaRefundService.isSamePaymentDay(any())).thenReturn(true);
        when(mayaRefundService.sumManualRefunds(any())).thenReturn(BigDecimal.ZERO);
        when(mayaRefundService.reversePayment(eq(booking), eq(new BigDecimal("1500.00")), anyString(), eq(true)))
                .thenReturn(new RefundExecution(new BigDecimal("1500.00"), "void-1", ReversalMethod.VOID));
        when(mayaRefundService.remainingRefundDue(eq(booking), any())).thenReturn(BigDecimal.ZERO);

        var result = cancellationService.cancelBooking(
                booking, null, null, "GUEST_CANCEL", "GUEST_CANCEL_REFUND_PENDING");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(result.auditTrigger()).isEqualTo("GUEST_CANCEL_VOID");
        assertThat(result.refundPending()).isFalse();
        verify(mayaRefundService).reversePayment(eq(booking), eq(new BigDecimal("1500.00")), anyString(), eq(true));
        verify(emailOutboxService).enqueueRefundProcessed(11L, new BigDecimal("1500.00"));
        verify(bookingHoldService).writeAuditLog(
                eq(booking), eq("CONFIRMED"), eq("CANCELLED"), eq("GUEST_CANCEL_VOID"), isNull(), isNull());
    }

    @Test
    void guestCancel_zeroEligible_doesNotCallMaya() {
        Booking booking = paidMayaBooking();
        stubPaidLedgerAndPolicy(BigDecimal.ZERO);

        var result = cancellationService.cancelBooking(
                booking, null, null, "GUEST_CANCEL", "GUEST_CANCEL_REFUND_PENDING");

        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.NOT_APPLICABLE);
        assertThat(result.refundPending()).isFalse();
        verify(mayaRefundService, never()).reversePayment(any(), any(), any(), anyBoolean());
        verify(emailOutboxService, never()).enqueueRefundProcessed(any(), any());
    }

    @Test
    void staffCancel_doesNotAutoReverseMaya() {
        Booking booking = paidMayaBooking();
        stubPaidLedgerAndPolicy(new BigDecimal("1500.00"));

        var result = cancellationService.cancelBooking(
                booking, "staff cancel", 9L, "STAFF_CANCEL", "STAFF_CANCEL_REFUND_PENDING");

        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(result.auditTrigger()).isEqualTo("STAFF_CANCEL_REFUND_PENDING");
        verify(mayaRefundService, never()).reversePayment(any(), any(), any(), anyBoolean());
    }

    private void stubPaidLedgerAndPolicy(BigDecimal refundEligible) {
        BookingLedger credit = new BookingLedger();
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(new BigDecimal("1500.00"));
        credit.setCreatedAt(Instant.now());
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(11L)).thenReturn(List.of(credit));
        when(mayaRefundService.mayaRefundableAmount(any())).thenReturn(new BigDecimal("1500.00"));
        when(refundPolicyService.evaluateCancellation(any(), any())).thenReturn(
                CancellationEvaluation.allowed(
                        refundEligible.compareTo(BigDecimal.ZERO) > 0
                                ? CancellationTier.FULL
                                : CancellationTier.NONE,
                        refundEligible,
                        new BigDecimal("1500.00").subtract(refundEligible),
                        refundEligible.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0,
                        "policy",
                        Instant.now(),
                        Instant.now()));
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
