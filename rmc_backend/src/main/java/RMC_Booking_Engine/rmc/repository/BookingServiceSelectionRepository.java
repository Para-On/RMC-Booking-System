package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingServiceSelection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingServiceSelectionRepository extends JpaRepository<BookingServiceSelection, Long> {

    List<BookingServiceSelection> findByBookingIdOrderByIdAsc(Long bookingId);

    boolean existsByServiceAddonId(Long serviceAddonId);
}
