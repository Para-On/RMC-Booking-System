package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.config.AppMailProperties;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.EmailOutbox;
import RMC_Booking_Engine.rmc.domain.entity.Guest;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.EmailKind;
import RMC_Booking_Engine.rmc.domain.enums.EmailOutboxStatus;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.EmailOutboxRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceTest {

    @Mock
    private EmailOutboxRepository emailOutboxRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ObjectProvider<org.springframework.mail.javamail.JavaMailSender> mailSenderProvider;

    private EmailOutboxService emailOutboxService;

    @BeforeEach
    void setUp() {
        emailOutboxService = new EmailOutboxService(
                emailOutboxRepository,
                bookingRepository,
                new AppMailProperties(true, "noreply@rmc.test", 5, 1),
                mailSenderProvider);
    }

    @Test
    void enqueueBookingReceived_queuesReferenceEmailWhenEnabled() {
        Booking booking = sampleBooking(BookingStatus.PENDING_PAYMENT, PaymentMethod.ONLINE_MAYA);
        when(emailOutboxRepository.existsByBookingIdAndEmailKind(11L, EmailKind.BOOKING_RECEIVED))
                .thenReturn(false);
        when(bookingRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(booking));

        emailOutboxService.enqueueBookingReceived(11L);

        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(emailOutboxRepository).save(captor.capture());
        EmailOutbox entry = captor.getValue();
        assertThat(entry.getEmailKind()).isEqualTo(EmailKind.BOOKING_RECEIVED);
        assertThat(entry.getRecipient()).isEqualTo("guest@example.com");
        assertThat(entry.getSubject()).contains("RMC-20260813-1001");
        assertThat(entry.getBody()).contains("Booking reference: RMC-20260813-1001");
        assertThat(entry.getBody()).contains("Pending payment");
        assertThat(entry.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
    }

    @Test
    void enqueueBookingReceived_payAtHotelMentionsAwaitingApproval() {
        Booking booking = sampleBooking(BookingStatus.PENDING_APPROVAL, PaymentMethod.PAY_AT_HOTEL);
        when(emailOutboxRepository.existsByBookingIdAndEmailKind(11L, EmailKind.BOOKING_RECEIVED))
                .thenReturn(false);
        when(bookingRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(booking));

        emailOutboxService.enqueueBookingReceived(11L);

        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(emailOutboxRepository).save(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Awaiting hotel approval");
    }

    @Test
    void enqueueBookingReceived_skipsWhenAlreadyQueued() {
        when(emailOutboxRepository.existsByBookingIdAndEmailKind(11L, EmailKind.BOOKING_RECEIVED))
                .thenReturn(true);

        emailOutboxService.enqueueBookingReceived(11L);

        verify(bookingRepository, never()).findByIdWithDetails(any());
        verify(emailOutboxRepository, never()).save(any());
    }

    @Test
    void enqueueBookingReceived_skipsWhenMailDisabled() {
        emailOutboxService = new EmailOutboxService(
                emailOutboxRepository,
                bookingRepository,
                new AppMailProperties(false, "noreply@rmc.test", 5, 1),
                mailSenderProvider);

        emailOutboxService.enqueueBookingReceived(11L);

        verify(emailOutboxRepository, never()).existsByBookingIdAndEmailKind(any(), any());
        verify(emailOutboxRepository, never()).save(any());
    }

    private static Booking sampleBooking(BookingStatus status, PaymentMethod paymentMethod) {
        Guest guest = new Guest();
        guest.setFullName("Ada Guest");
        guest.setEmail("guest@example.com");

        RoomType roomType = new RoomType();
        roomType.setName("Deluxe Room");

        Booking booking = new Booking();
        booking.setId(11L);
        booking.setReference("RMC-20260813-1001");
        booking.setGuest(guest);
        booking.setRoomType(roomType);
        booking.setCheckInDate(LocalDate.of(2026, 8, 20));
        booking.setCheckOutDate(LocalDate.of(2026, 8, 22));
        booking.setCurrency("PHP");
        booking.setQuotedTotal(new BigDecimal("7600.00"));
        booking.setPaymentMethod(paymentMethod);
        booking.setStatus(status);
        return booking;
    }
}
