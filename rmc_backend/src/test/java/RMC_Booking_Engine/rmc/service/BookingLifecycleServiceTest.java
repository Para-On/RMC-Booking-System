package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingLedgerRepository bookingLedgerRepository;

    @Mock
    private InventoryHoldRepository inventoryHoldRepository;

    @Mock
    private BookingAuditLogRepository bookingAuditLogRepository;

    @Mock
    private MayaCheckoutClient mayaCheckoutClient;

    @Mock
    private MayaPaymentService mayaPaymentService;

    @Mock
    private BookingCancellationService bookingCancellationService;

    @InjectMocks
    private BookingHoldService bookingHoldService;

    private BookingLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new BookingLifecycleService(
                bookingRepository,
                bookingLedgerRepository,
                bookingHoldService,
                bookingCancellationService,
                mayaCheckoutClient,
                mayaPaymentService);
    }

    @Test
    void expirePendingPayments_marksExpiredBookingsAsFailed() {
        Booking booking = pendingBooking();
        booking.setMayaCheckoutId(null);
        when(bookingRepository.findExpiredByStatus(any(), any())).thenReturn(List.of(booking));
        when(inventoryHoldRepository.findByBookingIdAndStatus(1L, HoldStatus.ACTIVE)).thenReturn(List.of());

        int count = lifecycleService.expirePendingPayments();

        assertThat(count).isEqualTo(1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.FAILED);
        verify(bookingRepository).save(booking);
        verify(bookingAuditLogRepository).save(any());
    }

    @Test
    void expirePendingPayments_extendsHoldWhenMayaCheckoutStillOpen() {
        Booking booking = pendingBooking();
        booking.setMayaCheckoutId("checkout-open");
        when(bookingRepository.findExpiredByStatus(any(), any())).thenReturn(List.of(booking));
        when(mayaCheckoutClient.getCheckout("checkout-open")).thenReturn(new MayaCheckoutStatus(
                "checkout-open",
                null,
                "PENDING",
                null,
                new BigDecimal("1000.00"),
                null,
                "PHP",
                booking.getReference()));
        when(inventoryHoldRepository.findByBookingIdAndStatus(1L, HoldStatus.ACTIVE)).thenReturn(List.of());

        int count = lifecycleService.expirePendingPayments();

        assertThat(count).isZero();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getExpiresAt()).isAfter(Instant.now());
        verify(mayaPaymentService, never()).handleWebhookPayload(any());
    }

    @Test
    void expirePendingPayments_reconcilesSuccessfulMayaPayment() {
        Booking booking = pendingBooking();
        booking.setMayaCheckoutId("checkout-paid");
        MayaCheckoutStatus paid = new MayaCheckoutStatus(
                "checkout-paid",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                new BigDecimal("1000.00"),
                null,
                "PHP",
                booking.getReference());
        when(bookingRepository.findExpiredByStatus(any(), any())).thenReturn(List.of(booking));
        when(mayaCheckoutClient.getCheckout("checkout-paid")).thenReturn(paid);

        int count = lifecycleService.expirePendingPayments();

        assertThat(count).isZero();
        verify(mayaPaymentService).handleWebhookPayload(paid);
        verify(bookingAuditLogRepository, never()).save(any());
    }

    @Test
    void markNoShows_onlyAppliesToConfirmedBookings() {
        Booking confirmed = confirmedBooking();
        when(bookingRepository.findUncheckedInBefore(any(), any(LocalDate.class)))
                .thenReturn(List.of(confirmed));
        when(bookingLedgerRepository.existsByIdempotencyKey("noshow-RMC-TEST")).thenReturn(false);

        int count = lifecycleService.markNoShows();

        assertThat(count).isEqualTo(1);
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        verify(bookingLedgerRepository).save(any());
    }

    @Test
    void markNoShows_skipsPayLaterBookings() {
        Booking payLater = payLaterBooking();
        when(bookingRepository.findUncheckedInBefore(any(), any(LocalDate.class)))
                .thenReturn(List.of(payLater));

        int count = lifecycleService.markNoShows();

        assertThat(count).isZero();
        assertThat(payLater.getStatus()).isEqualTo(BookingStatus.CONFIRMED_PAY_LATER);
        verify(bookingLedgerRepository, never()).save(any());
    }

    private Booking pendingBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setReference("RMC-PENDING");
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        return booking;
    }

    private Booking confirmedBooking() {
        Booking booking = new Booking();
        booking.setId(2L);
        booking.setReference("RMC-TEST");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().minusDays(1));
        return booking;
    }

    private Booking payLaterBooking() {
        Booking booking = new Booking();
        booking.setId(3L);
        booking.setReference("RMC-PAYLATER");
        booking.setStatus(BookingStatus.CONFIRMED_PAY_LATER);
        booking.setCheckInDate(LocalDate.now().minusDays(1));
        return booking;
    }
}
