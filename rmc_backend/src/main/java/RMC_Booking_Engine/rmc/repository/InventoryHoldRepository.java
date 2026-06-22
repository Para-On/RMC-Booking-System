package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.InventoryHold;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryHoldRepository extends JpaRepository<InventoryHold, Long> {

    List<InventoryHold> findByBookingId(Long bookingId);

    @Query(value = """
            SELECT COALESCE(SUM(h.held_count), 0)
            FROM inventory_hold h
            WHERE h.room_type_id = :roomTypeId
              AND h.hold_date = :holdDate
              AND h.status = 'ACTIVE'
              AND (h.expires_at IS NULL OR h.expires_at > UTC_TIMESTAMP())
            """, nativeQuery = true)
    int countActiveHeldUnits(@Param("roomTypeId") Long roomTypeId, @Param("holdDate") LocalDate holdDate);

    List<InventoryHold> findByBookingIdAndStatus(Long bookingId, HoldStatus status);
}
