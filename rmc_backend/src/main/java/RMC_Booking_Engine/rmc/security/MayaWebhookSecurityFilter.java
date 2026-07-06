package RMC_Booking_Engine.rmc.security;

import RMC_Booking_Engine.rmc.service.MayaWebhookSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class MayaWebhookSecurityFilter extends OncePerRequestFilter {

    private final MayaWebhookSecurityService webhookSecurityService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/payments/maya/webhook".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        byte[] body = request.getInputStream().readAllBytes();
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, body);

        if (!webhookSecurityService.isAllowed(cachedRequest, body)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Webhook rejected\"}");
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }
}
