package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.PromoCode;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.enums.PromoCodeType;
import RMC_Booking_Engine.rmc.domain.enums.PromoDiscountType;
import RMC_Booking_Engine.rmc.dto.AppliedPromoDto;
import RMC_Booking_Engine.rmc.dto.CreatePromoCodeRequest;
import RMC_Booking_Engine.rmc.dto.PromoCodeDto;
import RMC_Booking_Engine.rmc.dto.UpdatePromoCodeRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.PromoCodeRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Manila");

    private final PromoCodeRepository promoCodeRepository;
    private final RatePlanRepository ratePlanRepository;

    @Transactional(readOnly = true)
    public List<PromoCodeDto> listStaffPromoCodes() {
        LocalDate today = today();
        return promoCodeRepository.findAllByOrderByStartsOnDescNameAsc().stream()
                .peek(pc -> pc.getRatePlans().size())
                .map(pc -> toDto(pc, today))
                .toList();
    }

    @Transactional
    public PromoCodeDto create(CreatePromoCodeRequest request) {
        PromoCode promoCode = new PromoCode();
        applyRequest(
                promoCode,
                request.name(),
                request.description(),
                request.promoType(),
                request.offerCode(),
                request.organizationCode(),
                request.discountType(),
                request.discountValue(),
                request.startsOn(),
                request.endsOn(),
                request.maxUses(),
                request.active(),
                request.ratePlanIds());
        promoCode.setUsedCount(0);
        promoCode.setCreatedAt(Instant.now());
        return toDto(promoCodeRepository.save(promoCode), today());
    }

    @Transactional
    public PromoCodeDto update(Long id, UpdatePromoCodeRequest request) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Promo code not found"));
        if (request.maxUses() < promoCode.getUsedCount()) {
            throw new BusinessException("Max uses cannot be less than current used count");
        }
        applyRequest(
                promoCode,
                request.name(),
                request.description(),
                request.promoType(),
                request.offerCode(),
                request.organizationCode(),
                request.discountType(),
                request.discountValue(),
                request.startsOn(),
                request.endsOn(),
                request.maxUses(),
                request.active(),
                request.ratePlanIds());
        promoCode.setUpdatedAt(Instant.now());
        return toDto(promoCodeRepository.save(promoCode), today());
    }

    @Transactional
    public void delete(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Promo code not found"));
        promoCodeRepository.delete(promoCode);
    }

    /**
     * Validate guest-entered codes without a rate plan (active/window/uses/type fields).
     * Empty codes → no-op. Throws BusinessException when invalid.
     */
    @Transactional(readOnly = true)
    public void assertGuestCodesPresentAndValid(
            String promoType, String offerCode, String organizationCode) {
        String offer = normalize(offerCode);
        String org = normalize(organizationCode);
        if (offer == null && org == null && (promoType == null || promoType.isBlank())) {
            return;
        }
        if (offer == null) {
            throw new BusinessException("Offer code is required");
        }
        PromoCode promoCode = promoCodeRepository.findByOfferCodeIgnoreCase(offer)
                .orElseThrow(() -> new BusinessException("Invalid promo code"));
        if (promoType == null || promoType.isBlank()) {
            throw new BusinessException("Select a promo type");
        }
        assertPromoTypeMatches(promoCode, promoType);
        validateCodesForType(promoCode, offer, org);
        LocalDate today = today();
        if (!promoCode.isActive()
                || today.isBefore(promoCode.getStartsOn())
                || today.isAfter(promoCode.getEndsOn())) {
            throw new BusinessException("This promo code is not currently available");
        }
        if (promoCode.getUsedCount() >= promoCode.getMaxUses()) {
            throw new BusinessException("This promo code has reached its usage limit");
        }
    }

    /**
     * Resolve a guest-entered code for a rate plan. Empty optional = no codes supplied.
     * Throws when codes were supplied but invalid / exhausted / out of scope.
     */
    @Transactional(readOnly = true)
    public Optional<PromoCodeMatch> resolveForRatePlan(
            Long ratePlanId,
            BigDecimal roomTotal,
            String promoType,
            String offerCode,
            String organizationCode) {
        String offer = normalize(offerCode);
        String org = normalize(organizationCode);
        if (offer == null && org == null && (promoType == null || promoType.isBlank())) {
            return Optional.empty();
        }
        if (offer == null) {
            throw new BusinessException("Offer code is required");
        }

        PromoCode promoCode = promoCodeRepository.findByOfferCodeIgnoreCase(offer)
                .orElseThrow(() -> new BusinessException("Invalid promo code"));

        if (promoType != null && !promoType.isBlank()) {
            assertPromoTypeMatches(promoCode, promoType);
        }
        validateCodesForType(promoCode, offer, org);
        assertApplicable(promoCode, ratePlanId);

        BigDecimal amountOff = calculateAmountOff(roomTotal, promoCode);
        if (amountOff.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Promo code does not apply to this stay");
        }
        return Optional.of(new PromoCodeMatch(promoCode, amountOff));
    }

    /** Backward-compatible resolve without explicit guest promo type. */
    @Transactional(readOnly = true)
    public Optional<PromoCodeMatch> resolveForRatePlan(
            Long ratePlanId, BigDecimal roomTotal, String offerCode, String organizationCode) {
        return resolveForRatePlan(ratePlanId, roomTotal, null, offerCode, organizationCode);
    }

    @Transactional(readOnly = true)
    public Optional<AppliedPromoDto> resolveApplied(
            Long ratePlanId,
            BigDecimal roomTotal,
            String promoType,
            String offerCode,
            String organizationCode) {
        return resolveForRatePlan(ratePlanId, roomTotal, promoType, offerCode, organizationCode)
                .map(match -> toApplied(match.promoCode(), match.amountOff()));
    }

    @Transactional(readOnly = true)
    public Optional<AppliedPromoDto> resolveApplied(
            Long ratePlanId, BigDecimal roomTotal, String offerCode, String organizationCode) {
        return resolveApplied(ratePlanId, roomTotal, null, offerCode, organizationCode);
    }

    /** Soft lookup for search: invalid codes return empty (caller may surface error separately). */
    @Transactional(readOnly = true)
    public Optional<PromoCodeMatch> tryResolveForRatePlan(
            Long ratePlanId,
            BigDecimal roomTotal,
            String promoType,
            String offerCode,
            String organizationCode) {
        try {
            return resolveForRatePlan(ratePlanId, roomTotal, promoType, offerCode, organizationCode);
        } catch (BusinessException ex) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public Optional<PromoCodeMatch> tryResolveForRatePlan(
            Long ratePlanId, BigDecimal roomTotal, String offerCode, String organizationCode) {
        return tryResolveForRatePlan(ratePlanId, roomTotal, null, offerCode, organizationCode);
    }

    @Transactional
    public void consumeUsage(PromoCode promoCode) {
        int updated = promoCodeRepository.tryIncrementUsedCount(promoCode.getId());
        if (updated != 1) {
            throw new BusinessException("This promo code has reached its usage limit");
        }
    }

    @Transactional
    public void releaseUsageIfAttached(Booking booking) {
        if (booking == null || booking.getPromoCode() == null) {
            return;
        }
        Long id = booking.getPromoCode().getId();
        promoCodeRepository.tryDecrementUsedCount(id);
        booking.setPromoCode(null);
    }

    public BigDecimal applyAmountOff(BigDecimal roomTotal, BigDecimal amountOff) {
        if (roomTotal == null) {
            return BigDecimal.ZERO;
        }
        if (amountOff == null || amountOff.compareTo(BigDecimal.ZERO) <= 0) {
            return scale(roomTotal);
        }
        return scale(roomTotal.subtract(amountOff).max(BigDecimal.ZERO));
    }

    public AppliedPromoDto toApplied(PromoCode promoCode, BigDecimal amountOff) {
        return new AppliedPromoDto(
                promoCode.getId(),
                promoCode.getName(),
                promoCode.getDescription(),
                promoCode.getDiscountType().name(),
                promoCode.getDiscountValue(),
                amountOff,
                buildLabel(promoCode));
    }

    private void applyRequest(
            PromoCode promoCode,
            String name,
            String description,
            String promoTypeRaw,
            String offerCode,
            String organizationCode,
            String discountTypeRaw,
            BigDecimal discountValue,
            LocalDate startsOn,
            LocalDate endsOn,
            int maxUses,
            boolean active,
            List<Long> ratePlanIds) {
        if (endsOn.isBefore(startsOn)) {
            throw new BusinessException("End date must be on or after start date");
        }
        PromoCodeType type = parseType(promoTypeRaw);
        PromoDiscountType discountType = parseDiscountType(discountTypeRaw);
        String offer = normalize(offerCode);
        String org = normalize(organizationCode);
        if (offer == null) {
            throw new BusinessException("Offer code is required");
        }
        if (type == PromoCodeType.SPECIAL_RATE) {
            org = null;
        } else if (org == null) {
            throw new BusinessException("Organization code is required for corporate and agency rates");
        }
        if (discountType == PromoDiscountType.PERCENT && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Percent discount cannot exceed 100");
        }

        promoCodeRepository.findByOfferCodeIgnoreCase(offer).ifPresent(existing -> {
            if (promoCode.getId() == null || !existing.getId().equals(promoCode.getId())) {
                throw new BusinessException("Offer code already exists");
            }
        });

        promoCode.setName(name.trim());
        promoCode.setDescription(trimOrNull(description));
        promoCode.setPromoType(type);
        promoCode.setOfferCode(offer.toUpperCase(Locale.ROOT));
        promoCode.setOrganizationCode(org == null ? null : org.toUpperCase(Locale.ROOT));
        promoCode.setDiscountType(discountType);
        promoCode.setDiscountValue(scale(discountValue));
        promoCode.setStartsOn(startsOn);
        promoCode.setEndsOn(endsOn);
        promoCode.setMaxUses(maxUses);
        promoCode.setActive(active);
        promoCode.setRatePlans(resolveRatePlans(ratePlanIds));
    }

    private void validateCodesForType(PromoCode promoCode, String offer, String org) {
        if (promoCode.getPromoType() == PromoCodeType.SPECIAL_RATE) {
            return;
        }
        if (org == null) {
            throw new BusinessException("Organization code is required for this promo");
        }
        if (promoCode.getOrganizationCode() == null
                || !promoCode.getOrganizationCode().equalsIgnoreCase(org)) {
            throw new BusinessException("Invalid promo code");
        }
        if (!promoCode.getOfferCode().equalsIgnoreCase(offer)) {
            throw new BusinessException("Invalid promo code");
        }
    }

    private void assertPromoTypeMatches(PromoCode promoCode, String expectedTypeRaw) {
        PromoCodeType expected = parseType(expectedTypeRaw);
        if (promoCode.getPromoType() != expected) {
            throw new BusinessException("Invalid promo code");
        }
    }

    private void assertApplicable(PromoCode promoCode, Long ratePlanId) {
        LocalDate today = today();
        if (!promoCode.isActive()
                || today.isBefore(promoCode.getStartsOn())
                || today.isAfter(promoCode.getEndsOn())) {
            throw new BusinessException("This promo code is not currently available");
        }
        if (promoCode.getUsedCount() >= promoCode.getMaxUses()) {
            throw new BusinessException("This promo code has reached its usage limit");
        }
        boolean inScope = promoCode.getRatePlans().stream()
                .anyMatch(rp -> rp.getId().equals(ratePlanId));
        if (!inScope) {
            throw new BusinessException("This promo code does not apply to the selected rate plan");
        }
    }

    private Set<RatePlan> resolveRatePlans(List<Long> ratePlanIds) {
        if (ratePlanIds == null || ratePlanIds.isEmpty()) {
            throw new BusinessException("Select at least one rate plan for this promo code");
        }
        Set<RatePlan> plans = new HashSet<>();
        for (Long id : ratePlanIds) {
            RatePlan plan = ratePlanRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Rate plan not found: " + id));
            plans.add(plan);
        }
        return plans;
    }

    private BigDecimal calculateAmountOff(BigDecimal roomTotal, PromoCode promoCode) {
        BigDecimal off;
        if (promoCode.getDiscountType() == PromoDiscountType.PERCENT) {
            off = roomTotal.multiply(promoCode.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            off = scale(promoCode.getDiscountValue());
        }
        if (off.compareTo(roomTotal) > 0) {
            off = roomTotal;
        }
        return scale(off);
    }

    private PromoCodeDto toDto(PromoCode promoCode, LocalDate today) {
        boolean applicable = promoCode.isActive()
                && !today.isBefore(promoCode.getStartsOn())
                && !today.isAfter(promoCode.getEndsOn())
                && promoCode.getUsedCount() < promoCode.getMaxUses();
        List<Long> planIds = promoCode.getRatePlans().stream().map(RatePlan::getId).sorted().toList();
        List<String> planNames = promoCode.getRatePlans().stream()
                .map(RatePlan::getName)
                .sorted()
                .toList();
        return new PromoCodeDto(
                promoCode.getId(),
                promoCode.getName(),
                promoCode.getDescription(),
                promoCode.getPromoType().name(),
                promoCode.getOfferCode(),
                promoCode.getOrganizationCode(),
                promoCode.getDiscountType().name(),
                promoCode.getDiscountValue(),
                promoCode.getStartsOn(),
                promoCode.getEndsOn(),
                promoCode.getMaxUses(),
                promoCode.getUsedCount(),
                Math.max(0, promoCode.getMaxUses() - promoCode.getUsedCount()),
                promoCode.isActive(),
                applicable,
                planIds,
                planNames);
    }

    private String buildLabel(PromoCode promoCode) {
        if (promoCode.getDiscountType() == PromoDiscountType.PERCENT) {
            return promoCode.getName() + " · " + promoCode.getDiscountValue().stripTrailingZeros().toPlainString() + "% off";
        }
        return promoCode.getName() + " · ₱" + promoCode.getDiscountValue().setScale(2, RoundingMode.HALF_UP) + " off";
    }

    private PromoCodeType parseType(String raw) {
        try {
            return PromoCodeType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException("Invalid promo type");
        }
    }

    private PromoDiscountType parseDiscountType(String raw) {
        try {
            return PromoDiscountType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException("Invalid discount type");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate today() {
        return LocalDate.now(HOTEL_ZONE);
    }
}
