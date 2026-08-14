package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.obs.OpsAlertSignals;
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
class MayaAmountMismatchTest {

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
    private OpsAlertSignals opsAlertSignals;

    @Mock
    private MayaPaymentEvidenceService mayaPaymentEvidenceService;

    @InjectMocks
    private MayaPaymentService mayaPaymentService;

    private Booking pending;

    @BeforeEach
    void setUp() {
        pending = pendingMaya("RMC-MISMATCH-1", "chk-mismatch-1", new BigDecimal("2128.00"));
        when(additionalChargeService.tryHandleMayaPayload(any(), any())).thenReturn(false);
    }

    @Test
    void webhookSuccess_wrongAmount_doesNotMarkPaid() {
        MayaCheckoutStatus payload = success("chk-mismatch-1", "RMC-MISMATCH-1", new BigDecimal("1.00"));
        when(bookingRepository.findByReferenceForUpdate("RMC-MISMATCH-1")).thenReturn(Optional.of(pending));

        mayaPaymentService.handleWebhookPayload(payload);

        assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(bookingRepository, never()).save(any());
        verify(bookingLedgerRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(opsAlertSignals).webhookFailure("RMC-MISMATCH-1", "amount mismatch");
        verify(bookingHoldService, never()).writeAuditLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void lateWebhookSuccess_wrongAmount_doesNotConfirm() {
        pending.setStatus(BookingStatus.FAILED);
        MayaCheckoutStatus payload = success("chk-mismatch-1", "RMC-MISMATCH-1", new BigDecimal("9999.00"));
        when(bookingRepository.findByReferenceForUpdate("RMC-MISMATCH-1")).thenReturn(Optional.of(pending));

        mayaPaymentService.handleWebhookPayload(payload);

        assertThat(pending.getStatus()).isEqualTo(BookingStatus.FAILED);
        verify(bookingHoldService, never()).restoreHolds(any());
        verify(opsAlertSignals).webhookFailure("RMC-MISMATCH-1", "late amount mismatch");
    }

    private static MayaCheckoutStatus success(String checkoutId, String reference, BigDecimal amount) {
        return new MayaCheckoutStatus(
                checkoutId, null, "COMPLETED", "PAYMENT_SUCCESS", amount, null, "PHP", reference);
    }

    private static Booking pendingMaya(String reference, String checkoutId, BigDecimal quoted) {
        Booking booking = new Booking();
        booking.setId(31L);
        booking.setReference(reference);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        booking.setMayaCheckoutId(checkoutId);
        booking.setQuotedTotal(quoted);
        booking.setCurrency("PHP");
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        booking.setRoomType(roomType);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(12));
        return booking;
    }
}
