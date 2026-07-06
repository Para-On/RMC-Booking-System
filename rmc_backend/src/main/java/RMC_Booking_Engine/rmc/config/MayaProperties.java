package RMC_Booking_Engine.rmc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "maya")
public record MayaProperties(
        String publicKey,
        String secretKey,
        String apiBaseUrl,
        String frontendBaseUrl,
        Webhook webhook) {

    public MayaProperties {
        if (webhook == null) {
            webhook = new Webhook(false, "", "", "");
        }
    }

    public boolean isConfigured() {
        return publicKey != null && !publicKey.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && apiBaseUrl != null && !apiBaseUrl.isBlank();
    }

    public record Webhook(
            boolean ipVerifyEnabled,
            String allowedIps,
            String sharedSecret,
            String hmacSecret) {
    }
}
