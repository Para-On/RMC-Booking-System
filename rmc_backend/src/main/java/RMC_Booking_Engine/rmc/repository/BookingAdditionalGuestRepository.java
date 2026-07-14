package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingAdditionalGuest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingAdditionalGuestRepository extends JpaRepository<BookingAdditionalGuest, Long> {

    List<BookingAdditionalGuest> findByBookingIdOrderBySortOrderAscIdAsc(Long bookingId);
}
