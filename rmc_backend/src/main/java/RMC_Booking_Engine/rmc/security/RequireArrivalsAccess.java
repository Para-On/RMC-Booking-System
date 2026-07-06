package RMC_Booking_Engine.rmc.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@staffNavAccessService.canAccess(authentication, '/staff/arrivals')")
public @interface RequireArrivalsAccess {
}
