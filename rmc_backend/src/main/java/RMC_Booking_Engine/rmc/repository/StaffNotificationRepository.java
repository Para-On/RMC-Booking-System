package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.StaffNotification;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffNotificationRepository extends JpaRepository<StaffNotification, Long> {

    List<StaffNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(
            value =
                    """
            SELECT COUNT(*) FROM staff_notification n
            WHERE n.id NOT IN (
                SELECT r.notification_id FROM staff_notification_read r
                WHERE r.staff_user_id = :staffUserId
            )
            """,
            nativeQuery = true)
    long countUnreadForStaff(@Param("staffUserId") Long staffUserId);

    @Query(
            value =
                    """
            SELECT n.id FROM staff_notification n
            WHERE n.id NOT IN (
                SELECT r.notification_id FROM staff_notification_read r
                WHERE r.staff_user_id = :staffUserId
            )
            """,
            nativeQuery = true)
    List<Long> findUnreadIdsForStaff(@Param("staffUserId") Long staffUserId);
}
