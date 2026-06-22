package RMC_Booking_Engine.rmc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        int guestBookingsPerMinute) {
}
