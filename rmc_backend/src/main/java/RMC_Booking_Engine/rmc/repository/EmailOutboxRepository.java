package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.EmailOutbox;
import RMC_Booking_Engine.rmc.domain.enums.EmailOutboxStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    boolean existsByBookingId(Long bookingId);

    @Query("""
            SELECT e FROM EmailOutbox e
            WHERE e.status = :status AND e.nextRetryAt <= :now
            ORDER BY e.nextRetryAt ASC
            """)
    List<EmailOutbox> findDueForProcessing(
            @Param("status") EmailOutboxStatus status,
            @Param("now") Instant now);
}
