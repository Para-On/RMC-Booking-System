package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigurationAuditLogRepository extends JpaRepository<ConfigurationAuditLog, Long> {
}
