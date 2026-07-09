package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RoomItemAddon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomItemAddonRepository extends JpaRepository<RoomItemAddon, Long> {

    List<RoomItemAddon> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<RoomItemAddon> findAllByOrderBySortOrderAscNameAsc();
}
