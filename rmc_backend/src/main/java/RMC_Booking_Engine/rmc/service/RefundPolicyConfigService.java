package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.dto.RefundPolicyConfigDto;
import RMC_Booking_Engine.rmc.dto.UpdateRefundPolicyRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.RefundPolicyRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundPolicyConfigService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final RefundPolicyRepository refundPolicyRepository;
    private final ConfigurationAuditLogRepository configurationAuditLogRepository;
    private final ConfigService configService;

    @Transactional(readOnly = true)
    public RefundPolicyConfigDto getActivePolicy() {
        return refundPolicyRepository.findFirstByActiveTrueOrderByIdAsc()
                .map(this::toDto)
                .orElse(RefundPolicyConfigDto.empty());
    }

    @Transactional
    public RefundPolicyConfigDto updateActivePolicy(UpdateRefundPolicyRequest request, StaffPrincipal staff) {
        RefundPolicy policy = refundPolicyRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseGet(this::createDefaultPolicy);

        auditFieldChange(policy, "name", policy.getName(), request.name(), staff.id());
        auditFieldChange(policy, "enabled", policy.getEnabled(), request.enabled(), staff.id());
        auditFieldChange(policy, "fullCutoffValue", policy.getFullCutoffValue(), request.fullCutoffValue(), staff.id());
        auditFieldChange(
                policy, "fullCutoffUnit", policy.getFullCutoffUnit().name(), request.fullCutoffUnit(), staff.id());
        auditFieldChange(policy, "partialEnabled", policy.getPartialEnabled(), request.partialEnabled(), staff.id());
        auditFieldChange(
                policy,
                "partialRefundPercent",
                policy.getPartialRefundPercent(),
                request.partialRefundPercent(),
                staff.id());
        auditFieldChange(policy, "checkInTime", formatTime(policy.getCheckInTime()), request.checkInTime(), staff.id());
        auditFieldChange(policy, "timezone", policy.getTimezone(), request.timezone(), staff.id());
        auditFieldChange(policy, "description", policy.getDescription(), request.description(), staff.id());

        policy.setName(request.name().trim());
        policy.setEnabled(request.enabled());
        policy.setFullCutoffValue(request.fullCutoffValue());
        policy.setFullCutoffUnit(CutoffUnit.fromString(request.fullCutoffUnit()));
        policy.setPartialEnabled(request.partialEnabled());
        policy.setPartialRefundPercent(request.partialRefundPercent());
        policy.setCheckInTime(LocalTime.parse(request.checkInTime(), TIME_FORMAT));
        policy.setTimezone(request.timezone().trim());
        policy.setDescription(request.description() != null ? request.description().trim() : null);
        policy.setUpdatedAt(Instant.now());

        return toDto(refundPolicyRepository.save(policy));
    }

    private RefundPolicy createDefaultPolicy() {
        RefundPolicy policy = new RefundPolicy();
        policy.setName("Default hotel policy");
        policy.setActive(true);
        return refundPolicyRepository.save(policy);
    }

    private RefundPolicyConfigDto toDto(RefundPolicy policy) {
        int refundPercent = policy.getPartialRefundPercent() != null ? policy.getPartialRefundPercent() : 0;
        return new RefundPolicyConfigDto(
                policy.getId(),
                policy.getName(),
                Boolean.TRUE.equals(policy.getEnabled()),
                policy.getFullCutoffValue() != null ? policy.getFullCutoffValue() : 0,
                policy.getFullCutoffUnit() != null ? policy.getFullCutoffUnit().name() : CutoffUnit.HOURS.name(),
                Boolean.TRUE.equals(policy.getPartialEnabled()),
                refundPercent,
                100 - refundPercent,
                formatTime(policy.getCheckInTime()),
                policy.getTimezone(),
                policy.getDescription(),
                configService.isManualRefundEnabled());
    }

    private void auditFieldChange(
            RefundPolicy policy, String key, Object oldValue, Object newValue, Long staffUserId) {
        String oldString = oldValue != null ? String.valueOf(oldValue) : null;
        String newString = newValue != null ? String.valueOf(newValue) : null;
        if (oldString != null && oldString.equals(newString)) {
            return;
        }
        ConfigurationAuditLog log = new ConfigurationAuditLog();
        log.setEntityType("REFUND_POLICY");
        log.setEntityId(policy.getId());
        log.setConfigKey(key);
        log.setOldValue(oldString);
        log.setNewValue(newString);
        log.setStaffUserId(staffUserId);
        log.setCreatedAt(Instant.now());
        configurationAuditLogRepository.save(log);
    }

    private static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMAT) : "14:00";
    }

    public void assertValidTimezone(String timezone) {
        try {
            java.time.ZoneId.of(timezone);
        } catch (Exception ex) {
            throw new BusinessException("Invalid timezone: " + timezone);
        }
    }
}
