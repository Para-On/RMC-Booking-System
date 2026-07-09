package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.domain.enums.StaffThemePreference;
import RMC_Booking_Engine.rmc.dto.ChangeStaffPasswordRequest;
import RMC_Booking_Engine.rmc.dto.StaffProfileResponse;
import RMC_Booking_Engine.rmc.dto.UpdateStaffProfileRequest;
import RMC_Booking_Engine.rmc.dto.UpdateStaffThemeRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StaffProfileService {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrandingStorageService brandingStorageService;

    @Transactional(readOnly = true)
    public StaffProfileResponse getProfile(Long staffId) {
        return toResponse(findUser(staffId));
    }

    @Transactional
    public StaffProfileResponse updateProfile(Long staffId, UpdateStaffProfileRequest request) {
        StaffUser user = findUser(staffId);
        user.setFullName(request.fullName().trim());
        String phone = request.phone();
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        return toResponse(staffUserRepository.save(user));
    }

    @Transactional
    public StaffProfileResponse updateTheme(Long staffId, UpdateStaffThemeRequest request) {
        StaffUser user = findUser(staffId);
        user.setThemePreference(request.themePreference());
        return toResponse(staffUserRepository.save(user));
    }

    @Transactional
    public StaffProfileResponse uploadAvatar(Long staffId, MultipartFile file) {
        StaffUser user = findUser(staffId);
        String imageUrl = brandingStorageService.storeStaffProfileImage(file);
        user.setProfileImageUrl(imageUrl);
        return toResponse(staffUserRepository.save(user));
    }

    @Transactional
    public void changePassword(Long staffId, ChangeStaffPasswordRequest request) {
        if (!request.newPassword().equals(request.newPassword().trim())) {
            throw new BusinessException("Password cannot start or end with spaces");
        }
        StaffUser user = findUser(staffId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        staffUserRepository.save(user);
    }

    private StaffUser findUser(Long staffId) {
        return staffUserRepository
                .findById(staffId)
                .orElseThrow(() -> new BusinessException("Staff account not found"));
    }

    private StaffProfileResponse toResponse(StaffUser user) {
        StaffThemePreference theme =
                user.getThemePreference() != null ? user.getThemePreference() : StaffThemePreference.LIGHT;
        return new StaffProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getRole(),
                theme,
                user.isMfaEnabled());
    }
}
