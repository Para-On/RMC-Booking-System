package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.CancellationEvaluation;
import RMC_Booking_Engine.rmc.service.RefundPolicyService.PolicySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    private RefundPolicyService refundPolicyService;

    @BeforeEach
    void setUp() {
        refundPolicyService = new RefundPolicyService(snapshotService);
    }

    @Test
    void evaluateCancellation_fullRefundBeforeCutoff() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(5));
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isTrue();
        assertThat(evaluation.tier()).isEqualTo(CancellationTier.FULL);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("1000.00");
        assertThat(evaluation.deductionAmount()).isEqualByComparingTo("0");
        assertThat(evaluation.refundPercentApplied()).isEqualTo(100);
    }

    @Test
    void evaluateCancellation_partialRefundWithinCutoffWindow() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(1));
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isTrue();
        assertThat(evaluation.tier()).isEqualTo(CancellationTier.PARTIAL);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("500.00");
        assertThat(evaluation.deductionAmount()).isEqualByComparingTo("500.00");
        assertThat(evaluation.refundPercentApplied()).isEqualTo(50);
    }

    @Test
    void evaluateCancellation_blocksWhenPartialDisabled() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(1));
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, false, 50, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.blockReason()).contains("Cancellation window has passed");
    }

    @Test
    void evaluateCancellation_nonRefundableAllowsCancelWithoutRefundWhenPartialEnabled() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(3));
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, false));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isTrue();
        assertThat(evaluation.tier()).isEqualTo(CancellationTier.NONE);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("0");
    }

    @Test
    void evaluateCancellation_allowsSameDayCheckInBeforeConfiguredCheckInTime() {
        Booking booking = booking(LocalDate.now(ZONE));
        stubPolicy(booking, policy(24, CutoffUnit.HOURS, true, 50, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        if (refundPolicyService.hasCheckInPassed(booking)) {
            assertThat(evaluation.allowed()).isFalse();
        } else {
            assertThat(evaluation.allowed()).isTrue();
        }
    }

    @Test
    void evaluateCancellation_blocksAfterCheckInDate() {
        Booking booking = booking(LocalDate.now(ZONE).minusDays(1));
        stubPolicy(booking, policy(48, CutoffUnit.HOURS, true, 50, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.blockReason()).contains("Check-in has already passed");
    }

    @Test
    void evaluateCancellation_supportsDayBasedCutoff() {
        LocalDate checkIn = LocalDate.now(ZONE).plusDays(10);
        Booking booking = booking(checkIn);
        stubPolicy(booking, policy(7, CutoffUnit.DAYS, true, 50, true));

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("10000.00"));

        assertThat(evaluation.tier()).isEqualTo(CancellationTier.FULL);
        assertThat(evaluation.refundEligibleAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void evaluateCancellation_blocksWhenPolicyDisabled() {
        Booking booking = booking(LocalDate.now(ZONE).plusDays(5));
        PolicySnapshot disabled = new PolicySnapshot(
                false, 48, CutoffUnit.HOURS, true, 50, CHECK_IN, ZONE, null, true);
        stubPolicy(booking, disabled);

        CancellationEvaluation evaluation =
                refundPolicyService.evaluateCancellation(booking, new BigDecimal("1000.00"));

        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.blockReason()).contains("Online cancellation is not available");
    }

    private void stubPolicy(Booking booking, PolicySnapshot snapshot) {
        when(snapshotService.resolveSnapshot(booking)).thenReturn(snapshot);
    }

    private PolicySnapshot policy(
            int cutoffValue, CutoffUnit unit, boolean partialEnabled, int partialPercent, boolean roomRefundable) {
        return new PolicySnapshot(
                true,
                cutoffValue,
                unit,
                partialEnabled,
                partialPercent,
                CHECK_IN,
                ZONE,
                "Test policy",
                roomRefundable);
    }

    private Booking booking(LocalDate checkIn) {
        RoomType roomType = new RoomType();
        roomType.setRefundable(true);

        RatePlan ratePlan = new RatePlan();
        ratePlan.setRoomType(roomType);

        Booking booking = new Booking();
        booking.setCheckInDate(checkIn);
        booking.setRatePlan(ratePlan);
        booking.setRoomType(roomType);
        return booking;
    }
}
