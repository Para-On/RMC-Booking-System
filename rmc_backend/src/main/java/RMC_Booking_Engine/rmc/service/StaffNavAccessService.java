package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.StaffNavModule;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.repository.StaffNavModuleRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.util.JsonStringListConverter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("staffNavAccessService")
@RequiredArgsConstructor
public class StaffNavAccessService {

    private final StaffNavModuleRepository staffNavModuleRepository;

    @Transactional(readOnly = true)
    public boolean canAccess(Authentication authentication, String navPath) {
        if (authentication == null || !(authentication.getPrincipal() instanceof StaffPrincipal staff)) {
            return false;
        }
        return canAccessPath(staff.role().name(), navPath);
    }

    @Transactional(readOnly = true)
    public boolean canAccessAny(Authentication authentication, String... navPaths) {
        if (navPaths == null) {
            return false;
        }
        for (String navPath : navPaths) {
            if (canAccess(authentication, navPath)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean canAccessPath(String staffRole, String navPath) {
        if (StaffRole.ADMIN.name().equals(staffRole)) {
            return true;
        }
        Optional<StaffNavModule> module = staffNavModuleRepository.findByPathAndEnabledTrue(navPath.trim());
        if (module.isEmpty()) {
            return false;
        }
        List<String> allowedRoles = JsonStringListConverter.fromJson(module.get().getAllowedRoles());
        return allowedRoles.contains(staffRole);
    }
}
