package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Promo;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.PromoDiscountType;
import RMC_Booking_Engine.rmc.dto.AppliedPromoDto;
import RMC_Booking_Engine.rmc.dto.CreatePromoRequest;
import RMC_Booking_Engine.rmc.dto.PromoDto;
import RMC_Booking_Engine.rmc.dto.UpdatePromoRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.PromoRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
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
public class PromoService {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Manila");

    private final PromoRepository promoRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Transactional(readOnly = true)
    public List<PromoDto> listStaffPromos() {
        LocalDate today = today();
        return promoRepository.findAllByOrderByStartsOnDescNameAsc().stream()
                .peek(promo -> promo.getRoomTypes().size())
                .map(promo -> toDto(promo, today))
                .toList();
    }

    @Transactional
    public PromoDto create(CreatePromoRequest request) {
        Promo promo = new Promo();
        applyRequest(
                promo,
                request.name(),
                request.description(),
                request.discountType(),
                request.discountValue(),
                request.startsOn(),
                request.endsOn(),
                request.active(),
                request.roomTypeIds());
        promo.setCreatedAt(Instant.now());
        return toDto(promoRepository.save(promo), today());
    }

    @Transactional
    public PromoDto update(Long id, UpdatePromoRequest request) {
        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Promo not found"));
        applyRequest(
                promo,
                request.name(),
                request.description(),
                request.discountType(),
                request.discountValue(),
                request.startsOn(),
                request.endsOn(),
                request.active(),
                request.roomTypeIds());
        promo.setUpdatedAt(Instant.now());
        return toDto(promoRepository.save(promo), today());
    }

    @Transactional
    public void delete(Long id) {
        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Promo not found"));
        promoRepository.delete(promo);
    }

    @Transactional(readOnly = true)
    public Optional<AppliedPromoDto> findBestAppliedPromo(Long roomTypeId, BigDecimal roomTotal) {
        return findBestPromo(roomTypeId, roomTotal).map(match -> toApplied(match.promo(), match.amountOff()));
    }

    @Transactional(readOnly = true)
    public Optional<PromoMatch> findBestPromo(Long roomTypeId, BigDecimal roomTotal) {
        if (roomTypeId == null || roomTotal == null || roomTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        LocalDate today = today();
        Promo bestPromo = null;
        BigDecimal bestOff = BigDecimal.ZERO;
        for (Promo promo : promoRepository.findActiveForRoomTypeOnDate(roomTypeId, today)) {
            BigDecimal amountOff = calculateAmountOff(roomTotal, promo);
            if (amountOff.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (bestPromo == null || amountOff.compareTo(bestOff) > 0) {
                bestPromo = promo;
                bestOff = amountOff;
            }
        }
        return bestPromo == null ? Optional.empty() : Optional.of(new PromoMatch(bestPromo, bestOff));
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

    public LocalDate today() {
        return LocalDate.now(HOTEL_ZONE);
    }

    private void applyRequest(
            Promo promo,
            String name,
            String description,
            String discountType,
            BigDecimal discountValue,
            LocalDate startsOn,
            LocalDate endsOn,
            boolean active,
            List<Long> roomTypeIds) {
        PromoDiscountType type = parseType(discountType);
        validateWindow(startsOn, endsOn, type, discountValue);

        promo.setName(name.trim());
        promo.setDescription(trimOrNull(description));
        promo.setDiscountType(type);
        promo.setDiscountValue(scale(discountValue));
        promo.setStartsOn(startsOn);
        promo.setEndsOn(endsOn);
        promo.setActive(active);
        promo.setRoomTypes(resolveRoomTypes(roomTypeIds));
    }

    private Set<RoomType> resolveRoomTypes(List<Long> roomTypeIds) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) {
            throw new BusinessException("Select at least one room type for this promo");
        }
        Set<Long> unique = new HashSet<>(roomTypeIds);
        List<RoomType> found = roomTypeRepository.findAllById(unique);
        if (found.size() != unique.size()) {
            throw new BusinessException("One or more room types are invalid");
        }
        return new HashSet<>(found);
    }

    private void validateWindow(
            LocalDate startsOn, LocalDate endsOn, PromoDiscountType type, BigDecimal value) {
        if (endsOn.isBefore(startsOn)) {
            throw new BusinessException("Promo end date must be on or after the start date");
        }
        if (type == PromoDiscountType.PERCENT && value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Percent discount cannot exceed 100");
        }
    }

    private PromoDiscountType parseType(String discountType) {
        try {
            return PromoDiscountType.valueOf(discountType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException("Discount type must be PERCENT or FIXED");
        }
    }

    private BigDecimal calculateAmountOff(BigDecimal roomTotal, Promo promo) {
        BigDecimal off;
        if (promo.getDiscountType() == PromoDiscountType.PERCENT) {
            off = roomTotal.multiply(promo.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            off = scale(promo.getDiscountValue());
        }
        if (off.compareTo(roomTotal) > 0) {
            off = scale(roomTotal);
        }
        return off;
    }

    private AppliedPromoDto toApplied(Promo promo, BigDecimal amountOff) {
        return new AppliedPromoDto(
                promo.getId(),
                promo.getName(),
                promo.getDescription(),
                promo.getDiscountType().name(),
                promo.getDiscountValue(),
                amountOff,
                buildLabel(promo));
    }

    private String buildLabel(Promo promo) {
        if (promo.getDiscountType() == PromoDiscountType.PERCENT) {
            return stripTrailingZeros(promo.getDiscountValue()) + "% off";
        }
        return "Save ₱" + stripTrailingZeros(promo.getDiscountValue());
    }

    private String stripTrailingZeros(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private PromoDto toDto(Promo promo, LocalDate today) {
        boolean effective = promo.isActive()
                && !today.isBefore(promo.getStartsOn())
                && !today.isAfter(promo.getEndsOn());
        List<Long> ids = promo.getRoomTypes().stream().map(RoomType::getId).sorted().toList();
        List<String> names = promo.getRoomTypes().stream()
                .map(RoomType::getName)
                .sorted()
                .toList();
        return new PromoDto(
                promo.getId(),
                promo.getName(),
                promo.getDescription(),
                promo.getDiscountType().name(),
                promo.getDiscountValue(),
                promo.getStartsOn(),
                promo.getEndsOn(),
                promo.isActive(),
                effective,
                ids,
                names);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
