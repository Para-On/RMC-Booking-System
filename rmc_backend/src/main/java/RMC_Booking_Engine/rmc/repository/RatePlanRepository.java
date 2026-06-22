package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {

    List<RatePlan> findByRoomTypeIdAndActiveTrue(Long roomTypeId);

    Optional<RatePlan> findFirstByRoomTypeIdAndActiveTrueOrderByIdAsc(Long roomTypeId);

    @Query("SELECT rp FROM RatePlan rp JOIN FETCH rp.roomType ORDER BY rp.id")
    List<RatePlan> findAllWithRoomType();

    @Query("SELECT rp FROM RatePlan rp JOIN FETCH rp.roomType WHERE rp.id = :id")
    Optional<RatePlan> findByIdWithRoomType(@Param("id") Long id);
}
