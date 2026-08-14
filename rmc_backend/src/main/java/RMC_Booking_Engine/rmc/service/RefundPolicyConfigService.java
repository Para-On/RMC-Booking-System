package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.dto.RefundPolicyConfigDto;
import RMC_Booking_Engine.rmc.dto.UpdateRefundPolicyRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RefundPolicyRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundPolicyConfigService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final RefundPolicyRepository refundPolicyRepository;
    private final RatePlanRepository ratePlanRepository;
    private final ConfigurationAuditLogRepository configurationAuditLogRepository;
    private final ConfigService configService;

    @Transactional(readOnly = true)
    public List<RefundPolicyConfigDto> listPolicies() {
        return refundPolicyRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    /** @deprecated Prefer listPolicies; kept for callers expecting a single default. */
    @Transactional(readOnly = true)
    public RefundPolicyConfigDto getActivePolicy() {
        return refundPolicyRepository.findFirstByActiveTrueOrderByIdAsc()
                .map(this::toDto)
                .orElse(RefundPolicyConfigDto.empty());
    }

    @Transactional
    public RefundPolicyConfigDto createPolicy(UpdateRefundPolicyRequest request, StaffPrincipal staff) {
        assertValidTimezone(request.timezone());
        String name = request.name().trim();
        if (refundPolicyRepository.existsByNameIgnoreCaseAndActiveTrue(name)) {
            throw new BusinessException("A refund policy named \"" + name + "\" already exists");
        }
        RefundPolicy policy = new RefundPolicy();
        policy.setActive(true);
        applyRequest(policy, request);
        policy = refundPolicyRepository.save(policy);
        auditFieldChange(policy, "created", null, policy.getName(), staff.id());
        return toDto(policy);
    }

    @Transactional
    public RefundPolicyConfigDto updatePolicy(Long id, UpdateRefundPolicyRequest request, StaffPrincipal staff) {
        assertValidTimezone(request.timezone());
        RefundPolicy policy = refundPolicyRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new BusinessException("Refund policy not found"));
        String name = request.name().trim();
        if (refundPolicyRepository.existsByNameIgnoreCaseAndActiveTrueAndIdNot(name, id)) {
            throw new BusinessException("A refund policy named \"" + name + "\" already exists");
        }
        auditFieldChange(policy, "name", policy.getName(), name, staff.id());
        auditFieldChange(policy, "enabled", policy.getEnabled(), request.enabled(), staff.id());
        auditFieldChange(policy, "fullCutoffValue", policy.getFullCutoffValue(), request.fullCutoffValue(), staff.id());
        auditFieldChange(
                policy, "fullCutoffUnit", policy.getFullCutoffUnit().name(), request.fullCutoffUnit(), staff.id());
        auditFieldChange(policy, "partialEnabled", policy.getPartialEnabled(), request.partialEnabled(), staff.id());
        auditFieldChange(
                policy, "partialRefundPercent", policy.getPartialRefundPercent(), request.partialRefundPercent(), staff.id());
        auditFieldChange(
                policy,
                "nightsDeductionEnabled",
                policy.getNightsDeductionEnabled(),
                request.nightsDeductionEnabled(),
                staff.id());
        auditFieldChange(policy, "nightsDeducted", policy.getNightsDeducted(), request.nightsDeducted(), staff.id());
        auditFieldChange(policy, "refundable", policy.getRefundable(), request.refundable(), staff.id());
        applyRequest(policy, request);
        RefundPolicy saved = refundPolicyRepository.save(policy);
        syncLinkedRatePlans(saved);
        return toDto(saved);
    }

    /** @deprecated Prefer updatePolicy(id, …). */
    @Transactional
    public RefundPolicyConfigDto updateActivePolicy(UpdateRefundPolicyRequest request, StaffPrincipal staff) {
        RefundPolicy policy = refundPolicyRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseGet(this::createDefaultPolicy);
        return updatePolicy(policy.getId(), request, staff);
    }

    @Transactional
    public void deactivatePolicy(Long id, StaffPrincipal staff) {
        RefundPolicy policy = refundPolicyRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new BusinessException("Refund policy not found"));
        long linked = ratePlanRepository.countByRefundPolicyIdAndActiveTrue(id);
        if (linked > 0) {
            throw new BusinessException(
                    "Cannot deactivate policy while " + linked + " active rate plan(s) still reference it");
        }
        policy.setActive(false);
        policy.setUpdatedAt(Instant.now());
        refundPolicyRepository.save(policy);
        auditFieldChange(policy, "active", "true", "false", staff.id());
    }

    private void applyRequest(RefundPolicy policy, UpdateRefundPolicyRequest request) {
        policy.setName(request.name().trim());
        policy.setEnabled(request.enabled());
        policy.setFullCutoffValue(request.fullCutoffValue());
        policy.setFullCutoffUnit(CutoffUnit.fromString(request.fullCutoffUnit()));
        policy.setPartialEnabled(request.partialEnabled());
        policy.setPartialRefundPercent(request.partialRefundPercent());
        policy.setNightsDeductionEnabled(Boolean.TRUE.equals(request.nightsDeductionEnabled()));
        policy.setNightsDeducted(request.nightsDeducted() != null ? request.nightsDeducted() : 1);
        policy.setCheckInTime(LocalTime.parse(request.checkInTime(), TIME_FORMAT));
        policy.setTimezone(request.timezone().trim());
        String description = request.description() != null ? request.description().trim() : "";
        if (description.isEmpty()) {
            throw new BusinessException("Policy description is required");
        }
        policy.setDescription(description);
        policy.setRefundable(Boolean.TRUE.equals(request.refundable()));
        policy.setUpdatedAt(Instant.now());
    }

    private void syncLinkedRatePlans(RefundPolicy policy) {
        ratePlanRepository.findByRefundPolicyIdAndActiveTrue(policy.getId()).forEach(plan -> {
            copyPolicyOntoRatePlan(plan, policy);
            ratePlanRepository.save(plan);
        });
    }

    /** Keeps denormalized rate_plan columns in sync for guest badges / legacy readers. */
    public static void copyPolicyOntoRatePlan(
            RMC_Booking_Engine.rmc.domain.entity.RatePlan plan, RefundPolicy policy) {
        plan.setPolicyEnabled(Boolean.TRUE.equals(policy.getEnabled()));
        plan.setFullCutoffValue(policy.getFullCutoffValue());
        plan.setFullCutoffUnit(policy.getFullCutoffUnit());
        plan.setPartialEnabled(Boolean.TRUE.equals(policy.getPartialEnabled()));
        plan.setPartialRefundPercent(policy.getPartialRefundPercent());
        plan.setCheckInTime(policy.getCheckInTime());
        plan.setTimezone(policy.getTimezone());
        plan.setPolicyDescription(policy.getDescription());
        plan.setCancellationPolicy(policy.getDescription());
        plan.setRefundable(Boolean.TRUE.equals(policy.getRefundable()));
        if (policy.getFullCutoffUnit() != null && policy.getFullCutoffValue() != null
                && policy.getFullCutoffUnit() == CutoffUnit.HOURS) {
            plan.setRefundWindowHours(policy.getFullCutoffValue());
        }
        plan.setLateCancelRefundPercent(policy.getPartialRefundPercent());
        plan.setAllowLateCancellation(Boolean.TRUE.equals(policy.getPartialEnabled()));
    }

    private RefundPolicy createDefaultPolicy() {
        RefundPolicy policy = new RefundPolicy();
        policy.setName("Default hotel policy");
        policy.setActive(true);
        policy.setRefundable(true);
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
                Boolean.TRUE.equals(policy.getNightsDeductionEnabled()),
                policy.getNightsDeducted() != null ? policy.getNightsDeducted() : 1,
                formatTime(policy.getCheckInTime()),
                policy.getTimezone(),
                policy.getDescription(),
                Boolean.TRUE.equals(policy.getRefundable()),
                Boolean.TRUE.equals(policy.getActive()),
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
