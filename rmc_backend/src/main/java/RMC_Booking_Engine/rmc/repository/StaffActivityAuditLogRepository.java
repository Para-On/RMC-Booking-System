package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.StaffActivityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffActivityAuditLogRepository extends JpaRepository<StaffActivityAuditLog, Long> {

    Page<StaffActivityAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
