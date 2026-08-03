package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingRefundPolicySnapshot;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import RMC_Booking_Engine.rmc.repository.BookingRefundPolicySnapshotRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingRefundPolicySnapshotServiceTest {

    @Mock
    private BookingRefundPolicySnapshotRepository snapshotRepository;
    @Mock
    private RatePlanRepository ratePlanRepository;

    private BookingRefundPolicySnapshotService service;

    @BeforeEach
    void setUp() {
        service = new BookingRefundPolicySnapshotService(snapshotRepository, ratePlanRepository);
    }

    @Test
    void attachSnapshot_copiesFieldsFromLinkedRefundPolicy() {
        RoomType roomType = new RoomType();
        roomType.setId(1L);

        RefundPolicy policy = new RefundPolicy();
        policy.setId(55L);
        policy.setEnabled(true);
        policy.setFullCutoffValue(72);
        policy.setFullCutoffUnit(CutoffUnit.HOURS);
        policy.setPartialEnabled(true);
        policy.setPartialRefundPercent(25);
        policy.setNightsDeductionEnabled(true);
        policy.setNightsDeducted(2);
        policy.setCheckInTime(LocalTime.of(15, 0));
        policy.setTimezone("Asia/Manila");
        policy.setDescription("Flexible plan policy");
        policy.setRefundable(true);

        RatePlan plan = new RatePlan();
        plan.setId(10L);
        plan.setRefundPolicy(policy);

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setRatePlan(plan);
        booking.setRoomType(roomType);

        when(snapshotRepository.findByBookingId(100L)).thenReturn(Optional.empty());
        when(ratePlanRepository.findByIdWithRoomType(10L)).thenReturn(Optional.of(plan));
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingRefundPolicySnapshot snapshot = service.attachSnapshotIfAbsent(booking, roomType);

        ArgumentCaptor<BookingRefundPolicySnapshot> captor =
                ArgumentCaptor.forClass(BookingRefundPolicySnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        BookingRefundPolicySnapshot saved = captor.getValue();

        assertThat(saved.getFullCutoffValue()).isEqualTo(72);
        assertThat(saved.getPartialRefundPercent()).isEqualTo(25);
        assertThat(saved.getNightsDeductionEnabled()).isTrue();
        assertThat(saved.getNightsDeducted()).isEqualTo(2);
        assertThat(saved.getDescription()).isEqualTo("Flexible plan policy");
        assertThat(saved.getRoomRefundable()).isTrue();
        assertThat(saved.getRefundPolicyId()).isEqualTo(55L);
        assertThat(snapshot.getFullCutoffValue()).isEqualTo(72);
    }

    @Test
    void livePolicyChangeDoesNotRewriteExistingSnapshot_impliedByFindFirst() {
        BookingRefundPolicySnapshot existing = new BookingRefundPolicySnapshot();
        existing.setFullCutoffValue(99);
        existing.setNightsDeducted(1);

        when(snapshotRepository.findByBookingId(101L)).thenReturn(Optional.of(existing));

        Booking booking = new Booking();
        booking.setId(101L);
        BookingRefundPolicySnapshot result = service.attachSnapshotIfAbsent(booking, new RoomType());

        assertThat(result.getFullCutoffValue()).isEqualTo(99);
    }
}
