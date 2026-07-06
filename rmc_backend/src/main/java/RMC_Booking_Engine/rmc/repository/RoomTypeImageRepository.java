package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RoomTypeImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {

    List<RoomTypeImage> findByRoomTypeIdOrderBySortOrderAscIdAsc(Long roomTypeId);
}
