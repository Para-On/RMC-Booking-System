package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingRefundPolicySnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRefundPolicySnapshotRepository extends JpaRepository<BookingRefundPolicySnapshot, Long> {

    Optional<BookingRefundPolicySnapshot> findByBookingId(Long bookingId);
}
