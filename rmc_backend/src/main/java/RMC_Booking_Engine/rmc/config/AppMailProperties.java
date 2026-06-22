package RMC_Booking_Engine.rmc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(
        boolean enabled,
        String from) {
}
