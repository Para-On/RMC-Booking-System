package RMC_Booking_Engine.rmc.config;

import RMC_Booking_Engine.rmc.security.StaffActivityAuditInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class StaffAuditWebConfig implements WebMvcConfigurer {

    private final StaffActivityAuditInterceptor staffActivityAuditInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(staffActivityAuditInterceptor).addPathPatterns("/api/staff/**");
    }
}
