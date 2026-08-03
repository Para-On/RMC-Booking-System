package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.CancellationEvaluation;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.PolicySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundPolicyServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");
    private static final LocalTime CHECK_IN = LocalTime.of(14, 0);

    @Mock
    private BookingRefundPolicySnapshotService snapshotService;
    @Mock
    private PricingService pricingService;

    private RefundPolicyService refundPolicyService;

    @BeforeEach
    void setUp() {
        refundPolicyService = new RefundPolicyService(snapshotService, pricingService);
    }

    @Test
    void evaluateCancellation_fullRefundBeforeCutoff() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(5), 1);
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, false, 1, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isTrue();
        assertThat(evaluation.tier()).isEqualTo(CancellationTier.FULL);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void evaluateCancellation_partialRefundWithinCutoffWindow() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(1), 1);
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, false, 1, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.tier()).isEqualTo(CancellationTier.PARTIAL);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void evaluateCancellation_nightsThenPercent_stacks() {
        LocalDate checkIn = LocalDate.now(ZONE).plusDays(1);
        Booking booking = booking(checkIn, 3);
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, true, 1, true));
        when(pricingService.calculateStayPricing(eq(9L), eq(checkIn), eq(checkIn.plusDays(3))))
                .thenReturn(List.of(
                        night(checkIn, "1000.00"),
                        night(checkIn.plusDays(1), "1000.00"),
                        night(checkIn.plusDays(2), "1000.00")));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("3000.00"));

        // fee 1000, remaining 2000, 50% → 1000 refund
        assertThat(evaluation.tier()).isEqualTo(CancellationTier.PARTIAL);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("1000.00");
        assertThat(evaluation.deductionAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    void evaluateCancellation_nightsFeeCappedAtStayLength() {
        LocalDate checkIn = LocalDate.now(ZONE).plusDays(1);
        Booking booking = booking(checkIn, 1);
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, false, 50, true, 2, true));
        when(pricingService.calculateStayPricing(eq(9L), eq(checkIn), eq(checkIn.plusDays(1))))
                .thenReturn(List.of(night(checkIn, "2500.00")));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("2500.00"));

        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("0.00");
        assertThat(evaluation.deductionAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void evaluateCancellation_blocksWhenPartialDisabled() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(1), 1);
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, false, 50, false, 1, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isFalse();
    }

    @Test
    void evaluateCancellation_nonRefundableAllowsCancelWithoutRefundWhenPartialEnabled() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(3), 1);
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, false, 1, false));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.tier()).isEqualTo(CancellationTier.NONE);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("0");
    }

    @Test
    void evaluateCancellation_blocksWhenPolicyDisabled() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(5), 1);
        PolicySnapshot disabled = new PolicySnapshot(
                false, 48, CutoffUnit.HOURS, true, 50, false, 1, CHECK_IN, ZONE, null, true);
        stubPolicy(booking, disabled);

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isFalse();
    }

    private void stubPolicy(Booking booking, PolicySnapshot snapshot) {
        when(snapshotService.resolveSnapshot(booking)).thenReturn(snapshot);
    }

    private PolicySnapshot policy(
            int cutoffValue,
            CutoffUnit unit,
            boolean partialEnabled,
            int partialPercent,
            boolean nightsEnabled,
            int nightsDeducted,
            boolean roomRefundable) {
        return new PolicySnapshot(
                true,
                cutoffValue,
                unit,
                partialEnabled,
                partialPercent,
                nightsEnabled,
                nightsDeducted,
                CHECK_IN,
                ZONE,
                "Test policy",
                roomRefundable);
    }

    private NightlyRateDto night(LocalDate date, String total) {
        return new NightlyRateDto(
                date, new BigDecimal(total), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(total));
    }

    private Booking booking(LocalDate checkIn, int nights) {
        RoomType roomType = new RoomType();
        roomType.setRefundable(true);

        RatePlan ratePlan = new RatePlan();
        ratePlan.setId(9L);
        ratePlan.setRoomType(roomType);

        Booking booking = new Booking();
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkIn.plusDays(nights));
        booking.setRatePlan(ratePlan);
        booking.setRoomType(roomType);
        return booking;
    }
}
