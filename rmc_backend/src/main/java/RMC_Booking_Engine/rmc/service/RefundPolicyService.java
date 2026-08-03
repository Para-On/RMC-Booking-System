package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.dto.RefundPolicyPreviewDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundPolicyService {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    private final BookingRefundPolicySnapshotService snapshotService;
    private final PricingService pricingService;

    public record PolicySnapshot(
            boolean enabled,
            int fullCutoffValue,
            CutoffUnit fullCutoffUnit,
            boolean partialEnabled,
            int partialRefundPercent,
            boolean nightsDeductionEnabled,
            int nightsDeducted,
            LocalTime checkInTime,
            ZoneId timezone,
            String description,
            boolean roomRefundable) {
    }

    public record CancellationEvaluation(
            boolean allowed,
            CancellationTier tier,
            BigDecimal refundEligibleAmount,
            BigDecimal deductionAmount,
            Integer refundPercentApplied,
            String blockReason,
            String policySummary,
            Instant fullCutoffAt,
            Instant checkInAt) {

        public static CancellationEvaluation blocked(String reason) {
            return new CancellationEvaluation(
                    false, CancellationTier.NONE, BigDecimal.ZERO, BigDecimal.ZERO,
                    0, reason, null, null, null);
        }

        public static CancellationEvaluation allowed(
                CancellationTier tier,
                BigDecimal refundAmount,
                BigDecimal deductionAmount,
                int refundPercent,
                String policySummary,
                Instant fullCutoffAt,
                Instant checkInAt) {
            return new CancellationEvaluation(
                    true, tier, refundAmount, deductionAmount, refundPercent,
                    null, policySummary, fullCutoffAt, checkInAt);
        }
    }

    public CancellationEvaluation evaluateCancellation(Booking booking, BigDecimal paidAmount) {
        PolicySnapshot policy = snapshotService.resolveSnapshot(booking);
        BigDecimal safePaid = paidAmount != null ? paidAmount.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        ZonedDateTime cancelAt = ZonedDateTime.now(policy.timezone());
        ZonedDateTime checkInAt = checkInDateTime(booking, policy);
        ZonedDateTime fullCutoffAt = subtractCutoff(checkInAt, policy.fullCutoffValue(), policy.fullCutoffUnit());
        Instant checkInInstant = checkInAt.toInstant();
        Instant fullCutoffInstant = fullCutoffAt.toInstant();

        if (!policy.enabled()) {
            return CancellationEvaluation.blocked("Online cancellation is not available");
        }

        if (!cancelAt.isBefore(checkInAt)) {
            return CancellationEvaluation.blocked("Check-in has already passed for this booking");
        }

        if (!policy.roomRefundable()) {
            if (policy.partialEnabled() || policy.nightsDeductionEnabled()) {
                return CancellationEvaluation.allowed(
                        CancellationTier.NONE,
                        BigDecimal.ZERO,
                        safePaid,
                        0,
                        buildNonRefundableSummary(policy, fullCutoffAt, checkInAt),
                        fullCutoffInstant,
                        checkInInstant);
            }
            return CancellationEvaluation.blocked("This non-refundable booking cannot be cancelled online");
        }

        if (cancelAt.isBefore(fullCutoffAt)) {
            return CancellationEvaluation.allowed(
                    CancellationTier.FULL,
                    safePaid,
                    BigDecimal.ZERO,
                    100,
                    buildFullRefundSummary(policy, fullCutoffAt),
                    fullCutoffInstant,
                    checkInInstant);
        }

        boolean hasPartialPath = policy.partialEnabled() || policy.nightsDeductionEnabled();
        if (!hasPartialPath) {
            return CancellationEvaluation.blocked("Cancellation window has passed for this booking");
        }

        BigDecimal remaining = safePaid;
        BigDecimal nightsFee = BigDecimal.ZERO;
        if (policy.nightsDeductionEnabled()) {
            nightsFee = calculateNightsFee(booking, policy.nightsDeducted()).min(remaining);
            remaining = remaining.subtract(nightsFee).max(BigDecimal.ZERO);
        }

        BigDecimal refundAmount = remaining;
        int refundPercent = 100;
        if (policy.partialEnabled()) {
            refundAmount = calculatePartialRefund(remaining, policy.partialRefundPercent());
            refundPercent = policy.partialRefundPercent();
        }
        BigDecimal deduction = safePaid.subtract(refundAmount).max(BigDecimal.ZERO);
        return CancellationEvaluation.allowed(
                CancellationTier.PARTIAL,
                refundAmount,
                deduction,
                refundPercent,
                buildPartialRefundSummary(policy, fullCutoffAt, nightsFee, refundPercent),
                fullCutoffInstant,
                checkInInstant);
    }

    public RefundPolicyPreviewDto previewCancellation(Booking booking, BigDecimal paidAmount) {
        CancellationEvaluation evaluation = evaluateCancellation(booking, paidAmount);
        return new RefundPolicyPreviewDto(
                evaluation.policySummary(),
                evaluation.tier() != null ? evaluation.tier().name() : null,
                evaluation.refundEligibleAmount(),
                evaluation.refundPercentApplied(),
                evaluation.deductionAmount(),
                evaluation.fullCutoffAt(),
                evaluation.checkInAt(),
                evaluation.allowed(),
                evaluation.blockReason());
    }

    public boolean isWithinRefundWindow(Booking booking) {
        return evaluateCancellation(booking, BigDecimal.ONE).tier() == CancellationTier.FULL;
    }

    public boolean hasCheckInPassed(Booking booking) {
        PolicySnapshot policy = snapshotService.resolveSnapshot(booking);
        return !ZonedDateTime.now(policy.timezone()).isBefore(checkInDateTime(booking, policy));
    }

    public void assertCancellationAllowed(Booking booking, BigDecimal paidAmount) {
        CancellationEvaluation evaluation = evaluateCancellation(booking, paidAmount);
        if (!evaluation.allowed()) {
            throw new BusinessException(evaluation.blockReason());
        }
    }

    private BigDecimal calculateNightsFee(Booking booking, int configuredNights) {
        if (booking.getRatePlan() == null
                || booking.getCheckInDate() == null
                || booking.getCheckOutDate() == null) {
            return BigDecimal.ZERO;
        }
        long stayNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (stayNights <= 0) {
            return BigDecimal.ZERO;
        }
        int nightsToCharge = Math.min(Math.max(configuredNights, 1), (int) stayNights);
        try {
            List<NightlyRateDto> nights = pricingService.calculateStayPricing(
                    booking.getRatePlan().getId(), booking.getCheckInDate(), booking.getCheckOutDate());
            return nights.stream()
                    .limit(nightsToCharge)
                    .map(NightlyRateDto::taxInclusiveTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private ZonedDateTime checkInDateTime(Booking booking, PolicySnapshot policy) {
        return booking.getCheckInDate().atTime(policy.checkInTime()).atZone(policy.timezone());
    }

    private ZonedDateTime subtractCutoff(ZonedDateTime checkInAt, int value, CutoffUnit unit) {
        return switch (unit) {
            case HOURS -> checkInAt.minusHours(value);
            case DAYS -> checkInAt.minusDays(value);
        };
    }

    private BigDecimal calculatePartialRefund(BigDecimal paidAmount, int percent) {
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int safePercent = Math.min(100, Math.max(0, percent));
        return paidAmount
                .multiply(BigDecimal.valueOf(safePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String buildFullRefundSummary(PolicySnapshot policy, ZonedDateTime fullCutoffAt) {
        return "Full refund if cancelled before "
                + fullCutoffAt.format(DISPLAY_FORMAT)
                + " (" + formatCutoff(policy) + " before check-in).";
    }

    private String buildPartialRefundSummary(
            PolicySnapshot policy, ZonedDateTime fullCutoffAt, BigDecimal nightsFee, int refundPercent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Late cancellation after ").append(fullCutoffAt.format(DISPLAY_FORMAT)).append(':');
        if (policy.nightsDeductionEnabled()) {
            sb.append(" retain up to ")
                    .append(policy.nightsDeducted())
                    .append(" night(s)");
            if (nightsFee != null && nightsFee.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(" (₱").append(nightsFee.setScale(2, RoundingMode.HALF_UP)).append(')');
            }
            sb.append(policy.partialEnabled() ? ", then " : ".");
        }
        if (policy.partialEnabled()) {
            sb.append(refundPercent).append("% of remaining amount refundable before check-in.");
        } else if (!policy.nightsDeductionEnabled()) {
            sb.append(" partial refund rules apply before check-in.");
        }
        return sb.toString();
    }

    private String buildNonRefundableSummary(
            PolicySnapshot policy, ZonedDateTime fullCutoffAt, ZonedDateTime checkInAt) {
        return "Non-refundable room. Cancellation allowed before "
                + checkInAt.format(DISPLAY_FORMAT)
                + " with no refund.";
    }

    private String formatCutoff(PolicySnapshot policy) {
        return policy.fullCutoffValue() + " "
                + (policy.fullCutoffUnit() == CutoffUnit.DAYS ? "day(s)" : "hour(s)");
    }
}
