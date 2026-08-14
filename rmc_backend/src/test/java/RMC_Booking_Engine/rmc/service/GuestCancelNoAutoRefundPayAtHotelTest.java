package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
class GuestCancelNoAutoRefundPayAtHotelTest {

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
    void guestCancel_payAtHotel_doesNotCallMaya() {
        Booking booking = new Booking();
        booking.setId(22L);
        booking.setReference("RMC-20260814-2002");
        booking.setStatus(BookingStatus.CONFIRMED_PAY_LATER);
        booking.setPaymentMethod(PaymentMethod.PAY_AT_HOTEL);
        booking.setCurrency("PHP");

        BookingLedger credit = new BookingLedger();
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(new BigDecimal("800.00"));
        credit.setCreatedAt(Instant.now());
        when(bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(22L)).thenReturn(List.of(credit));
        when(mayaRefundService.mayaRefundableAmount(any())).thenReturn(new BigDecimal("800.00"));
        when(refundPolicyService.evaluateCancellation(any(), any())).thenReturn(
                CancellationEvaluation.allowed(
                        CancellationTier.FULL,
                        new BigDecimal("800.00"),
                        BigDecimal.ZERO,
                        100,
                        "policy",
                        Instant.now(),
                        Instant.now()));

        var result = cancellationService.cancelBooking(
                booking, null, null, "GUEST_CANCEL", "GUEST_CANCEL_REFUND_PENDING");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(result.auditTrigger()).isEqualTo("GUEST_CANCEL_REFUND_PENDING");
        verify(mayaRefundService, never()).reversePayment(any(), any(), any(), anyBoolean());
    }
}
