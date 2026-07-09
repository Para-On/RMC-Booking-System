package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RoomServiceAddon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomServiceAddonRepository extends JpaRepository<RoomServiceAddon, Long> {

    List<RoomServiceAddon> findByActiveTrueOrderBySortOrderAscTitleAsc();

    List<RoomServiceAddon> findAllByOrderBySortOrderAscTitleAsc();
}
