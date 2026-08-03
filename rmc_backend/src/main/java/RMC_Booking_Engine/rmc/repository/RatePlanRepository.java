package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {

    List<RatePlan> findByRoomTypeIdAndActiveTrue(Long roomTypeId);

    @Query("SELECT DISTINCT rp FROM RatePlan rp LEFT JOIN FETCH rp.refundPolicy "
            + "LEFT JOIN FETCH rp.roomCategory LEFT JOIN FETCH rp.roomView LEFT JOIN FETCH rp.bedType "
            + "WHERE rp.roomType.id = :roomTypeId AND rp.active = true")
    List<RatePlan> findActiveByRoomTypeIdWithProduct(@Param("roomTypeId") Long roomTypeId);

    List<RatePlan> findByRoomTypeId(Long roomTypeId);

    Optional<RatePlan> findFirstByRoomTypeIdAndActiveTrueOrderByIdAsc(Long roomTypeId);

    List<RatePlan> findByRefundPolicyIdAndActiveTrue(Long refundPolicyId);

    long countByRefundPolicyIdAndActiveTrue(Long refundPolicyId);

    @Query("SELECT rp FROM RatePlan rp JOIN FETCH rp.roomType LEFT JOIN FETCH rp.refundPolicy "
            + "LEFT JOIN FETCH rp.roomCategory LEFT JOIN FETCH rp.roomView LEFT JOIN FETCH rp.bedType ORDER BY rp.id")
    List<RatePlan> findAllWithRoomType();

    @Query("SELECT rp FROM RatePlan rp JOIN FETCH rp.roomType LEFT JOIN FETCH rp.refundPolicy "
            + "LEFT JOIN FETCH rp.roomCategory LEFT JOIN FETCH rp.roomView LEFT JOIN FETCH rp.bedType WHERE rp.id = :id")
    Optional<RatePlan> findByIdWithRoomType(@Param("id") Long id);
}
