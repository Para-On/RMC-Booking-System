package RMC_Booking_Engine.rmc.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryGuestBookingRateLimiterTest {

    private final InMemoryGuestBookingRateLimiter rateLimiter = new InMemoryGuestBookingRateLimiter();

    @Test
    void allowsRequestsWithinLimit() {
        assertTrue(rateLimiter.tryConsume("1.2.3.4", 2));
        assertTrue(rateLimiter.tryConsume("1.2.3.4", 2));
        assertFalse(rateLimiter.tryConsume("1.2.3.4", 2));
    }

    @Test
    void tracksClientsIndependently() {
        assertTrue(rateLimiter.tryConsume("1.2.3.4", 1));
        assertTrue(rateLimiter.tryConsume("5.6.7.8", 1));
    }
}
