package RMC_Booking_Engine.rmc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String signingSecret,
        int accessTokenMinutes,
        int refreshTokenDays,
        int lockoutThreshold,
        int lockoutMinutes) {
}
