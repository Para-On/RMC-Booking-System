package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.MayaProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MayaWebhookSecurityService {

    private static final String HMAC_HEADER = "X-Webhook-Signature";
    private static final String SHARED_SECRET_HEADER = "X-Webhook-Token";

    private final MayaProperties mayaProperties;

    public boolean isAllowed(HttpServletRequest request, byte[] rawBody) {
        MayaProperties.Webhook webhook = mayaProperties.webhook();

        if (webhook.ipVerifyEnabled() && !isAllowedIp(request, webhook.allowedIps())) {
            return false;
        }

        if (webhook.sharedSecret() != null && !webhook.sharedSecret().isBlank()) {
            String token = request.getHeader(SHARED_SECRET_HEADER);
            if (token == null || !constantTimeEquals(token.trim(), webhook.sharedSecret().trim())) {
                return false;
            }
        }

        if (webhook.hmacSecret() != null && !webhook.hmacSecret().isBlank()) {
            String signature = request.getHeader(HMAC_HEADER);
            if (signature == null || signature.isBlank()) {
                return false;
            }
            String expected = hmacSha256Hex(webhook.hmacSecret(), rawBody);
            if (expected == null || !constantTimeEquals(signature.trim(), expected)) {
                return false;
            }
        }

        return true;
    }

    private boolean isAllowedIp(HttpServletRequest request, String allowedIps) {
        if (allowedIps == null || allowedIps.isBlank()) {
            return false;
        }
        Set<String> allowed = Arrays.stream(allowedIps.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isBlank())
                .collect(Collectors.toSet());

        String clientIp = resolveClientIp(request);
        return allowed.contains(clientIp);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String hmacSha256Hex(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }
}
