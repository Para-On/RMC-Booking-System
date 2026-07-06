package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.dto.CreateStaffUserRequest;
import RMC_Booking_Engine.rmc.dto.StaffUserDto;
import RMC_Booking_Engine.rmc.dto.UpdateStaffUserRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffUserService {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffUserDto> listUsers() {
        return staffUserRepository.findAllByOrderByEmailAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public StaffUserDto createUser(CreateStaffUserRequest request, StaffPrincipal actor) {
        String email = request.email().trim().toLowerCase();
        if (staffUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BusinessException("A staff account with this email already exists");
        }
        if (request.password().length() < 8) {
            throw new BusinessException("Password must be at least 8 characters");
        }
        assertRoleAssignment(actor, request.role());

        StaffUser user = new StaffUser();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return toDto(staffUserRepository.save(user));
    }

    @Transactional
    public StaffUserDto updateUser(Long id, UpdateStaffUserRequest request, StaffPrincipal actor) {
        StaffUser user = findUser(id);
        if (user.getId().equals(actor.id()) && Boolean.FALSE.equals(request.active())) {
            throw new BusinessException("You cannot disable your own account");
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.role() != null) {
            if (user.getId().equals(actor.id()) && request.role() != user.getRole()) {
                throw new BusinessException("You cannot change your own role");
            }
            assertRoleAssignment(actor, request.role());
            user.setRole(request.role());
        }
        return toDto(staffUserRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword, StaffPrincipal actor) {
        if (newPassword.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters");
        }
        StaffUser user = findUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        staffUserRepository.save(user);
    }

    private StaffUser findUser(Long id) {
        return staffUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Staff user not found"));
    }

    private void assertRoleAssignment(StaffPrincipal actor, StaffRole targetRole) {
        if (targetRole == StaffRole.ADMIN && actor.role() != StaffRole.ADMIN) {
            throw new BusinessException("Only administrators can assign the admin role");
        }
    }

    private StaffUserDto toDto(StaffUser user) {
        return new StaffUserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.isMfaEnabled(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
