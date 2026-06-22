package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingAuditLogRepository extends JpaRepository<BookingAuditLog, Long> {

    List<BookingAuditLog> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}
