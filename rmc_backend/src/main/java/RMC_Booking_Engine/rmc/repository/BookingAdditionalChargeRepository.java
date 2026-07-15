package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingAdditionalCharge;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingAdditionalChargeRepository extends JpaRepository<BookingAdditionalCharge, Long> {

    List<BookingAdditionalCharge> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    Optional<BookingAdditionalCharge> findByIdAndBookingId(Long id, Long bookingId);

    Optional<BookingAdditionalCharge> findByMayaRequestRef(String mayaRequestRef);

    Optional<BookingAdditionalCharge> findByMayaCheckoutId(String mayaCheckoutId);

    @Query("""
            SELECT c FROM BookingAdditionalCharge c
            JOIN FETCH c.booking b
            JOIN FETCH b.guest
            WHERE c.id = :id
            """)
    Optional<BookingAdditionalCharge> findByIdWithBooking(@Param("id") Long id);
}
