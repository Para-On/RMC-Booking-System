package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.AppliedPromoDto;
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
    private final PromoService promoService;

    public RoomAvailabilityMapper(
            PricingService pricingService,
            RoomCatalogMapper roomCatalogMapper,
            PromoService promoService) {
        this.pricingService = pricingService;
        this.roomCatalogMapper = roomCatalogMapper;
        this.promoService = promoService;
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
        AppliedPromoDto promo = promoService
                .findBestAppliedPromo(roomType.getId(), totalTaxInclusive)
                .orElse(null);
        BigDecimal originalTotal = promo != null ? totalTaxInclusive : null;
        BigDecimal discountedTotal = promo != null
                ? promoService.applyAmountOff(totalTaxInclusive, promo.amountOff())
                : totalTaxInclusive;

        RoomCatalogCardDto catalog = roomCatalogMapper.toCard(roomType);
        return toDto(
                roomType,
                ratePlan,
                availableUnits,
                totalBase,
                discountedTotal,
                originalTotal,
                promo,
                nightlyRates,
                catalog);
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
            BigDecimal originalTotalTaxInclusive,
            AppliedPromoDto promo,
            List<NightlyRateDto> nightlyRates,
            RoomCatalogCardDto catalog) {
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
                originalTotalTaxInclusive,
                promo,
                "PHP",
                nightlyRates,
                catalog.squareMeters(),
                catalog.imageUrls(),
                catalog.amenities(),
                catalog.roomCategoryLabel(),
                catalog.roomViewLabel(),
                catalog.bedTypeLabel(),
                catalog.refundable(),
                catalog.freeCancellation());
    }
}
