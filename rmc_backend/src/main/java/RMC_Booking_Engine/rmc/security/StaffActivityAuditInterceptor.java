package RMC_Booking_Engine.rmc.security;

import RMC_Booking_Engine.rmc.domain.enums.StaffActivityAction;
import RMC_Booking_Engine.rmc.service.StaffActivityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class StaffActivityAuditInterceptor implements HandlerInterceptor {

    private final StaffActivityAuditService staffActivityAuditService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        if (ex != null || response.getStatus() >= 400) {
            return;
        }

        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/staff/")) {
            return;
        }
        if (uri.startsWith("/api/staff/auth/")
                || uri.startsWith("/api/staff/audit/")
                || uri.equals("/api/staff/nav")) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof StaffPrincipal staff)) {
            return;
        }

        StaffActivityAction action = mapAction(request.getMethod());
        if (action == null) {
            return;
        }

        staffActivityAuditService.record(staff, action, request.getMethod(), uri);
    }

    private StaffActivityAction mapAction(String method) {
        if (method == null) {
            return null;
        }
        return switch (method.toUpperCase()) {
            case "GET", "HEAD" -> StaffActivityAction.VIEW;
            case "POST" -> StaffActivityAction.ADD;
            case "PUT", "PATCH" -> StaffActivityAction.EDIT;
            case "DELETE" -> StaffActivityAction.DELETE;
            default -> null;
        };
    }
}
