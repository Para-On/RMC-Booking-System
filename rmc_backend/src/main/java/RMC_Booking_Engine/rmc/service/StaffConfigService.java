package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.dto.CreateRatePlanRequest;
import RMC_Booking_Engine.rmc.dto.CreateRoomNumberRequest;
import RMC_Booking_Engine.rmc.dto.CreateRoomTypeRequest;
import RMC_Booking_Engine.rmc.dto.CreateRoomTypeResponse;
import RMC_Booking_Engine.rmc.dto.ConfigurationAuditEntryDto;
import RMC_Booking_Engine.rmc.dto.CreateRoomUnitRequest;
import RMC_Booking_Engine.rmc.dto.ManagerConfigResponse;
import RMC_Booking_Engine.rmc.dto.RatePlanConfigDto;
import RMC_Booking_Engine.rmc.dto.RoomConfigOptionDto;
import RMC_Booking_Engine.rmc.dto.RoomConfigOptionsResponse;
import RMC_Booking_Engine.rmc.dto.CreateRoomConfigOptionRequest;
import RMC_Booking_Engine.rmc.dto.RoomNumberDto;
import RMC_Booking_Engine.rmc.dto.RoomTypeConfigDto;
import RMC_Booking_Engine.rmc.dto.RoomTypeDeleteResult;
import RMC_Booking_Engine.rmc.dto.RoomTypeDetailDto;
import RMC_Booking_Engine.rmc.dto.RoomUnitConfigDto;
import RMC_Booking_Engine.rmc.dto.SystemConfigItemDto;
import RMC_Booking_Engine.rmc.dto.UpdateDailyRatesRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRatePlanRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomConfigOptionRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomNumberRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomTypeCatalogRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomTypeRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRoomUnitRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.DailyRate;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.entity.RoomConfigOption;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.entity.RoomTypeImage;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.domain.entity.SystemConfig;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.domain.enums.RoomConfigOptionType;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.DailyRateRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanImageRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RefundPolicyRepository;
import RMC_Booking_Engine.rmc.repository.RoomConfigOptionRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeImageRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import RMC_Booking_Engine.rmc.repository.RoomUnitRepository;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import RMC_Booking_Engine.rmc.repository.SystemConfigRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.util.JsonStringListConverter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffConfigService {

    private static final int DEFAULT_RATE_SEED_DAYS = 120;
    private static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER);

    private final SystemConfigRepository systemConfigRepository;
    private final RatePlanRepository ratePlanRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomConfigOptionRepository roomConfigOptionRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final RatePlanImageRepository ratePlanImageRepository;
    private final DailyRateRepository dailyRateRepository;
    private final ConfigurationAuditLogRepository configurationAuditLogRepository;
    private final BookingRepository bookingRepository;
    private final InventoryHoldRepository inventoryHoldRepository;
    private final StaffUserRepository staffUserRepository;
    private final RoomDayStatusResolver roomDayStatusResolver;

    @Transactional(readOnly = true)
    public ManagerConfigResponse getConfig() {
        List<SystemConfigItemDto> systemConfig = systemConfigRepository.findAll().stream()
                .sorted((a, b) -> a.getConfigKey().compareToIgnoreCase(b.getConfigKey()))
                .map(c -> new SystemConfigItemDto(c.getConfigKey(), c.getConfigValue(), c.getDescription()))
                .toList();

        List<RatePlanConfigDto> ratePlans = ratePlanRepository.findAllWithRoomType().stream()
                .map(this::toRatePlanDto)
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

        Map<Long, Booking> bookingByUnit = bookingsForToday();
        List<RoomUnitConfigDto> roomUnits = roomUnitRepository.findAll().stream()
                .sorted((a, b) -> a.getRoomNumber().compareToIgnoreCase(b.getRoomNumber()))
                .map(unit -> toRoomUnitConfigDto(unit, bookingByUnit.get(unit.getId())))
                .toList();

        return new ManagerConfigResponse(systemConfig, ratePlans, roomTypes, roomUnits);
    }

    @Transactional(readOnly = true)
    public RoomConfigOptionsResponse getRoomConfigOptions() {
        return new RoomConfigOptionsResponse(
                listOptions(RoomConfigOptionType.ROOM_CATEGORY),
                listOptions(RoomConfigOptionType.ROOM_VIEW),
                listOptions(RoomConfigOptionType.BED_TYPE),
                listOptions(RoomConfigOptionType.ROOM_STATUS),
                listOptions(RoomConfigOptionType.AMENITY));
    }

    @Transactional
    public RoomConfigOptionDto createRoomConfigOption(
            CreateRoomConfigOptionRequest request, StaffPrincipal staff) {
        String label = request.label().trim();
        if (label.isBlank()) {
            throw new BusinessException("Label is required");
        }
        RoomConfigOptionType type = request.optionType();
        if (roomConfigOptionRepository.existsByOptionTypeAndLabelIgnoreCase(type, label)) {
            throw new BusinessException("This option already exists: " + label);
        }

        int nextSort = roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(type)
                .stream()
                .mapToInt(RoomConfigOption::getSortOrder)
                .max()
                .orElse(0) + 1;

        RoomConfigOption option = new RoomConfigOption();
        option.setOptionType(type);
        option.setLabel(label);
        option.setSortOrder(nextSort);
        option.setActive(true);
        option = roomConfigOptionRepository.save(option);

        recordAudit("ROOM_CONFIG_OPTION", option.getId(), type.name(), null, label, staff.id());
        return toOptionDto(option);
    }

    @Transactional
    public RoomConfigOptionDto updateRoomConfigOption(
            Long id, UpdateRoomConfigOptionRequest request, StaffPrincipal staff) {
        RoomConfigOption option = roomConfigOptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room option not found"));
        if (!Boolean.TRUE.equals(option.getActive())) {
            throw new BusinessException("Room option not found");
        }

        String label = request.label().trim();
        if (label.isBlank()) {
            throw new BusinessException("Label is required");
        }
        RoomConfigOptionType type = option.getOptionType();
        if (roomConfigOptionRepository.existsByOptionTypeAndLabelIgnoreCaseAndIdNot(type, label, id)) {
            throw new BusinessException("This option already exists: " + label);
        }

        String previous = option.getLabel();
        option.setLabel(label);
        option = roomConfigOptionRepository.save(option);

        recordAudit("ROOM_CONFIG_OPTION", option.getId(), type.name(), previous, label, staff.id());
        return toOptionDto(option);
    }

    @Transactional
    public void deleteRoomConfigOption(Long id, StaffPrincipal staff) {
        RoomConfigOption option = roomConfigOptionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room option not found"));
        if (!Boolean.TRUE.equals(option.getActive())) {
            throw new BusinessException("Room option not found");
        }

        long usageCount = countOptionUsage(option);
        if (usageCount > 0) {
            throw new BusinessException(
                    "Cannot remove \"" + option.getLabel() + "\" because it is used by "
                            + usageCount + " room type(s)");
        }

        String previous = option.getLabel();
        option.setActive(false);
        roomConfigOptionRepository.save(option);

        recordAudit(
                "ROOM_CONFIG_OPTION",
                option.getId(),
                option.getOptionType().name(),
                previous,
                "REMOVED",
                staff.id());
    }

    private long countOptionUsage(RoomConfigOption option) {
        return switch (option.getOptionType()) {
            case ROOM_CATEGORY -> roomTypeRepository.countByRoomCategoryId(option.getId());
            case ROOM_VIEW -> roomTypeRepository.countByRoomViewId(option.getId());
            case BED_TYPE -> roomTypeRepository.countByBedTypeId(option.getId());
            case ROOM_STATUS -> roomUnitRepository.countByStatusOptionId(option.getId());
            case AMENITY -> countAmenityLabelUsage(option.getLabel());
        };
    }

    /** Room types store amenity labels as JSON strings (not FKs). */
    private long countAmenityLabelUsage(String label) {
        if (label == null || label.isBlank()) {
            return 0;
        }
        String needle = label.trim();
        return roomTypeRepository.findAll().stream()
                .filter(roomType -> JsonStringListConverter.fromJson(roomType.getAmenities()).stream()
                        .anyMatch(amenity -> amenity != null && amenity.equalsIgnoreCase(needle)))
                .count();
    }

    private List<RoomConfigOptionDto> listOptions(RoomConfigOptionType type) {
        return roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(type)
                .stream()
                .map(this::toOptionDto)
                .toList();
    }

    private RoomConfigOptionDto toOptionDto(RoomConfigOption option) {
        return new RoomConfigOptionDto(
                option.getId(), option.getOptionType().name(), option.getLabel());
    }

    private RoomConfigOption requireOption(Long id, RoomConfigOptionType type, String label) {
        return roomConfigOptionRepository.findByIdAndOptionTypeAndActiveTrue(id, type)
                .orElseThrow(() -> new BusinessException("Invalid " + label + " selection"));
    }

    @Transactional(readOnly = true)
    public List<RoomTypeDetailDto> listRoomTypeDetails() {
        return roomTypeRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(this::toRoomTypeDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomNumberDto> listRoomNumbers(boolean unassignedOnly) {
        List<RoomUnit> units = unassignedOnly
                ? roomUnitRepository.findByRoomTypeIsNullOrderByRoomNumberAsc()
                : roomUnitRepository.findAll().stream()
                        .sorted((a, b) -> a.getRoomNumber().compareToIgnoreCase(b.getRoomNumber()))
                        .toList();
        Map<Long, Booking> bookingByUnit = bookingsForToday();
        return units.stream()
                .map(unit -> toRoomNumberDto(unit, bookingByUnit.get(unit.getId())))
                .toList();
    }

    @Transactional
    public RoomNumberDto createRoomNumber(CreateRoomNumberRequest request, StaffPrincipal staff) {
        String roomNumber = request.roomNumber().trim();
        if (roomUnitRepository.existsByRoomNumber(roomNumber)) {
            throw new BusinessException("Room number already exists: " + roomNumber);
        }

        RoomUnit unit = new RoomUnit();
        unit.setRoomNumber(roomNumber);
        unit.setFloorLabel(request.floorLabel() != null ? request.floorLabel().trim() : null);
        unit.setStatus(RoomUnitStatus.AVAILABLE);
        unit.setStatusOption(defaultAvailableStatusOption());
        unit = roomUnitRepository.save(unit);

        recordAudit("ROOM_UNIT", unit.getId(), "created", null, roomNumber, staff.id());
        return toRoomNumberDto(unit, null);
    }

    @Transactional
    public RoomNumberDto updateRoomNumber(Long unitId, UpdateRoomNumberRequest request, StaffPrincipal staff) {
        RoomUnit unit = roomUnitRepository.findById(unitId)
                .orElseThrow(() -> new BusinessException("Room unit not found"));

        String roomNumber = request.roomNumber().trim();
        if (roomNumber.isBlank()) {
            throw new BusinessException("Room number is required");
        }
        if (roomUnitRepository.findByRoomNumberIgnoreCase(roomNumber)
                .filter(existing -> !existing.getId().equals(unitId))
                .isPresent()) {
            throw new BusinessException("Room number already exists: " + roomNumber);
        }

        String previousNumber = unit.getRoomNumber();
        if (!previousNumber.equals(roomNumber)) {
            recordAudit("ROOM_UNIT", unit.getId(), "roomNumber", previousNumber, roomNumber, staff.id());
            unit.setRoomNumber(roomNumber);
        }

        String floorLabel = request.floorLabel() != null ? request.floorLabel().trim() : null;
        String previousFloor = unit.getFloorLabel();
        if (previousFloor != null ? !previousFloor.equals(floorLabel) : floorLabel != null) {
            recordAudit("ROOM_UNIT", unit.getId(), "floorLabel", previousFloor, floorLabel, staff.id());
            unit.setFloorLabel(floorLabel);
        }

        applyStatusOption(unit, request.statusOptionId(), staff);
        unit = roomUnitRepository.save(unit);

        Booking booking = bookingRepository
                .findActiveBookingsForUnitOnDate(unit.getId(), LocalDate.now(), ACTIVE_BOOKING_STATUSES)
                .stream()
                .findFirst()
                .orElse(null);
        return toRoomNumberDto(unit, booking);
    }

    @Transactional
    public void deleteRoomNumber(Long unitId, StaffPrincipal staff) {
        RoomUnit unit = roomUnitRepository.findById(unitId)
                .orElseThrow(() -> new BusinessException("Room unit not found"));

        if (bookingRepository.existsByRoomUnitIdAndCheckedOutAtIsNull(unit.getId())) {
            throw new BusinessException("Cannot delete a room assigned to an active booking");
        }

        if (bookingRepository.existsByRoomUnitId(unit.getId())) {
            bookingRepository.clearRoomUnitAssignment(unit.getId());
        }

        RoomType roomType = unit.getRoomType();
        if (roomType != null) {
            if (unit.getStatus() != RoomUnitStatus.OUT_OF_ORDER && roomType.getTotalCapacity() > 0) {
                roomType.setTotalCapacity(roomType.getTotalCapacity() - 1);
            }
            roomTypeRepository.save(roomType);
        }

        recordAudit("ROOM_UNIT", unit.getId(), "deleted", unit.getRoomNumber(), null, staff.id());
        roomUnitRepository.delete(unit);
    }

    @Transactional
    public RoomTypeDeleteResult deleteRoomType(Long id, StaffPrincipal staff) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room type not found"));

        boolean inUse = bookingRepository.existsByRoomTypeId(id)
                || inventoryHoldRepository.existsByRoomTypeId(id);
        if (inUse) {
            if (Boolean.TRUE.equals(roomType.getActive())) {
                roomType.setActive(false);
                roomTypeRepository.save(roomType);
                for (RatePlan ratePlan : ratePlanRepository.findByRoomTypeId(id)) {
                    if (Boolean.TRUE.equals(ratePlan.getActive())) {
                        ratePlan.setActive(false);
                        ratePlanRepository.save(ratePlan);
                    }
                }
                recordAudit("ROOM_TYPE", id, "active", "true", "false", staff.id());
            }
            return new RoomTypeDeleteResult(
                    true,
                    "Room type is used by bookings or inventory holds, so it was hidden from guests instead of deleted");
        }

        String name = roomType.getName();
        for (RoomUnit unit : roomUnitRepository.findByRoomTypeIdOrderByRoomNumberAsc(id)) {
            unit.setRoomType(null);
            roomUnitRepository.save(unit);
        }
        for (RatePlan ratePlan : ratePlanRepository.findByRoomTypeId(id)) {
            dailyRateRepository.deleteByRatePlanId(ratePlan.getId());
            ratePlanRepository.delete(ratePlan);
        }
        roomTypeImageRepository.deleteByRoomTypeId(id);
        roomTypeRepository.delete(roomType);
        recordAudit("ROOM_TYPE", id, "deleted", name, null, staff.id());
        return new RoomTypeDeleteResult(false, "Room type deleted");
    }

    private void applyStatusOption(RoomUnit unit, Long statusOptionId, StaffPrincipal staff) {
        RoomConfigOption statusOption = requireStatusOption(statusOptionId);
        boolean operational = roomDayStatusResolver.isOperationalAvailable(statusOption);
        RoomUnitStatus targetStatus = operational ? RoomUnitStatus.AVAILABLE : RoomUnitStatus.OUT_OF_ORDER;

        if (targetStatus == RoomUnitStatus.OUT_OF_ORDER && unit.getStatus() != RoomUnitStatus.OUT_OF_ORDER) {
            if (bookingRepository.existsByRoomUnitIdAndCheckedOutAtIsNull(unit.getId())) {
                throw new BusinessException("Cannot deactivate a room assigned to an active booking");
            }
            RoomType roomType = unit.getRoomType();
            if (roomType != null && roomType.getTotalCapacity() > 0) {
                roomType.setTotalCapacity(roomType.getTotalCapacity() - 1);
                roomTypeRepository.save(roomType);
            }
        }

        if (unit.getStatus() == RoomUnitStatus.OUT_OF_ORDER && targetStatus == RoomUnitStatus.AVAILABLE) {
            RoomType roomType = unit.getRoomType();
            if (roomType != null) {
                roomType.setTotalCapacity(roomType.getTotalCapacity() + 1);
                roomTypeRepository.save(roomType);
            }
        }

        String previousStatus = unit.getStatusOption() != null
                ? unit.getStatusOption().getLabel()
                : unit.getStatus().name();
        if (unit.getStatusOption() == null
                || !unit.getStatusOption().getId().equals(statusOption.getId())
                || unit.getStatus() != targetStatus) {
            recordAudit("ROOM_UNIT", unit.getId(), "status", previousStatus, statusOption.getLabel(), staff.id());
        }
        unit.setStatus(targetStatus);
        unit.setStatusOption(statusOption);
    }

    @Transactional
    public CreateRoomTypeResponse createRoomType(CreateRoomTypeRequest request, StaffPrincipal staff) {
        String name = request.name().trim();
        if (roomTypeRepository.findAll().stream().anyMatch(rt -> rt.getName().equalsIgnoreCase(name))) {
            throw new BusinessException("A room type with this name already exists");
        }

        List<Long> roomUnitIds = request.roomUnitIds();
        Set<Long> uniqueIds = new HashSet<>(roomUnitIds);
        if (uniqueIds.size() != roomUnitIds.size()) {
            throw new BusinessException("Duplicate room numbers selected");
        }

        List<RoomUnit> selectedUnits = roomUnitRepository.findAllByIdIn(roomUnitIds);
        if (selectedUnits.size() != roomUnitIds.size()) {
            throw new BusinessException("One or more selected room numbers were not found");
        }
        for (RoomUnit unit : selectedUnits) {
            if (unit.getRoomType() != null) {
                throw new BusinessException("Room number already assigned: " + unit.getRoomNumber());
            }
        }

        int unitCount = selectedUnits.size();
        int totalCapacity = Math.max(request.totalCapacity(), unitCount);
        List<String> amenities = JsonStringListConverter.sanitize(request.amenities());

        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setDescription(request.description() != null ? request.description().trim() : null);
        roomType.setMaxAdults(request.maxAdults());
        roomType.setMaxChildren(request.maxChildren());
        roomType.setTotalCapacity(totalCapacity);
        roomType.setOverbookingBuffer(request.overbookingBuffer() != null ? request.overbookingBuffer() : 0);
        roomType.setMinAdvanceBookingHours(
                request.minAdvanceBookingHours() != null ? request.minAdvanceBookingHours() : 2);
        roomType.setMaxAdvanceBookingDays(
                request.maxAdvanceBookingDays() != null ? request.maxAdvanceBookingDays() : 90);
        roomType.setActive(request.active() == null || request.active());
        roomType.setSquareMeters(request.squareMeters());
        roomType.setAmenities(JsonStringListConverter.toJson(amenities));
        roomType.setRoomCategory(
                requireOption(request.roomCategoryId(), RoomConfigOptionType.ROOM_CATEGORY, "room category"));
        roomType.setRoomView(requireOption(request.roomViewId(), RoomConfigOptionType.ROOM_VIEW, "room view"));
        roomType.setBedType(requireOption(request.bedTypeId(), RoomConfigOptionType.BED_TYPE, "bed type"));
        roomType = roomTypeRepository.save(roomType);

        for (RoomUnit unit : selectedUnits) {
            unit.setRoomType(roomType);
            roomUnitRepository.save(unit);
        }

        saveRoomTypeImages(roomType, request.imageUrls() != null ? request.imageUrls() : List.of());

        recordAudit("ROOM_TYPE", roomType.getId(), "created", null, name, staff.id());

        return new CreateRoomTypeResponse(toRoomTypeDetail(roomType), null, 0, unitCount);
    }

    @Transactional
    public RatePlanConfigDto createRatePlan(Long roomTypeId, CreateRatePlanRequest request, StaffPrincipal staff) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new BusinessException("Room type not found"));
        RefundPolicy policy = refundPolicyRepository.findByIdAndActiveTrue(request.refundPolicyId())
                .orElseThrow(() -> new BusinessException("Refund policy not found"));

        RatePlan ratePlan = new RatePlan();
        ratePlan.setRoomType(roomType);
        ratePlan.setRefundPolicy(policy);
        ratePlan.setName(request.name().trim());
        ratePlan.setHoldTtlMinutes(request.holdTtlMinutes() != null ? request.holdTtlMinutes() : 30);
        ratePlan.setPayLaterCutoffHours(request.payLaterCutoffHours() != null ? request.payLaterCutoffHours() : 24);
        ratePlan.setActive(request.active() == null || request.active());
        ratePlan.setBaseNightlyRate(request.baseNightlyRate());
        // Capacity columns remain on rate_plan for DB defaults; guest SoT is room type
        if (ratePlan.getMaxAdults() == null) {
            ratePlan.setMaxAdults(roomType.getMaxAdults() != null ? roomType.getMaxAdults() : 2);
        }
        if (ratePlan.getMaxChildren() == null) {
            ratePlan.setMaxChildren(roomType.getMaxChildren() != null ? roomType.getMaxChildren() : 0);
        }
        RefundPolicyConfigService.copyPolicyOntoRatePlan(ratePlan, policy);

        ratePlan = ratePlanRepository.save(ratePlan);
        seedDailyRates(ratePlan, request.baseNightlyRate());

        recordAudit("RATE_PLAN", ratePlan.getId(), "created", null, ratePlan.getName(), staff.id());
        return toRatePlanDto(ratePlan);
    }

    @Transactional
    public void deactivateRatePlan(Long id, StaffPrincipal staff) {
        RatePlan ratePlan = ratePlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Rate plan not found"));
        if (Boolean.TRUE.equals(ratePlan.getActive())) {
            ratePlan.setActive(false);
            ratePlanRepository.save(ratePlan);
            recordAudit("RATE_PLAN", id, "active", "true", "false", staff.id());
        }
    }

    /**
     * Copies the active refund policy template into a rate plan's policy fields, and syncs the
     * legacy fields kept for DB compatibility.
     */
    private void applyTemplatePolicy(RatePlan plan) {
        RefundPolicy template = refundPolicyRepository.findFirstByActiveTrueOrderByIdAsc().orElse(null);
        if (template != null) {
            plan.setPolicyEnabled(Boolean.TRUE.equals(template.getEnabled()));
            plan.setFullCutoffValue(template.getFullCutoffValue());
            plan.setFullCutoffUnit(template.getFullCutoffUnit());
            plan.setPartialEnabled(Boolean.TRUE.equals(template.getPartialEnabled()));
            plan.setPartialRefundPercent(template.getPartialRefundPercent());
            plan.setCheckInTime(template.getCheckInTime());
            plan.setTimezone(template.getTimezone());
            plan.setPolicyDescription(template.getDescription());
        } else {
            plan.setPolicyEnabled(true);
            plan.setFullCutoffValue(48);
            plan.setFullCutoffUnit(CutoffUnit.HOURS);
            plan.setPartialEnabled(true);
            plan.setPartialRefundPercent(50);
            plan.setCheckInTime(LocalTime.of(14, 0));
            plan.setTimezone("Asia/Manila");
        }
        plan.setRefundable(false);
        syncLegacyPolicyFields(plan);
    }

    /** Keeps the deprecated refundWindowHours/allowLateCancellation/lateCancelRefundPercent/cancellationPolicy in sync. */
    private void syncLegacyPolicyFields(RatePlan plan) {
        Integer fullCutoffValue = plan.getFullCutoffValue();
        CutoffUnit fullCutoffUnit = plan.getFullCutoffUnit();
        int refundWindowHours = fullCutoffValue != null
                ? (fullCutoffUnit == CutoffUnit.DAYS ? fullCutoffValue * 24 : fullCutoffValue)
                : 24;
        plan.setRefundWindowHours(refundWindowHours);
        plan.setAllowLateCancellation(Boolean.TRUE.equals(plan.getPartialEnabled()));
        plan.setLateCancelRefundPercent(
                plan.getPartialRefundPercent() != null ? plan.getPartialRefundPercent() : 50);
        if (plan.getCancellationPolicy() == null || plan.getCancellationPolicy().isBlank()) {
            plan.setCancellationPolicy(
                    plan.getPolicyDescription() != null
                            ? plan.getPolicyDescription()
                            : (Boolean.TRUE.equals(plan.getRefundable())
                                    ? "Free cancellation up to " + refundWindowHours + " hours before check-in."
                                    : "Non-refundable rate."));
        }
    }

    private void overlayRatePlanPolicy(
            RatePlan plan,
            Boolean policyEnabled,
            Integer fullCutoffValue,
            CutoffUnit fullCutoffUnit,
            Boolean partialEnabled,
            Integer partialRefundPercent,
            LocalTime checkInTime,
            String timezone,
            String policyDescription,
            Boolean refundable,
            String cancellationPolicy) {
        if (policyEnabled != null) {
            plan.setPolicyEnabled(policyEnabled);
        }
        if (fullCutoffValue != null) {
            plan.setFullCutoffValue(fullCutoffValue);
        }
        if (fullCutoffUnit != null) {
            plan.setFullCutoffUnit(fullCutoffUnit);
        }
        if (partialEnabled != null) {
            plan.setPartialEnabled(partialEnabled);
        }
        if (partialRefundPercent != null) {
            plan.setPartialRefundPercent(partialRefundPercent);
        }
        if (checkInTime != null) {
            plan.setCheckInTime(checkInTime);
        }
        if (timezone != null && !timezone.isBlank()) {
            plan.setTimezone(timezone);
        }
        if (policyDescription != null) {
            plan.setPolicyDescription(policyDescription);
        }
        if (refundable != null) {
            plan.setRefundable(refundable);
        }
        if (cancellationPolicy != null && !cancellationPolicy.isBlank()) {
            plan.setCancellationPolicy(cancellationPolicy);
        }
        syncLegacyPolicyFields(plan);
    }

    private void saveRoomTypeImages(RoomType roomType, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        int order = 0;
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            RoomTypeImage image = new RoomTypeImage();
            image.setRoomType(roomType);
            image.setImageUrl(imageUrl.trim());
            image.setSortOrder(order++);
            roomTypeImageRepository.save(image);
        }
    }

    private RoomNumberDto toRoomNumberDto(RoomUnit unit, Booking booking) {
        var resolved = roomDayStatusResolver.resolve(unit, booking);
        return new RoomNumberDto(
                unit.getId(),
                unit.getRoomNumber(),
                unit.getFloorLabel(),
                resolved.dayStatus(),
                resolved.statusLabel(),
                unit.getStatusOption() != null ? unit.getStatusOption().getId() : null,
                unit.getRoomType() != null ? unit.getRoomType().getId() : null,
                unit.getRoomType() != null ? unit.getRoomType().getName() : null);
    }

    private RoomUnitConfigDto toRoomUnitConfigDto(RoomUnit unit, Booking booking) {
        var resolved = roomDayStatusResolver.resolve(unit, booking);
        return new RoomUnitConfigDto(
                unit.getId(),
                unit.getRoomType() != null ? unit.getRoomType().getId() : null,
                unit.getRoomType() != null ? unit.getRoomType().getName() : null,
                unit.getRoomNumber(),
                unit.getFloorLabel(),
                resolved.dayStatus(),
                resolved.statusLabel(),
                unit.getStatusOption() != null ? unit.getStatusOption().getId() : null);
    }

    private Map<Long, Booking> bookingsForToday() {
        List<RoomUnit> units = roomUnitRepository.findAllByOrderByRoomNumberAsc();
        List<Long> unitIds = units.stream().map(RoomUnit::getId).toList();
        if (unitIds.isEmpty()) {
            return Map.of();
        }
        return bookingRepository
                .findActiveBookingsForUnitsOnDate(unitIds, LocalDate.now(), ACTIVE_BOOKING_STATUSES)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        booking -> booking.getRoomUnit().getId(), booking -> booking, (a, b) -> a));
    }

    private RoomConfigOption defaultAvailableStatusOption() {
        return roomConfigOptionRepository
                .findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(RoomConfigOptionType.ROOM_STATUS)
                .stream()
                .filter(option -> roomDayStatusResolver.isOperationalAvailable(option))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Room status configuration is missing an Available option"));
    }

    private RoomConfigOption requireStatusOption(Long id) {
        return roomConfigOptionRepository.findByIdAndOptionTypeAndActiveTrue(id, RoomConfigOptionType.ROOM_STATUS)
                .orElseThrow(() -> new BusinessException("Invalid room status selection"));
    }

    private int seedDailyRates(RatePlan ratePlan, BigDecimal amount) {
        applyPrimaryNightlyRate(ratePlan, amount);
        return DEFAULT_RATE_SEED_DAYS;
    }

    /**
     * Writes primary amount onto upcoming nights that are not date overrides.
     * Creates missing rows; leaves {@code overridden} nights untouched.
     */
    private void applyPrimaryNightlyRate(RatePlan ratePlan, BigDecimal amount) {
        LocalDate start = LocalDate.now();
        for (int day = 0; day < DEFAULT_RATE_SEED_DAYS; day++) {
            LocalDate rateDate = start.plusDays(day);
            DailyRate rate = dailyRateRepository.findByRatePlanIdAndRateDate(ratePlan.getId(), rateDate)
                    .orElseGet(() -> {
                        DailyRate created = new DailyRate();
                        created.setRatePlan(ratePlan);
                        created.setRateDate(rateDate);
                        created.setCurrency("PHP");
                        created.setOverridden(false);
                        return created;
                    });
            if (rate.isOverridden()) {
                continue;
            }
            rate.setAmount(amount);
            rate.setOverridden(false);
            dailyRateRepository.save(rate);
        }
    }

    private RatePlanConfigDto toRatePlanDto(RatePlan rp) {
        RefundPolicy policy = rp.getRefundPolicy();
        BigDecimal primary = rp.getBaseNightlyRate();
        BigDecimal sample = sampleNightlyRate(rp.getId());
        return new RatePlanConfigDto(
                rp.getId(),
                rp.getRoomType().getId(),
                rp.getRoomType().getName(),
                rp.getName(),
                policy != null ? policy.getId() : null,
                policy != null ? policy.getName() : null,
                Boolean.TRUE.equals(rp.getPolicyEnabled()),
                Boolean.TRUE.equals(rp.getRefundable()),
                rp.getHoldTtlMinutes(),
                rp.getPayLaterCutoffHours(),
                Boolean.TRUE.equals(rp.getActive()),
                primary != null ? primary : sample,
                sample);
    }

    private BigDecimal sampleNightlyRate(Long ratePlanId) {
        return dailyRateRepository.findByRatePlanIdAndRateDate(ratePlanId, LocalDate.now())
                .map(DailyRate::getAmount)
                .orElseGet(() -> dailyRateRepository.findFirstByRatePlanIdOrderByRateDateDesc(ratePlanId)
                        .map(DailyRate::getAmount)
                        .orElse(null));
    }

    private RoomTypeDetailDto toRoomTypeDetail(RoomType roomType) {
        RatePlan ratePlan = ratePlanRepository.findFirstByRoomTypeIdAndActiveTrueOrderByIdAsc(roomType.getId())
                .orElse(null);
        BigDecimal baseRate = null;
        if (ratePlan != null) {
            baseRate = dailyRateRepository.findByRatePlanIdAndRateDate(ratePlan.getId(), LocalDate.now())
                    .map(DailyRate::getAmount)
                    .orElse(null);
        }
        return toRoomTypeDetail(roomType, ratePlan, baseRate);
    }

    private RoomTypeDetailDto toRoomTypeDetail(RoomType roomType, RatePlan ratePlan, BigDecimal baseRate) {
        List<String> imageUrls = roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomType.getId())
                .stream()
                .map(RoomTypeImage::getImageUrl)
                .toList();
        List<RoomNumberDto> assignedRooms = roomUnitRepository.findByRoomTypeIdOrderByRoomNumberAsc(roomType.getId())
                .stream()
                .map(unit -> toRoomNumberDto(unit, null))
                .toList();

        return new RoomTypeDetailDto(
                roomType.getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getMaxAdults(),
                roomType.getMaxChildren(),
                roomType.getTotalCapacity(),
                roomType.getOverbookingBuffer(),
                roomType.getMinAdvanceBookingHours(),
                roomType.getMaxAdvanceBookingDays(),
                Boolean.TRUE.equals(roomType.getActive()),
                ratePlan != null ? ratePlan.getId() : null,
                ratePlan != null ? ratePlan.getName() : null,
                baseRate,
                assignedRooms.size(),
                roomType.getSquareMeters(),
                JsonStringListConverter.fromJson(roomType.getAmenities()),
                Boolean.TRUE.equals(roomType.getRefundable()),
                Boolean.TRUE.equals(roomType.getFreeCancellation()),
                imageUrls,
                assignedRooms,
                roomType.getRoomCategory() != null ? roomType.getRoomCategory().getId() : null,
                roomType.getRoomCategory() != null ? roomType.getRoomCategory().getLabel() : null,
                roomType.getRoomView() != null ? roomType.getRoomView().getId() : null,
                roomType.getRoomView() != null ? roomType.getRoomView().getLabel() : null,
                roomType.getBedType() != null ? roomType.getBedType().getId() : null,
                roomType.getBedType() != null ? roomType.getBedType().getLabel() : null);
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

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isBlank()) {
                throw new BusinessException("Rate plan name is required");
            }
            recordAudit("RATE_PLAN", id, "name", ratePlan.getName(), name, staff.id());
            ratePlan.setName(name);
        }
        if (request.refundPolicyId() != null) {
            RefundPolicy policy = refundPolicyRepository.findByIdAndActiveTrue(request.refundPolicyId())
                    .orElseThrow(() -> new BusinessException("Refund policy not found"));
            Long oldId = ratePlan.getRefundPolicy() != null ? ratePlan.getRefundPolicy().getId() : null;
            recordAudit("RATE_PLAN", id, "refundPolicyId", String.valueOf(oldId), String.valueOf(policy.getId()), staff.id());
            ratePlan.setRefundPolicy(policy);
            RefundPolicyConfigService.copyPolicyOntoRatePlan(ratePlan, policy);
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
        if (request.baseNightlyRate() != null) {
            BigDecimal previous = ratePlan.getBaseNightlyRate();
            recordAudit(
                    "RATE_PLAN",
                    id,
                    "baseNightlyRate",
                    previous != null ? previous.toPlainString() : null,
                    request.baseNightlyRate().toPlainString(),
                    staff.id());
            ratePlan.setBaseNightlyRate(request.baseNightlyRate());
            applyPrimaryNightlyRate(ratePlan, request.baseNightlyRate());
        }

        ratePlanRepository.save(ratePlan);
        return toRatePlanDto(ratePlan);
    }

    @Transactional
    public RoomTypeConfigDto updateRoomType(Long id, UpdateRoomTypeRequest request, StaffPrincipal staff) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room type not found"));

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isBlank()) {
                throw new BusinessException("Room type name is required");
            }
            recordAudit("ROOM_TYPE", id, "name", roomType.getName(), name, staff.id());
            roomType.setName(name);
        }
        if (request.description() != null) {
            recordAudit("ROOM_TYPE", id, "description", roomType.getDescription(), request.description(), staff.id());
            roomType.setDescription(request.description().trim());
        }
        if (request.maxAdults() != null) {
            recordAudit("ROOM_TYPE", id, "maxAdults",
                    String.valueOf(roomType.getMaxAdults()), String.valueOf(request.maxAdults()), staff.id());
            roomType.setMaxAdults(request.maxAdults());
        }
        if (request.maxChildren() != null) {
            recordAudit("ROOM_TYPE", id, "maxChildren",
                    String.valueOf(roomType.getMaxChildren()), String.valueOf(request.maxChildren()), staff.id());
            roomType.setMaxChildren(request.maxChildren());
        }
        if (request.totalCapacity() != null) {
            recordAudit("ROOM_TYPE", id, "totalCapacity",
                    String.valueOf(roomType.getTotalCapacity()), String.valueOf(request.totalCapacity()), staff.id());
            roomType.setTotalCapacity(request.totalCapacity());
        }
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

    @Transactional
    public RoomTypeDetailDto updateRoomTypeCatalog(
            Long id, UpdateRoomTypeCatalogRequest request, StaffPrincipal staff) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Room type not found"));

        String name = request.name().trim();
        if (roomTypeRepository.findAll().stream()
                .anyMatch(rt -> !rt.getId().equals(id) && rt.getName().equalsIgnoreCase(name))) {
            throw new BusinessException("A room type with this name already exists");
        }

        List<Long> roomUnitIds = request.roomUnitIds();
        Set<Long> uniqueIds = new HashSet<>(roomUnitIds);
        if (uniqueIds.size() != roomUnitIds.size()) {
            throw new BusinessException("Duplicate room numbers selected");
        }

        List<RoomUnit> selectedUnits = roomUnitRepository.findAllByIdIn(roomUnitIds);
        if (selectedUnits.size() != roomUnitIds.size()) {
            throw new BusinessException("One or more selected room numbers were not found");
        }
        for (RoomUnit unit : selectedUnits) {
            if (unit.getRoomType() != null && !unit.getRoomType().getId().equals(id)) {
                throw new BusinessException("Room number already assigned: " + unit.getRoomNumber());
            }
        }

        List<RoomUnit> currentlyAssigned = roomUnitRepository.findByRoomTypeIdOrderByRoomNumberAsc(id);
        Set<Long> selectedIdSet = new HashSet<>(roomUnitIds);
        for (RoomUnit unit : currentlyAssigned) {
            if (!selectedIdSet.contains(unit.getId())) {
                unit.setRoomType(null);
                roomUnitRepository.save(unit);
            }
        }
        for (RoomUnit unit : selectedUnits) {
            unit.setRoomType(roomType);
            roomUnitRepository.save(unit);
        }

        recordAudit("ROOM_TYPE", id, "name", roomType.getName(), name, staff.id());
        roomType.setName(name);
        roomType.setDescription(request.description() != null ? request.description().trim() : null);
        roomType.setMaxAdults(request.maxAdults());
        roomType.setMaxChildren(request.maxChildren());
        roomType.setTotalCapacity(Math.max(request.totalCapacity(), selectedUnits.size()));
        roomType.setOverbookingBuffer(request.overbookingBuffer() != null ? request.overbookingBuffer() : 0);
        roomType.setMinAdvanceBookingHours(
                request.minAdvanceBookingHours() != null ? request.minAdvanceBookingHours() : 2);
        roomType.setMaxAdvanceBookingDays(
                request.maxAdvanceBookingDays() != null ? request.maxAdvanceBookingDays() : 90);
        if (request.active() != null) {
            roomType.setActive(request.active());
        }
        roomType.setSquareMeters(request.squareMeters());
        roomType.setAmenities(JsonStringListConverter.toJson(
                JsonStringListConverter.sanitize(request.amenities())));
        roomType.setRoomCategory(
                requireOption(request.roomCategoryId(), RoomConfigOptionType.ROOM_CATEGORY, "room category"));
        roomType.setRoomView(requireOption(request.roomViewId(), RoomConfigOptionType.ROOM_VIEW, "room view"));
        roomType.setBedType(requireOption(request.bedTypeId(), RoomConfigOptionType.BED_TYPE, "bed type"));
        roomTypeRepository.save(roomType);

        replaceRoomTypeImages(roomType, request.imageUrls());

        recordAudit("ROOM_TYPE", id, "catalog_updated", null, name, staff.id());
        return toRoomTypeDetail(roomType);
    }

    private void replaceRoomTypeImages(RoomType roomType, List<String> imageUrls) {
        roomTypeImageRepository.findByRoomTypeIdOrderBySortOrderAscIdAsc(roomType.getId())
                .forEach(roomTypeImageRepository::delete);
        saveRoomTypeImages(roomType, imageUrls != null ? imageUrls : List.of());
    }

    @Transactional
    public RoomUnitConfigDto createRoomUnit(Long roomTypeId, CreateRoomUnitRequest request, StaffPrincipal staff) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new BusinessException("Room type not found"));

        String roomNumber = request.roomNumber().trim();
        if (roomUnitRepository.existsByRoomNumber(roomNumber)) {
            throw new BusinessException("Room number already exists: " + roomNumber);
        }

        RoomUnit unit = new RoomUnit();
        unit.setRoomType(roomType);
        unit.setRoomNumber(roomNumber);
        unit.setFloorLabel(request.floorLabel() != null ? request.floorLabel().trim() : null);
        unit.setStatus(RoomUnitStatus.AVAILABLE);
        unit.setStatusOption(defaultAvailableStatusOption());
        unit = roomUnitRepository.save(unit);

        roomType.setTotalCapacity(roomType.getTotalCapacity() + 1);
        roomTypeRepository.save(roomType);

        recordAudit("ROOM_UNIT", unit.getId(), "created", null, roomNumber, staff.id());

        return toRoomUnitConfigDto(unit, null);
    }

    @Transactional
    public int updateDailyRates(Long ratePlanId, UpdateDailyRatesRequest request, StaffPrincipal staff) {
        RatePlan ratePlan = ratePlanRepository.findByIdWithRoomType(ratePlanId)
                .orElseThrow(() -> new BusinessException("Rate plan not found"));

        if (request.toDate().isBefore(request.fromDate())) {
            throw new BusinessException("toDate must be on or after fromDate");
        }

        int updated = 0;
        for (LocalDate cursor = request.fromDate(); !cursor.isAfter(request.toDate()); cursor = cursor.plusDays(1)) {
            final LocalDate rateDate = cursor;
            DailyRate rate = dailyRateRepository.findByRatePlanIdAndRateDate(ratePlanId, rateDate)
                    .orElseGet(() -> {
                        DailyRate created = new DailyRate();
                        created.setRatePlan(ratePlan);
                        created.setRateDate(rateDate);
                        created.setCurrency("PHP");
                        return created;
                    });
            rate.setAmount(request.amount());
            rate.setOverridden(true);
            dailyRateRepository.save(rate);
            updated++;
        }

        recordAudit("RATE_PLAN", ratePlanId, "dailyRateOverride",
                request.fromDate() + ".." + request.toDate(),
                request.amount().toPlainString(), staff.id());

        return updated;
    }

    @Transactional(readOnly = true)
    public List<ConfigurationAuditEntryDto> getAuditLog() {
        return configurationAuditLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(entry -> new ConfigurationAuditEntryDto(
                        entry.getId(),
                        entry.getEntityType(),
                        entry.getEntityId(),
                        entry.getConfigKey(),
                        entry.getOldValue(),
                        entry.getNewValue(),
                        entry.getStaffUserId(),
                        resolveStaffEmail(entry.getStaffUserId()),
                        entry.getCreatedAt()))
                .toList();
    }

    @Transactional
    public RoomUnitConfigDto updateRoomUnit(Long unitId, UpdateRoomUnitRequest request, StaffPrincipal staff) {
        RoomUnit unit = roomUnitRepository.findById(unitId)
                .orElseThrow(() -> new BusinessException("Room unit not found"));

        applyStatusOption(unit, request.statusOptionId(), staff);
        unit = roomUnitRepository.save(unit);

        Booking booking = bookingRepository
                .findActiveBookingsForUnitOnDate(unit.getId(), LocalDate.now(), ACTIVE_BOOKING_STATUSES)
                .stream()
                .findFirst()
                .orElse(null);
        return toRoomUnitConfigDto(unit, booking);
    }

    private String resolveStaffEmail(Long staffUserId) {
        if (staffUserId == null) {
            return null;
        }
        return staffUserRepository.findById(staffUserId).map(StaffUser::getEmail).orElse(null);
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
