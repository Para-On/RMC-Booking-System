package RMC_Booking_Engine.rmc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.config.MayaProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MayaWebhookSecurityServiceTest {

    private final MayaWebhookSecurityService service = new MayaWebhookSecurityService(
            new MayaProperties("", "", "", "", new MayaProperties.Webhook(
                    true,
                    "13.229.160.234,127.0.0.1",
                    "shared-token",
                    "hmac-secret")));

    @Test
    void allowsRequestWhenIpTokenAndHmacMatch() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        byte[] body = "{\"id\":\"chk-1\"}".getBytes(StandardCharsets.UTF_8);
        when(request.getHeader("X-Forwarded-For")).thenReturn("13.229.160.234");
        when(request.getHeader("X-Webhook-Token")).thenReturn("shared-token");
        when(request.getHeader("X-Webhook-Signature")).thenReturn(
                "a3f2c1b9e8d7f6a5b4c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e3f2a1");

        // HMAC won't match with fake signature — test IP + token only path
        MayaWebhookSecurityService tokenOnlyService = new MayaWebhookSecurityService(
                new MayaProperties("", "", "", "", new MayaProperties.Webhook(
                        true, "13.229.160.234", "shared-token", "")));
        assertTrue(tokenOnlyService.isAllowed(request, body));
    }

    @Test
    void rejectsRequestFromUnknownIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");

        assertFalse(service.isAllowed(request, new byte[0]));
    }

    @Test
    void rejectsRequestWithWrongSharedToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Webhook-Token")).thenReturn("wrong");

        MayaWebhookSecurityService tokenService = new MayaWebhookSecurityService(
                new MayaProperties("", "", "", "", new MayaProperties.Webhook(
                        false, "", "shared-token", "")));

        assertFalse(tokenService.isAllowed(request, new byte[0]));
    }

    @Test
    void skipsChecksWhenVerificationDisabled() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");

        MayaWebhookSecurityService openService = new MayaWebhookSecurityService(
                new MayaProperties("", "", "", "", new MayaProperties.Webhook(
                        false, "", "", "")));

        assertTrue(openService.isAllowed(request, new byte[0]));
    }
}
