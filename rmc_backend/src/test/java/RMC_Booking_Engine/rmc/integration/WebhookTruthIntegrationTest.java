package RMC_Booking_Engine.rmc.integration;

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
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.service.AdditionalChargeService;
import RMC_Booking_Engine.rmc.service.BookingHoldService;
import RMC_Booking_Engine.rmc.service.MayaCheckoutClient;
import RMC_Booking_Engine.rmc.service.MayaPaymentEvidenceService;
import RMC_Booking_Engine.rmc.service.MayaPaymentService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Browser confirm-payment polls Maya; it must not invent paid state from the redirect
 * ({@code RMC-SPEC-ARCH-001.1}, {@code RMC-SPEC-PAY-002.1}).
 */
@ExtendWith(MockitoExtension.class)
class WebhookTruthIntegrationTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingHoldService bookingHoldService;
    @Mock
    private MayaCheckoutClient mayaCheckoutClient;
    @Mock
    private AdditionalChargeService additionalChargeService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private BookingLedgerRepository bookingLedgerRepository;
    @Mock
    private MayaPaymentEvidenceService mayaPaymentEvidenceService;

    @InjectMocks
    private MayaPaymentService mayaPaymentService;

    private Booking pending;

    @BeforeEach
    void setUp() {
        pending = pendingMaya();
    }

    @Test
    void confirmPaymentPoll_whenMayaStillPending_doesNotMarkPaid() {
        when(bookingRepository.findByReferenceForUpdate("RMC-TRUTH-1")).thenReturn(Optional.of(pending));
        when(mayaCheckoutClient.getCheckout("chk-truth-1")).thenReturn(new MayaCheckoutStatus(
                "chk-truth-1",
                null,
                "PENDING",
                "PAYMENT_PENDING",
                new BigDecimal("1500.00"),
                null,
                "PHP",
                "RMC-TRUTH-1"));
        when(additionalChargeService.tryHandleMayaPayload(any(), any())).thenReturn(false);

        mayaPaymentService.confirmPaymentByReference("RMC-TRUTH-1");

        assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(bookingRepository, never()).save(any());
        verify(bookingHoldService, never()).writeAuditLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmPaymentPoll_whenMayaUnreachable_doesNotInventPaid() {
        pending.setExpiresAt(Instant.now().plusSeconds(600));
        when(bookingRepository.findByReferenceForUpdate("RMC-TRUTH-1")).thenReturn(Optional.of(pending));
        when(mayaCheckoutClient.getCheckout("chk-truth-1"))
                .thenThrow(new BusinessException("Unable to verify Maya checkout: 502"));

        mayaPaymentService.confirmPaymentByReference("RMC-TRUTH-1");

        assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(bookingRepository, never()).save(any());
    }

    private static Booking pendingMaya() {
        Booking booking = new Booking();
        booking.setId(51L);
        booking.setReference("RMC-TRUTH-1");
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        booking.setMayaCheckoutId("chk-truth-1");
        booking.setQuotedTotal(new BigDecimal("1500.00"));
        booking.setCurrency("PHP");
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        booking.setRoomType(roomType);
        booking.setCheckInDate(LocalDate.now().plusDays(8));
        booking.setCheckOutDate(LocalDate.now().plusDays(10));
        booking.setExpiresAt(Instant.now().plusSeconds(900));
        return booking;
    }
}
