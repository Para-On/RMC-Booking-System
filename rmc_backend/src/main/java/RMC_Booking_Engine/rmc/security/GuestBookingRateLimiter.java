package RMC_Booking_Engine.rmc.security;

public interface GuestBookingRateLimiter {

    boolean tryConsume(String clientKey, int limitPerMinute);
}
