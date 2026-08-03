package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.DailyRate;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.GuestRoomCatalogItemDto;
import RMC_Booking_Engine.rmc.repository.DailyRateRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GuestRoomCatalogService {

    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;
    private final DailyRateRepository dailyRateRepository;
    private final RoomCatalogMapper roomCatalogMapper;

    public GuestRoomCatalogService(
            RoomTypeRepository roomTypeRepository,
            RatePlanRepository ratePlanRepository,
            DailyRateRepository dailyRateRepository,
            RoomCatalogMapper roomCatalogMapper) {
        this.roomTypeRepository = roomTypeRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.dailyRateRepository = dailyRateRepository;
        this.roomCatalogMapper = roomCatalogMapper;
    }

    public List<GuestRoomCatalogItemDto> listActiveCatalog() {
        List<GuestRoomCatalogItemDto> items = new ArrayList<>();
        for (RoomType roomType : roomTypeRepository.findByActiveTrue()) {
            List<RatePlan> plans = ratePlanRepository.findByRoomTypeIdAndActiveTrue(roomType.getId());
            if (plans.isEmpty()) {
                continue;
            }
            BigDecimal fromRate = null;
            RatePlan fromPlan = null;
            for (RatePlan plan : plans) {
                BigDecimal candidate = resolveFromNightlyRate(plan.getId());
                if (candidate == null) {
                    continue;
                }
                if (fromRate == null || candidate.compareTo(fromRate) < 0) {
                    fromRate = candidate;
                    fromPlan = plan;
                }
            }
            if (fromPlan == null) {
                fromPlan = plans.get(0);
            }
            items.add(new GuestRoomCatalogItemDto(
                    roomType.getId(),
                    fromPlan.getId(),
                    roomCatalogMapper.toCard(roomType),
                    fromRate,
                    "PHP"));
        }
        return items;
    }

    private BigDecimal resolveFromNightlyRate(Long ratePlanId) {
        LocalDate start = LocalDate.now().plusDays(1);
        for (int i = 0; i < 30; i++) {
            LocalDate date = start.plusDays(i);
            BigDecimal amount = dailyRateRepository.findByRatePlanIdAndRateDate(ratePlanId, date)
                    .map(DailyRate::getAmount)
                    .orElse(null);
            if (amount != null) {
                return amount;
            }
        }
        return null;
    }
}
