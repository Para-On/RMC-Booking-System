package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MayaWebhookIdempotencyTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingLedgerRepository bookingLedgerRepository;
    @Mock
    private BookingHoldService bookingHoldService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AdditionalChargeService additionalChargeService;
    @Mock
    private BookingRefundPolicySnapshotService bookingRefundPolicySnapshotService;
    @Mock
    private EntityManager entityManager;

    @Mock
    private MayaPaymentEvidenceService mayaPaymentEvidenceService;

    @InjectMocks
    private MayaPaymentService mayaPaymentService;

    private Booking pending;

    @BeforeEach
    void setUp() {
        pending = pendingMaya();
        when(additionalChargeService.tryHandleMayaPayload(any(), any())).thenReturn(false);
    }

    @Test
    void duplicateWebhook_onAlreadyPaidBooking_doesNotCreditAgain() {
        pending.setStatus(BookingStatus.PENDING_APPROVAL);
        MayaCheckoutStatus payload = success();
        when(bookingRepository.findByReferenceForUpdate("RMC-IDEM-1")).thenReturn(Optional.of(pending));

        mayaPaymentService.handleWebhookPayload(payload);
        mayaPaymentService.handleWebhookPayload(payload);

        verify(bookingLedgerRepository, never()).saveAndFlush(any(BookingLedger.class));
        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING_APPROVAL);
    }

    @Test
    void secondWebhook_afterFirstSuccess_doesNotDoubleApply() {
        MayaCheckoutStatus payload = success();
        when(bookingRepository.findByReferenceForUpdate("RMC-IDEM-1")).thenReturn(Optional.of(pending));
        when(bookingLedgerRepository.existsByIdempotencyKey("maya-credit-chk-idem-1")).thenReturn(false);
        when(bookingLedgerRepository.existsByBookingIdAndEntryType(41L, LedgerEntryType.CREDIT)).thenReturn(false);

        mayaPaymentService.handleWebhookPayload(payload);
        assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING_APPROVAL);

        mayaPaymentService.handleWebhookPayload(payload);

        verify(bookingLedgerRepository, times(1)).saveAndFlush(any(BookingLedger.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
        verify(bookingHoldService, times(1)).writeAuditLog(
                eq(pending),
                eq(BookingStatus.PENDING_PAYMENT.name()),
                eq(BookingStatus.PENDING_APPROVAL.name()),
                eq("MAYA_WEBHOOK"),
                eq(null),
                eq(null));
    }

    private static MayaCheckoutStatus success() {
        return new MayaCheckoutStatus(
                "chk-idem-1",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                new BigDecimal("1500.00"),
                null,
                "PHP",
                "RMC-IDEM-1");
    }

    private static Booking pendingMaya() {
        Booking booking = new Booking();
        booking.setId(41L);
        booking.setReference("RMC-IDEM-1");
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        booking.setMayaCheckoutId("chk-idem-1");
        booking.setQuotedTotal(new BigDecimal("1500.00"));
        booking.setCurrency("PHP");
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        booking.setRoomType(roomType);
        booking.setCheckInDate(LocalDate.now().plusDays(4));
        booking.setCheckOutDate(LocalDate.now().plusDays(6));
        return booking;
    }
}
