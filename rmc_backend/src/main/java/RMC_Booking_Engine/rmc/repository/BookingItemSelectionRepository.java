package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingItemSelection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemSelectionRepository extends JpaRepository<BookingItemSelection, Long> {

    List<BookingItemSelection> findByBookingIdOrderByIdAsc(Long bookingId);
}
