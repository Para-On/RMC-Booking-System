package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.StaffNotificationRead;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffNotificationReadRepository
        extends JpaRepository<StaffNotificationRead, StaffNotificationRead.StaffNotificationReadId> {

    @Query("SELECT r.notificationId FROM StaffNotificationRead r WHERE r.staffUserId = :staffUserId")
    Set<Long> findReadNotificationIdsByStaffUserId(@Param("staffUserId") Long staffUserId);
}
