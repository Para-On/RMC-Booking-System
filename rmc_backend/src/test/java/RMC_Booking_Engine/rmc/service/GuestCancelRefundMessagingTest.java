package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GuestCancelRefundMessagingTest {

    @Test
    void pendingRefund_doesNotClaimProcessed() {
        Booking booking = cancelled(RefundStatus.PENDING, new BigDecimal("1500.00"));
        String message = BookingService.cancellationMessage(booking);
        assertThat(message).contains("not complete yet");
        assertThat(message).doesNotContain("has been processed");
        assertThat(message).contains("1500.00");
    }

    @Test
    void failedRefund_doesNotClaimProcessed() {
        Booking booking = cancelled(RefundStatus.FAILED, new BigDecimal("1500.00"));
        String message = BookingService.cancellationMessage(booking);
        assertThat(message).contains("failed");
        assertThat(message).doesNotContain("has been processed");
    }

    @Test
    void completedRefund_mayClaimProcessed() {
        Booking booking = cancelled(RefundStatus.COMPLETED, new BigDecimal("1500.00"));
        assertThat(BookingService.cancellationMessage(booking)).contains("has been processed");
    }

    private static Booking cancelled(RefundStatus status, BigDecimal amount) {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setRefundStatus(status);
        booking.setRefundEligibleAmount(amount);
        booking.setCurrency("PHP");
        return booking;
    }
}
