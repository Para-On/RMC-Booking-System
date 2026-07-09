package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.StaffLoginAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.domain.enums.StaffLoginEvent;
import RMC_Booking_Engine.rmc.domain.enums.StaffLoginStatus;
import RMC_Booking_Engine.rmc.dto.PagedResponse;
import RMC_Booking_Engine.rmc.dto.StaffLoginAuditEntryDto;
import RMC_Booking_Engine.rmc.repository.StaffLoginAuditLogRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffLoginAuditService {

    private final StaffLoginAuditLogRepository staffLoginAuditLogRepository;

    @Transactional
    public void recordLoginSuccess(StaffUser user, String ipAddress) {
        StaffLoginAuditLog entry = baseEntry(user, user.getEmail(), StaffLoginEvent.LOGIN, StaffLoginStatus.SUCCESS, ipAddress);
        staffLoginAuditLogRepository.save(entry);
    }

    @Transactional
    public void recordLoginFailure(String email, String fullName, Long staffUserId, String reason, String ipAddress) {
        StaffLoginAuditLog entry = new StaffLoginAuditLog();
        entry.setStaffUserId(staffUserId);
        entry.setEmailAttempt(trimEmail(email));
        entry.setFullName(trimName(fullName));
        entry.setEventType(StaffLoginEvent.LOGIN);
        entry.setStatus(StaffLoginStatus.FAILED);
        entry.setIpAddress(ipAddress);
        entry.setFailureReason(trimReason(reason));
        staffLoginAuditLogRepository.save(entry);
    }

    @Transactional
    public void recordLogoutSuccess(StaffPrincipal staff, String ipAddress) {
        StaffLoginAuditLog entry = new StaffLoginAuditLog();
        entry.setStaffUserId(staff.id());
        entry.setEmailAttempt(staff.email());
        entry.setFullName(staff.fullName());
        entry.setEventType(StaffLoginEvent.LOGOUT);
        entry.setStatus(StaffLoginStatus.SUCCESS);
        entry.setIpAddress(ipAddress);
        staffLoginAuditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PagedResponse<StaffLoginAuditEntryDto> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<StaffLoginAuditLog> result = staffLoginAuditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<StaffLoginAuditEntryDto> content = result.getContent().stream()
                .map(this::toDto)
                .toList();
        return new PagedResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<StaffLoginAuditEntryDto> listRecent() {
        return list(0, 100).content();
    }

    private StaffLoginAuditLog baseEntry(
            StaffUser user,
            String email,
            StaffLoginEvent event,
            StaffLoginStatus status,
            String ipAddress) {
        StaffLoginAuditLog entry = new StaffLoginAuditLog();
        entry.setStaffUserId(user.getId());
        entry.setEmailAttempt(trimEmail(email));
        entry.setFullName(trimName(user.getFullName()));
        entry.setEventType(event);
        entry.setStatus(status);
        entry.setIpAddress(ipAddress);
        return entry;
    }

    private StaffLoginAuditEntryDto toDto(StaffLoginAuditLog entry) {
        return new StaffLoginAuditEntryDto(
                entry.getId(),
                entry.getCreatedAt(),
                entry.getFullName(),
                entry.getEmailAttempt(),
                entry.getEventType(),
                entry.getStatus(),
                entry.getIpAddress(),
                entry.getFailureReason());
    }

    private String trimEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimName(String fullName) {
        if (fullName == null) {
            return null;
        }
        String trimmed = fullName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 500) {
            return trimmed.substring(0, 500);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
