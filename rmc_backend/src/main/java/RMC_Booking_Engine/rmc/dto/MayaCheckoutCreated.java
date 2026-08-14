package RMC_Booking_Engine.rmc.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MayaCheckoutCreated(
        @JsonAlias("id") String checkoutId,
        String redirectUrl) {

    /** Prefer explicit checkoutId; else parse {@code id} from Maya redirectUrl. */
    public String resolvedCheckoutId() {
        if (checkoutId != null && !checkoutId.isBlank()) {
            return checkoutId.trim();
        }
        return extractIdFromRedirectUrl(redirectUrl);
    }

    static String extractIdFromRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return null;
        }
        try {
            String query = URI.create(redirectUrl).getQuery();
            if (query == null || query.isBlank()) {
                return null;
            }
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                if ("id".equalsIgnoreCase(key)) {
                    String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    return value.isBlank() ? null : value;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
