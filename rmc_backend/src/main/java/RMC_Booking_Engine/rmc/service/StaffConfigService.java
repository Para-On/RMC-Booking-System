package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.entity.SystemConfig;
import RMC_Booking_Engine.rmc.dto.ManagerConfigResponse;
import RMC_Booking_Engine.rmc.dto.RatePlanConfigDto;
import RMC_Booking_Engine.rmc.dto.RoomTypeConfigDto;
import RMC_Booking_Engine.rmc.dto.SystemConfigItemDto;
import RMC_Booking_Engine.rmc.dto.UpdateRatePlanRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomTypeRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import RMC_Booking_Engine.rmc.repository.SystemConfigRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final RatePlanRepository ratePlanRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ConfigurationAuditLogRepository configurationAuditLogRepository;

    @Transactional(readOnly = true)
    public ManagerConfigResponse getConfig() {
        List<SystemConfigItemDto> systemConfig = systemConfigRepository.findAll().stream()
                .sorted((a, b) -> a.getConfigKey().compareToIgnoreCase(b.getConfigKey()))
                .map(c -> new SystemConfigItemDto(c.getConfigKey(), c.getConfigValue(), c.getDescription()))
                .toList();

        List<RatePlanConfigDto> ratePlans = ratePlanRepository.findAllWithRoomType().stream()
                .map(rp -> new RatePlanConfigDto(
                        rp.getId(),
                        rp.getRoomType().getId(),
                        rp.getRoomType().getName(),
                        rp.getName(),
                        rp.getCancellationPolicy(),
                        rp.getRefundWindowHours(),
                        rp.getHoldTtlMinutes(),
                        rp.getPayLaterCutoffHours(),
                        Boolean.TRUE.equals(rp.getActive())))
                .toList();

        List<RoomTypeConfigDto> roomTypes = roomTypeRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(rt -> new RoomTypeConfigDto(
                        rt.getId(),
                        rt.getName(),
                        rt.getTotalCapacity(),
                        rt.getOverbookingBuffer(),
                        rt.getMinAdvanceBookingHours(),
                        rt.getMaxAdvanceBookingDays(),
                        Boolean.TRUE.equals(rt.getActive())))
                .toList();

        return new ManagerConfigResponse(systemConfig, ratePlans, roomTypes);
    }

    @Transactional
    public SystemConfigItemDto updateSystemConfig(String key, String value, StaffPrincipal staff) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException("Configuration key not found: " + key));
        String previous = config.getConfigValue();
        config.setConfigValue(value.trim());
        systemConfigRepository.save(config);
        recordAudit("SYSTEM_CONFIG", config.getId(), key, previous, value.trim(), staff.id());
        return new SystemConfigItemDto(config.getConfigKey(), config.getConfigValue(), config.getDescription());
    }

    @Transactional
    public RatePlanConfigDto updateRatePlan(Long id, UpdateRatePlanRequest request, StaffPrincipal staff) {
        RatePlan ratePlan = ratePlanRepository.findByIdWithRoomType(id)
                .orElseThrow(() -> new BusinessException("Rate plan not found"));

        if (request.cancellationPolicy() != null) {
            recordAudit("RATE_PLAN", id, "cancellationPolicy",
                    ratePlan.getCancellationPolicy(), request.cancellationPolicy(), staff.id());
            ratePlan.setCancellationPolicy(request.cancellationPolicy());
        }
        if (request.refundWindowHours() != null) {
            recordAudit("RATE_PLAN", id, "refundWindowHours",
                    String.valueOf(ratePlan.getRefundWindowHours()),
                    String.valueOf(request.refundWindowHours()), staff.id());
            ratePlan.setRefundWindowHours(request.refundWindowHours());
        }
        if (request.holdTtlMinutes() != null) {
            recordAudit("RATE_PLAN", id, "holdTtlMinutes",
                    String.valueOf(ratePlan.getHoldTtlMinutes()),
                    String.valueOf(request.holdTtlMinutes()), staff.id());
            ratePlan.setHoldTtlMinutes(request.holdTtlMinutes());
        }
        if (request.payLaterCutoffHours() != null) {
            recordAudit("RATE_PLAN", id, "payLaterCutoffHours",
                    String.valueOf(ratePlan.getPayLaterCutoffHours()),
                    String.valueOf(request.payLaterCutoffHours()), staff.id());
            ratePlan.setPayLaterCutoffHours(request.payLaterCutoffHours());
        }
        if (request.active() != null) {
            recordAudit("RATE_PLAN", id, "active",
                    String.valueOf(ratePlan.getActive()), String.valueOf(request.active()), staff.id());
            ratePlan.setActive(request.active());
        }

        ratePlanRepository.save(ratePlan);
        return new RatePlanConfigDto(
                ratePlan.getId(),
                ratePlan.getRoomType().getId(),
                ratePlan.getRoomType().getName(),
                ratePlan.getName(),
                ratePlan.getCancellationPolicy(),
                ratePlan.getRefundWindowHours(),
                ratePlan.getHoldTtlMinutes(),
                ratePlan.getPayLaterCutoffHours(),
                Boolean.TRUE.equals(ratePlan.getActive()));
    }

    @Transactional
    public RoomTypeConfigDto updateRoomType(Long id, UpdateRoomTypeRequest request, StaffPrincipal staff) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room type not found"));

        if (request.overbookingBuffer() != null) {
            recordAudit("ROOM_TYPE", id, "overbookingBuffer",
                    String.valueOf(roomType.getOverbookingBuffer()),
                    String.valueOf(request.overbookingBuffer()), staff.id());
            roomType.setOverbookingBuffer(request.overbookingBuffer());
        }
        if (request.minAdvanceBookingHours() != null) {
            recordAudit("ROOM_TYPE", id, "minAdvanceBookingHours",
                    String.valueOf(roomType.getMinAdvanceBookingHours()),
                    String.valueOf(request.minAdvanceBookingHours()), staff.id());
            roomType.setMinAdvanceBookingHours(request.minAdvanceBookingHours());
        }
        if (request.maxAdvanceBookingDays() != null) {
            recordAudit("ROOM_TYPE", id, "maxAdvanceBookingDays",
                    String.valueOf(roomType.getMaxAdvanceBookingDays()),
                    String.valueOf(request.maxAdvanceBookingDays()), staff.id());
            roomType.setMaxAdvanceBookingDays(request.maxAdvanceBookingDays());
        }
        if (request.active() != null) {
            recordAudit("ROOM_TYPE", id, "active",
                    String.valueOf(roomType.getActive()), String.valueOf(request.active()), staff.id());
            roomType.setActive(request.active());
        }

        roomTypeRepository.save(roomType);
        return new RoomTypeConfigDto(
                roomType.getId(),
                roomType.getName(),
                roomType.getTotalCapacity(),
                roomType.getOverbookingBuffer(),
                roomType.getMinAdvanceBookingHours(),
                roomType.getMaxAdvanceBookingDays(),
                Boolean.TRUE.equals(roomType.getActive()));
    }

    private void recordAudit(
            String entityType,
            Long entityId,
            String configKey,
            String oldValue,
            String newValue,
            Long staffUserId) {
        if (oldValue != null && oldValue.equals(newValue)) {
            return;
        }
        ConfigurationAuditLog log = new ConfigurationAuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setConfigKey(configKey);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setStaffUserId(staffUserId);
        log.setCreatedAt(Instant.now());
        configurationAuditLogRepository.save(log);
    }
}
