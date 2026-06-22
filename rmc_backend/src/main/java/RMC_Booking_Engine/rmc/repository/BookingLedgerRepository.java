package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingLedgerRepository extends JpaRepository<BookingLedger, Long> {

    List<BookingLedger> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
