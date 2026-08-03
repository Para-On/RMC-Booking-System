package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.AppliedPromoDto;
import RMC_Booking_Engine.rmc.dto.GuestRatePlanOfferDto;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.dto.RoomAvailabilityDto;
import RMC_Booking_Engine.rmc.dto.RoomCatalogCardDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RoomAvailabilityMapper {

    private final PricingService pricingService;
    private final PromoService promoService;
    private final PromoCodeService promoCodeService;
    private final RoomCatalogMapper roomCatalogMapper;

    public RoomAvailabilityMapper(
            PricingService pricingService,
            PromoService promoService,
            PromoCodeService promoCodeService,
            RoomCatalogMapper roomCatalogMapper) {
        this.pricingService = pricingService;
        this.promoService = promoService;
        this.promoCodeService = promoCodeService;
        this.roomCatalogMapper = roomCatalogMapper;
    }

    public RoomAvailabilityDto buildForStayMultiPlan(
            RoomType roomType,
            List<RatePlan> ratePlans,
            LocalDate checkIn,
            LocalDate checkOut,
            int availableUnits) {
        return buildForStayMultiPlan(roomType, ratePlans, checkIn, checkOut, availableUnits, null, null, null);
    }

    public RoomAvailabilityDto buildForStayMultiPlan(
            RoomType roomType,
            List<RatePlan> ratePlans,
            LocalDate checkIn,
            LocalDate checkOut,
            int availableUnits,
            String offerCode,
            String organizationCode) {
        return buildForStayMultiPlan(
                roomType, ratePlans, checkIn, checkOut, availableUnits, null, offerCode, organizationCode);
    }

    public RoomAvailabilityDto buildForStayMultiPlan(
            RoomType roomType,
            List<RatePlan> ratePlans,
            LocalDate checkIn,
            LocalDate checkOut,
            int availableUnits,
            String promoType,
            String offerCode,
            String organizationCode) {
        List<GuestRatePlanOfferDto> offers = new ArrayList<>();
        for (RatePlan ratePlan : ratePlans) {
            GuestRatePlanOfferDto offer =
                    buildOffer(roomType, ratePlan, checkIn, checkOut, promoType, offerCode, organizationCode);
            if (offer != null) {
                offers.add(offer);
            }
        }
        if (offers.isEmpty()) {
            return null;
        }

        offers.sort(Comparator.comparing(GuestRatePlanOfferDto::totalTaxInclusive));
        GuestRatePlanOfferDto cheapest = offers.get(0);
        boolean policiesVary = offers.stream().map(GuestRatePlanOfferDto::refundable).distinct().count() > 1
                || offers.stream().map(GuestRatePlanOfferDto::policySummary).distinct().count() > 1;

        RoomCatalogCardDto card = roomCatalogMapper.toCard(roomType);

        // Card media/meta from room type; From price + policy badges from cheapest plan
        return new RoomAvailabilityDto(
                roomType.getId(),
                cheapest.ratePlanId(),
                card.name(),
                card.description(),
                card.maxAdults(),
                card.maxChildren(),
                availableUnits,
                cheapest.totalBase(),
                cheapest.totalTaxInclusive(),
                cheapest.totalTaxInclusive(),
                cheapest.originalTotalTaxInclusive(),
                cheapest.promo(),
                "PHP",
                cheapest.nightlyBreakdown(),
                offers,
                card.squareMeters(),
                card.imageUrls(),
                card.amenities(),
                card.roomCategoryLabel(),
                card.roomViewLabel(),
                card.bedTypeLabel(),
                cheapest.refundable(),
                cheapest.freeCancellation(),
                policiesVary);
    }

    public RoomAvailabilityDto buildForStay(
            RoomType roomType,
            RatePlan ratePlan,
            LocalDate checkIn,
            LocalDate checkOut,
            int availableUnits) {
        return buildForStayMultiPlan(roomType, List.of(ratePlan), checkIn, checkOut, availableUnits);
    }

    public RoomAvailabilityDto buildForStay(
            RoomType roomType,
            RatePlan ratePlan,
            LocalDate checkIn,
            LocalDate checkOut,
            int availableUnits,
            String promoType,
            String offerCode,
            String organizationCode) {
        return buildForStayMultiPlan(
                roomType,
                List.of(ratePlan),
                checkIn,
                checkOut,
                availableUnits,
                promoType,
                offerCode,
                organizationCode);
    }

    private GuestRatePlanOfferDto buildOffer(
            RoomType roomType,
            RatePlan ratePlan,
            LocalDate checkIn,
            LocalDate checkOut,
            String promoType,
            String offerCode,
            String organizationCode) {
        List<NightlyRateDto> nightlyRates = loadNightlyRates(ratePlan.getId(), checkIn, checkOut);
        if (nightlyRates == null) {
            return null;
        }

        BigDecimal totalBase = pricingService.sumBase(nightlyRates);
        BigDecimal totalTaxInclusive = pricingService.sumTaxInclusive(nightlyRates);

        AppliedPromoDto promo = null;
        boolean codesPresent = (offerCode != null && !offerCode.isBlank())
                || (organizationCode != null && !organizationCode.isBlank())
                || (promoType != null && !promoType.isBlank());
        if (codesPresent) {
            promo = promoCodeService
                    .tryResolveForRatePlan(
                            ratePlan.getId(), totalTaxInclusive, promoType, offerCode, organizationCode)
                    .map(match -> promoCodeService.toApplied(match.promoCode(), match.amountOff()))
                    .orElse(null);
        } else {
            promo = promoService.findBestAppliedPromo(roomType.getId(), totalTaxInclusive).orElse(null);
        }

        BigDecimal originalTotal = promo != null ? totalTaxInclusive : null;
        BigDecimal discountedTotal = promo != null
                ? promoService.applyAmountOff(totalTaxInclusive, promo.amountOff())
                : totalTaxInclusive;

        boolean refundable = Boolean.TRUE.equals(ratePlan.getRefundable());
        boolean freeCancellation = refundable
                && Boolean.TRUE.equals(ratePlan.getPolicyEnabled())
                && ratePlan.getFullCutoffValue() != null
                && ratePlan.getFullCutoffValue() > 0;

        String summary = ratePlan.getPolicyDescription() != null && !ratePlan.getPolicyDescription().isBlank()
                ? ratePlan.getPolicyDescription()
                : ratePlan.getCancellationPolicy();

        return new GuestRatePlanOfferDto(
                ratePlan.getId(),
                ratePlan.getName(),
                totalBase,
                discountedTotal,
                originalTotal,
                promo,
                nightlyRates,
                refundable,
                freeCancellation,
                summary,
                ratePlan.getFullCutoffValue(),
                ratePlan.getFullCutoffUnit(),
                Boolean.TRUE.equals(ratePlan.getPartialEnabled()),
                ratePlan.getPartialRefundPercent());
    }

    private List<NightlyRateDto> loadNightlyRates(Long ratePlanId, LocalDate checkIn, LocalDate checkOut) {
        try {
            return pricingService.calculateStayPricing(ratePlanId, checkIn, checkOut);
        } catch (BusinessException ex) {
            return null;
        }
    }
}
