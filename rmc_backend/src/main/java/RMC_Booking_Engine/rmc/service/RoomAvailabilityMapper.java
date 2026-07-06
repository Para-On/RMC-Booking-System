package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.dto.RoomAvailabilityDto;
import RMC_Booking_Engine.rmc.dto.RoomCatalogCardDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RoomAvailabilityMapper {

    private final PricingService pricingService;
    private final RoomCatalogMapper roomCatalogMapper;

    public RoomAvailabilityMapper(PricingService pricingService, RoomCatalogMapper roomCatalogMapper) {
        this.pricingService = pricingService;
        this.roomCatalogMapper = roomCatalogMapper;
    }

    public RoomAvailabilityDto buildForStay(
            RoomType roomType,
            RatePlan ratePlan,
            LocalDate checkIn,
            LocalDate checkOut,
            int availableUnits) {
        List<NightlyRateDto> nightlyRates = loadNightlyRates(ratePlan.getId(), checkIn, checkOut);
        if (nightlyRates == null) {
            return null;
        }

        BigDecimal totalBase = pricingService.sumBase(nightlyRates);
        BigDecimal totalTaxInclusive = pricingService.sumTaxInclusive(nightlyRates);
        RoomCatalogCardDto catalog = roomCatalogMapper.toCard(roomType);
        return toDto(roomType, ratePlan, availableUnits, totalBase, totalTaxInclusive, nightlyRates, catalog);
    }

    private List<NightlyRateDto> loadNightlyRates(Long ratePlanId, LocalDate checkIn, LocalDate checkOut) {
        try {
            return pricingService.calculateStayPricing(ratePlanId, checkIn, checkOut);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private RoomAvailabilityDto toDto(
            RoomType roomType,
            RatePlan ratePlan,
            int availableUnits,
            BigDecimal totalBase,
            BigDecimal totalTaxInclusive,
            List<NightlyRateDto> nightlyRates,
            RoomCatalogCardDto catalog) {
        BigDecimal squareMeters = catalog.squareMeters();
        List<String> imageUrls = catalog.imageUrls();
        List<String> amenities = catalog.amenities();
        String roomCategoryLabel = catalog.roomCategoryLabel();
        String roomViewLabel = catalog.roomViewLabel();
        String bedTypeLabel = catalog.bedTypeLabel();
        boolean refundable = catalog.refundable();
        boolean freeCancellation = catalog.freeCancellation();

        return new RoomAvailabilityDto(
                roomType.getId(),
                ratePlan.getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getMaxAdults(),
                roomType.getMaxChildren(),
                availableUnits,
                totalBase,
                totalTaxInclusive,
                "PHP",
                nightlyRates,
                squareMeters,
                imageUrls,
                amenities,
                roomCategoryLabel,
                roomViewLabel,
                bedTypeLabel,
                refundable,
                freeCancellation);
    }
}
