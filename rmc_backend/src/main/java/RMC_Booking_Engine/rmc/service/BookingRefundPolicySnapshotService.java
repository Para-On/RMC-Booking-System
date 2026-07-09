package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingRefundPolicySnapshot;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.repository.BookingRefundPolicySnapshotRepository;
import RMC_Booking_Engine.rmc.repository.RefundPolicyRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingRefundPolicySnapshotService {

    private final RefundPolicyRepository refundPolicyRepository;
    private final BookingRefundPolicySnapshotRepository snapshotRepository;

    @Transactional
    public BookingRefundPolicySnapshot attachSnapshotIfAbsent(Booking booking, RoomType roomType) {
        return snapshotRepository.findByBookingId(booking.getId())
                .orElseGet(() -> snapshotRepository.save(buildSnapshot(booking, roomType)));
    }

    @Transactional(readOnly = true)
    public RefundPolicyService.PolicySnapshot resolveSnapshot(Booking booking) {
        return snapshotRepository.findByBookingId(booking.getId())
                .map(this::toPolicySnapshot)
                .orElseGet(() -> legacySnapshotFromRatePlan(booking));
    }

    private BookingRefundPolicySnapshot buildSnapshot(Booking booking, RoomType roomType) {
        RefundPolicy policy = refundPolicyRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No active refund policy configured"));

        BookingRefundPolicySnapshot snapshot = new BookingRefundPolicySnapshot();
        snapshot.setBooking(booking);
        snapshot.setRefundPolicyId(policy.getId());
        snapshot.setEnabled(policy.getEnabled());
        snapshot.setFullCutoffValue(policy.getFullCutoffValue());
        snapshot.setFullCutoffUnit(policy.getFullCutoffUnit());
        snapshot.setPartialEnabled(policy.getPartialEnabled());
        snapshot.setPartialRefundPercent(policy.getPartialRefundPercent());
        snapshot.setCheckInTime(policy.getCheckInTime());
        snapshot.setTimezone(policy.getTimezone());
        snapshot.setDescription(policy.getDescription());
        snapshot.setRoomRefundable(Boolean.TRUE.equals(roomType.getRefundable()));
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
                snapshot.getCheckInTime(),
                ZoneId.of(snapshot.getTimezone()),
                snapshot.getDescription(),
                Boolean.TRUE.equals(snapshot.getRoomRefundable()));
    }

    private RefundPolicyService.PolicySnapshot legacySnapshotFromRatePlan(Booking booking) {
        var plan = booking.getRatePlan();
        int refundWindowHours = plan.getRefundWindowHours() != null ? plan.getRefundWindowHours() : 48;
        return new RefundPolicyService.PolicySnapshot(
                true,
                refundWindowHours,
                CutoffUnit.HOURS,
                Boolean.TRUE.equals(plan.getAllowLateCancellation()),
                plan.getLateCancelRefundPercent() != null ? plan.getLateCancelRefundPercent() : 50,
                LocalTime.of(14, 0),
                ZoneId.of("Asia/Manila"),
                plan.getCancellationPolicy(),
                Boolean.TRUE.equals(booking.getRoomType().getRefundable()));
    }
}
