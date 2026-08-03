package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.RoomAvailabilityDto;
import RMC_Booking_Engine.rmc.dto.StayAvailabilityCheckResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final InventoryHoldRepository inventoryHoldRepository;
    private final RoomAvailabilityMapper roomAvailabilityMapper;

    public AvailabilityService(
            RoomTypeRepository roomTypeRepository,
            RatePlanRepository ratePlanRepository,
            InventoryHoldRepository inventoryHoldRepository,
            RoomAvailabilityMapper roomAvailabilityMapper) {
        this.roomTypeRepository = roomTypeRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.inventoryHoldRepository = inventoryHoldRepository;
        this.roomAvailabilityMapper = roomAvailabilityMapper;
    }

    public List<RoomAvailabilityDto> search(LocalDate checkIn, LocalDate checkOut, Long roomTypeId) {
        return search(checkIn, checkOut, roomTypeId, null, null, null);
    }

    public List<RoomAvailabilityDto> search(
            LocalDate checkIn,
            LocalDate checkOut,
            Long roomTypeId,
            String offerCode,
            String organizationCode) {
        return search(checkIn, checkOut, roomTypeId, null, offerCode, organizationCode);
    }

    public List<RoomAvailabilityDto> search(
            LocalDate checkIn,
            LocalDate checkOut,
            Long roomTypeId,
            String promoType,
            String offerCode,
            String organizationCode) {
        validateDates(checkIn, checkOut);

        List<RoomType> roomTypes = roomTypeId != null
                ? roomTypeRepository.findById(roomTypeId).filter(RoomType::getActive).stream().toList()
                : roomTypeRepository.findByActiveTrue();

        List<RoomAvailabilityDto> results = new ArrayList<>();
        for (RoomType roomType : roomTypes) {
            List<RatePlan> ratePlans = ratePlanRepository.findActiveByRoomTypeIdWithProduct(roomType.getId());
            if (ratePlans.isEmpty()) {
                continue;
            }

            int minAvailable = minAvailableUnits(roomType, checkIn, checkOut);
            if (minAvailable <= 0) {
                continue;
            }

            RoomAvailabilityDto room = roomAvailabilityMapper.buildForStayMultiPlan(
                    roomType,
                    ratePlans,
                    checkIn,
                    checkOut,
                    minAvailable,
                    promoType,
                    offerCode,
                    organizationCode);
            if (room != null) {
                results.add(room);
            }
        }
        return results;
    }

    public StayAvailabilityCheckResponse checkStay(
            Long roomTypeId, LocalDate checkIn, LocalDate checkOut, Long ratePlanId) {
        return checkStay(roomTypeId, checkIn, checkOut, ratePlanId, null, null, null);
    }

    public StayAvailabilityCheckResponse checkStay(
            Long roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut,
            Long ratePlanId,
            String promoType,
            String offerCode,
            String organizationCode) {
        validateDates(checkIn, checkOut);

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .filter(RoomType::getActive)
                .orElseThrow(() -> new BusinessException("Room type not found"));

        List<RatePlan> activePlans = ratePlanRepository.findActiveByRoomTypeIdWithProduct(roomType.getId());
        if (activePlans.isEmpty()) {
            throw new BusinessException("No active rate plan for this room");
        }

        List<LocalDate> unavailableDates = listUnavailableDates(roomType, checkIn, checkOut);
        if (!unavailableDates.isEmpty()) {
            String nights = unavailableDates.stream().map(LocalDate::toString).reduce((a, b) -> a + ", " + b).orElse("");
            return new StayAvailabilityCheckResponse(
                    false,
                    "This room is not available for every night of your stay. Unavailable dates: " + nights,
                    unavailableDates,
                    null);
        }

        int minAvailable = minAvailableUnits(roomType, checkIn, checkOut);
        RoomAvailabilityDto room;
        if (ratePlanId != null) {
            RatePlan selected = activePlans.stream()
                    .filter(p -> p.getId().equals(ratePlanId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Rate plan not found or inactive for this room"));
            room = roomAvailabilityMapper.buildForStay(
                    roomType,
                    selected,
                    checkIn,
                    checkOut,
                    minAvailable,
                    promoType,
                    offerCode,
                    organizationCode);
        } else {
            room = roomAvailabilityMapper.buildForStayMultiPlan(
                    roomType,
                    activePlans,
                    checkIn,
                    checkOut,
                    minAvailable,
                    promoType,
                    offerCode,
                    organizationCode);
        }
        if (room == null) {
            return new StayAvailabilityCheckResponse(
                    false,
                    "Rates are not available for the full stay period. Try different dates.",
                    List.of(),
                    null);
        }

        return new StayAvailabilityCheckResponse(true, null, List.of(), room);
    }

    public StayAvailabilityCheckResponse checkStay(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return checkStay(roomTypeId, checkIn, checkOut, null);
    }

    public List<LocalDate> listUnavailableDates(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        int sellableCap = roomType.getTotalCapacity() + roomType.getOverbookingBuffer();
        List<LocalDate> unavailable = new ArrayList<>();

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            int held = inventoryHoldRepository.countActiveHeldUnits(roomType.getId(), date);
            if (sellableCap - held <= 0) {
                unavailable.add(date);
            }
        }
        return unavailable;
    }

    public void assertAvailable(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);
        if (minAvailableUnits(roomType, checkIn, checkOut) <= 0) {
            throw new BusinessException("No availability for the selected dates");
        }
    }

    public int minAvailableUnits(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        int sellableCap = roomType.getTotalCapacity() + roomType.getOverbookingBuffer();
        int minAvailable = Integer.MAX_VALUE;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            int held = inventoryHoldRepository.countActiveHeldUnits(roomType.getId(), date);
            int available = sellableCap - held;
            minAvailable = Math.min(minAvailable, available);
        }
        return minAvailable == Integer.MAX_VALUE ? 0 : minAvailable;
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException("Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException("Check-out must be after check-in");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException("Check-in cannot be in the past");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > 30) {
            throw new BusinessException("Maximum stay is 30 nights");
        }
    }
}
