package RMC_Booking_Engine.rmc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(
        boolean enabled,
        String from,
        int maxAttempts,
        int retryBaseMinutes) {

    public AppMailProperties {
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }
        if (retryBaseMinutes <= 0) {
            retryBaseMinutes = 1;
        }
    }
}
