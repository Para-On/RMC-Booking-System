package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.config.MayaProperties;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.exception.BusinessException;
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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class MayaPaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingLedgerRepository bookingLedgerRepository;

    @Mock
    private BookingHoldService bookingHoldService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MayaCheckoutClient mayaCheckoutClient;

    @Mock
    private MayaProperties mayaProperties;

    @Mock
    private MayaRefundService mayaRefundService;

    @Mock
    private BookingRefundPolicySnapshotService bookingRefundPolicySnapshotService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MayaPaymentService mayaPaymentService;

    private Booking failedMayaBooking;

    @BeforeEach
    void setUp() {
        RoomType roomType = new RoomType();
        roomType.setId(1L);

        failedMayaBooking = new Booking();
        failedMayaBooking.setId(10L);
        failedMayaBooking.setReference("RMC-LATE-1");
        failedMayaBooking.setStatus(BookingStatus.FAILED);
        failedMayaBooking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        failedMayaBooking.setMayaCheckoutId("checkout-late-1");
        failedMayaBooking.setQuotedTotal(new BigDecimal("1500.00"));
        failedMayaBooking.setCurrency("PHP");
        failedMayaBooking.setRoomType(roomType);
        failedMayaBooking.setCheckInDate(LocalDate.now().plusDays(7));
        failedMayaBooking.setCheckOutDate(LocalDate.now().plusDays(9));

        lenient()
                .when(bookingRefundPolicySnapshotService.attachSnapshotIfAbsent(any(), any()))
                .thenReturn(null);
    }

    @Test
    void handleWebhookPayload_latePayment_confirmsWhenRoomAvailable() {
        MayaCheckoutStatus payload = new MayaCheckoutStatus(
                "checkout-late-1",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                new BigDecimal("1500.00"),
                null,
                "PHP",
                "RMC-LATE-1");

        when(bookingRepository.findByReferenceForUpdate("RMC-LATE-1"))
                .thenReturn(Optional.of(failedMayaBooking));
        when(bookingLedgerRepository.existsByIdempotencyKey("maya-credit-checkout-late-1")).thenReturn(false);
        when(bookingLedgerRepository.existsByBookingIdAndEntryType(10L, LedgerEntryType.CREDIT))
                .thenReturn(false);

        mayaPaymentService.handleWebhookPayload(payload);

        assertThat(failedMayaBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingHoldService).restoreHolds(failedMayaBooking);
        verify(bookingHoldService).writeAuditLog(
                eq(failedMayaBooking),
                eq(BookingStatus.FAILED.name()),
                eq(BookingStatus.CONFIRMED.name()),
                eq("LATE_PAYMENT_CONFIRM"),
                eq(null),
                eq("MAYA_WEBHOOK"));
        verify(mayaRefundService, never()).executeRefund(any(), any(), any());
    }

    @Test
    void handleWebhookPayload_latePayment_refundsWhenRoomUnavailable() {
        MayaCheckoutStatus payload = new MayaCheckoutStatus(
                "checkout-late-1",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                new BigDecimal("1500.00"),
                null,
                "PHP",
                "RMC-LATE-1");

        when(bookingRepository.findByReferenceForUpdate("RMC-LATE-1"))
                .thenReturn(Optional.of(failedMayaBooking));
        when(bookingLedgerRepository.existsByIdempotencyKey("maya-credit-checkout-late-1")).thenReturn(false);
        when(bookingLedgerRepository.existsByBookingIdAndEntryType(10L, LedgerEntryType.CREDIT))
                .thenReturn(false);
        doThrow(new BusinessException("No availability for the selected dates"))
                .when(bookingHoldService).restoreHolds(failedMayaBooking);

        mayaPaymentService.handleWebhookPayload(payload);

        assertThat(failedMayaBooking.getStatus()).isEqualTo(BookingStatus.FAILED);
        verify(mayaRefundService).executeRefund(
                eq(failedMayaBooking),
                eq(null),
                eq("Late payment - room no longer available"));
        verify(bookingHoldService).writeAuditLog(
                eq(failedMayaBooking),
                eq(BookingStatus.FAILED.name()),
                eq(BookingStatus.FAILED.name()),
                eq("LATE_PAYMENT_REFUND"),
                eq(null),
                any());
    }

    @Test
    void confirmPaymentByReference_alreadyConfirmed_isIdempotent() {
        failedMayaBooking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByReferenceForUpdate("RMC-LATE-1"))
                .thenReturn(Optional.of(failedMayaBooking));

        mayaPaymentService.confirmPaymentByReference("RMC-LATE-1");

        verify(mayaCheckoutClient, never()).getCheckout(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void handleWebhookPayload_duplicateLedgerEntry_stillConfirmsBooking() {
        MayaCheckoutStatus payload = new MayaCheckoutStatus(
                "checkout-pending-1",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                new BigDecimal("1500.00"),
                null,
                "PHP",
                "RMC-PENDING-1");

        Booking pendingBooking = new Booking();
        pendingBooking.setId(11L);
        pendingBooking.setReference("RMC-PENDING-1");
        pendingBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        pendingBooking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);
        pendingBooking.setMayaCheckoutId("checkout-pending-1");
        pendingBooking.setQuotedTotal(new BigDecimal("1500.00"));
        pendingBooking.setCurrency("PHP");
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        pendingBooking.setRoomType(roomType);
        pendingBooking.setCheckInDate(LocalDate.now().plusDays(3));
        pendingBooking.setCheckOutDate(LocalDate.now().plusDays(5));

        when(bookingRepository.findByReferenceForUpdate("RMC-PENDING-1"))
                .thenReturn(Optional.of(pendingBooking));
        when(bookingLedgerRepository.existsByIdempotencyKey("maya-credit-checkout-pending-1")).thenReturn(false);
        when(bookingLedgerRepository.existsByBookingIdAndEntryType(11L, LedgerEntryType.CREDIT))
                .thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(bookingLedgerRepository).saveAndFlush(any(BookingLedger.class));

        mayaPaymentService.handleWebhookPayload(payload);

        assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(entityManager).detach(any(BookingLedger.class));
    }
}
