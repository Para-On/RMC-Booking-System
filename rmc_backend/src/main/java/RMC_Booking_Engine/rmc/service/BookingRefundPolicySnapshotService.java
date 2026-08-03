package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingRefundPolicySnapshot;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.repository.BookingRefundPolicySnapshotRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingRefundPolicySnapshotService {

    private final BookingRefundPolicySnapshotRepository snapshotRepository;
    private final RatePlanRepository ratePlanRepository;

    @Transactional
    public BookingRefundPolicySnapshot attachSnapshotIfAbsent(Booking booking, RoomType roomType) {
        return snapshotRepository.findByBookingId(booking.getId())
                .orElseGet(() -> snapshotRepository.save(buildSnapshot(booking)));
    }

    @Transactional(readOnly = true)
    public RefundPolicyService.PolicySnapshot resolveSnapshot(Booking booking) {
        return snapshotRepository.findByBookingId(booking.getId())
                .map(this::toPolicySnapshot)
                .orElseGet(() -> legacySnapshotFromRatePlan(booking));
    }

    private BookingRefundPolicySnapshot buildSnapshot(Booking booking) {
        RatePlan plan = resolveRatePlanWithPolicy(booking);
        RefundPolicy policy = plan.getRefundPolicy();
        if (policy == null) {
            throw new IllegalStateException("Rate plan has no refund policy for snapshot");
        }

        BookingRefundPolicySnapshot snapshot = new BookingRefundPolicySnapshot();
        snapshot.setBooking(booking);
        snapshot.setRefundPolicyId(policy.getId());
        snapshot.setEnabled(Boolean.TRUE.equals(policy.getEnabled()));
        snapshot.setFullCutoffValue(policy.getFullCutoffValue() != null ? policy.getFullCutoffValue() : 48);
        snapshot.setFullCutoffUnit(
                policy.getFullCutoffUnit() != null ? policy.getFullCutoffUnit() : CutoffUnit.HOURS);
        snapshot.setPartialEnabled(Boolean.TRUE.equals(policy.getPartialEnabled()));
        snapshot.setPartialRefundPercent(
                policy.getPartialRefundPercent() != null ? policy.getPartialRefundPercent() : 50);
        snapshot.setNightsDeductionEnabled(Boolean.TRUE.equals(policy.getNightsDeductionEnabled()));
        snapshot.setNightsDeducted(policy.getNightsDeducted() != null ? policy.getNightsDeducted() : 1);
        snapshot.setCheckInTime(
                policy.getCheckInTime() != null ? policy.getCheckInTime() : LocalTime.of(14, 0));
        snapshot.setTimezone(
                policy.getTimezone() != null && !policy.getTimezone().isBlank()
                        ? policy.getTimezone()
                        : "Asia/Manila");
        snapshot.setDescription(policy.getDescription());
        snapshot.setRoomRefundable(Boolean.TRUE.equals(policy.getRefundable()));
        snapshot.setSnapshottedAt(Instant.now());
        return snapshot;
    }

    private RefundPolicyService.PolicySnapshot toPolicySnapshot(BookingRefundPolicySnapshot snapshot) {
        return new RefundPolicyService.PolicySnapshot(
                Boolean.TRUE.equals(snapshot.getEnabled()),
                snapshot.getFullCutoffValue(),
                snapshot.getFullCutoffUnit(),
                Boolean.TRUE.equals(snapshot.getPartialEnabled()),
                snapshot.getPartialRefundPercent(),
                Boolean.TRUE.equals(snapshot.getNightsDeductionEnabled()),
                snapshot.getNightsDeducted() != null ? snapshot.getNightsDeducted() : 1,
                snapshot.getCheckInTime(),
                ZoneId.of(snapshot.getTimezone()),
                snapshot.getDescription(),
                Boolean.TRUE.equals(snapshot.getRoomRefundable()));
    }

    private RefundPolicyService.PolicySnapshot legacySnapshotFromRatePlan(Booking booking) {
        RatePlan plan = resolveRatePlanWithPolicy(booking);
        RefundPolicy linked = plan.getRefundPolicy();
        if (linked != null) {
            return new RefundPolicyService.PolicySnapshot(
                    Boolean.TRUE.equals(linked.getEnabled()),
                    linked.getFullCutoffValue() != null ? linked.getFullCutoffValue() : 48,
                    linked.getFullCutoffUnit() != null ? linked.getFullCutoffUnit() : CutoffUnit.HOURS,
                    Boolean.TRUE.equals(linked.getPartialEnabled()),
                    linked.getPartialRefundPercent() != null ? linked.getPartialRefundPercent() : 50,
                    Boolean.TRUE.equals(linked.getNightsDeductionEnabled()),
                    linked.getNightsDeducted() != null ? linked.getNightsDeducted() : 1,
                    linked.getCheckInTime() != null ? linked.getCheckInTime() : LocalTime.of(14, 0),
                    ZoneId.of(
                            linked.getTimezone() != null && !linked.getTimezone().isBlank()
                                    ? linked.getTimezone()
                                    : "Asia/Manila"),
                    linked.getDescription(),
                    Boolean.TRUE.equals(linked.getRefundable()));
        }
        return new RefundPolicyService.PolicySnapshot(
                Boolean.TRUE.equals(plan.getPolicyEnabled()),
                plan.getFullCutoffValue() != null ? plan.getFullCutoffValue() : 48,
                plan.getFullCutoffUnit() != null ? plan.getFullCutoffUnit() : CutoffUnit.HOURS,
                Boolean.TRUE.equals(plan.getPartialEnabled()),
                plan.getPartialRefundPercent() != null ? plan.getPartialRefundPercent() : 50,
                false,
                1,
                plan.getCheckInTime() != null ? plan.getCheckInTime() : LocalTime.of(14, 0),
                ZoneId.of(
                        plan.getTimezone() != null && !plan.getTimezone().isBlank()
                                ? plan.getTimezone()
                                : "Asia/Manila"),
                plan.getPolicyDescription() != null ? plan.getPolicyDescription() : plan.getCancellationPolicy(),
                Boolean.TRUE.equals(plan.getRefundable()));
    }

    private RatePlan resolveRatePlanWithPolicy(Booking booking) {
        RatePlan plan = booking.getRatePlan();
        if (plan == null || plan.getId() == null) {
            throw new IllegalStateException("Booking has no rate plan for refund snapshot");
        }
        return ratePlanRepository.findByIdWithRoomType(plan.getId()).orElse(plan);
    }
}
