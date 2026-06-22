package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.dto.RoomAvailabilityDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final InventoryHoldRepository inventoryHoldRepository;
    private final PricingService pricingService;

    public List<RoomAvailabilityDto> search(LocalDate checkIn, LocalDate checkOut, Long roomTypeId) {
        validateDates(checkIn, checkOut);

        List<RoomType> roomTypes = roomTypeId != null
                ? roomTypeRepository.findById(roomTypeId).filter(RoomType::getActive).stream().toList()
                : roomTypeRepository.findByActiveTrue();

        List<RoomAvailabilityDto> results = new ArrayList<>();
        for (RoomType roomType : roomTypes) {
            RatePlan ratePlan = ratePlanRepository.findFirstByRoomTypeIdAndActiveTrueOrderByIdAsc(roomType.getId())
                    .orElse(null);
            if (ratePlan == null) {
                continue;
            }

            int minAvailable = minAvailableUnits(roomType, checkIn, checkOut);
            if (minAvailable <= 0) {
                continue;
            }

            buildRoomAvailability(roomType, ratePlan, checkIn, checkOut, minAvailable)
                    .ifPresent(results::add);
        }
        return results;
    }

    private Optional<RoomAvailabilityDto> buildRoomAvailability(
            RoomType roomType,
            RatePlan ratePlan,
            LocalDate checkIn,
            LocalDate checkOut,
            int minAvailable) {
        try {
            List<NightlyRateDto> nightlyBreakdown = pricingService.calculateStayPricing(
                    ratePlan.getId(), checkIn, checkOut);
            BigDecimal total = pricingService.sumTaxInclusive(nightlyBreakdown);
            return Optional.of(new RoomAvailabilityDto(
                    roomType.getId(),
                    ratePlan.getId(),
                    roomType.getName(),
                    roomType.getDescription(),
                    roomType.getMaxAdults(),
                    roomType.getMaxChildren(),
                    minAvailable,
                    total,
                    "PHP",
                    nightlyBreakdown));
        } catch (BusinessException ex) {
            return Optional.empty();
        }
    }

    public void assertAvailable(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);
        int minAvailable = minAvailableUnits(roomType, checkIn, checkOut);
        if (minAvailable <= 0) {
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
