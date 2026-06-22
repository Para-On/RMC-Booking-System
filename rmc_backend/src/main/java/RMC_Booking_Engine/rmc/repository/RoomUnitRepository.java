package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomUnitRepository extends JpaRepository<RoomUnit, Long> {

    List<RoomUnit> findByRoomTypeIdAndStatusOrderByRoomNumberAsc(
            Long roomTypeId, RoomUnitStatus status);

    Optional<RoomUnit> findByIdAndRoomTypeId(Long id, Long roomTypeId);
}
