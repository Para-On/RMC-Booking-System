package RMC_Booking_Engine.rmc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maya")
public record MayaProperties(
        String publicKey,
        String secretKey,
        String apiBaseUrl,
        String frontendBaseUrl) {

    public boolean isConfigured() {
        return publicKey != null && !publicKey.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && apiBaseUrl != null && !apiBaseUrl.isBlank();
    }
}
