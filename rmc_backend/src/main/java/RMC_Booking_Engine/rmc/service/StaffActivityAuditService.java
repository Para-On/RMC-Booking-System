package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.StaffActivityAuditLog;
import RMC_Booking_Engine.rmc.domain.enums.StaffActivityAction;
import RMC_Booking_Engine.rmc.dto.PagedResponse;
import RMC_Booking_Engine.rmc.dto.StaffActivityAuditEntryDto;
import RMC_Booking_Engine.rmc.repository.StaffActivityAuditLogRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.util.StaffModuleInfo;
import RMC_Booking_Engine.rmc.util.StaffModuleResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffActivityAuditService {

    private final StaffActivityAuditLogRepository staffActivityAuditLogRepository;

    @Transactional
    public void record(StaffPrincipal staff, StaffActivityAction action, String requestMethod, String requestPath) {
        StaffModuleInfo module = StaffModuleResolver.resolve(requestPath);
        StaffActivityAuditLog entry = new StaffActivityAuditLog();
        entry.setStaffUserId(staff.id());
        entry.setFullName(staff.fullName());
        entry.setRole(staff.role().name());
        entry.setActionType(action);
        entry.setModuleKey(module.key());
        entry.setModuleLabel(module.label());
        entry.setRequestMethod(requestMethod);
        entry.setRequestPath(trimPath(requestPath));
        staffActivityAuditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PagedResponse<StaffActivityAuditEntryDto> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<StaffActivityAuditLog> result = staffActivityAuditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<StaffActivityAuditEntryDto> content = result.getContent().stream()
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
    public List<StaffActivityAuditEntryDto> listRecent() {
        return list(0, 100).content();
    }

    private StaffActivityAuditEntryDto toDto(StaffActivityAuditLog entry) {
        return new StaffActivityAuditEntryDto(
                entry.getId(),
                entry.getCreatedAt(),
                entry.getFullName(),
                entry.getRole(),
                entry.getActionType(),
                entry.getModuleKey(),
                entry.getModuleLabel(),
                entry.getRequestMethod(),
                entry.getRequestPath());
    }

    private String trimPath(String requestPath) {
        if (requestPath == null) {
            return null;
        }
        return requestPath.length() > 500 ? requestPath.substring(0, 500) : requestPath;
    }
}
