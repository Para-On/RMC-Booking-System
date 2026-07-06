package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomUnitRepository extends JpaRepository<RoomUnit, Long> {

    List<RoomUnit> findByRoomTypeIdAndStatusOrderByRoomNumberAsc(
            Long roomTypeId, RoomUnitStatus status);

    List<RoomUnit> findByRoomTypeIdOrderByRoomNumberAsc(Long roomTypeId);

    List<RoomUnit> findByRoomTypeIsNullOrderByRoomNumberAsc();

    @Query("SELECT u FROM RoomUnit u WHERE u.id IN :ids")
    List<RoomUnit> findAllByIdIn(@Param("ids") List<Long> ids);

    Optional<RoomUnit> findByIdAndRoomTypeId(Long id, Long roomTypeId);

    boolean existsByRoomNumber(String roomNumber);

    Optional<RoomUnit> findByRoomNumberIgnoreCase(String roomNumber);

    long countByRoomTypeId(Long roomTypeId);

    long countByStatusOptionId(Long statusOptionId);

    List<RoomUnit> findAllByOrderByRoomNumberAsc();
}
