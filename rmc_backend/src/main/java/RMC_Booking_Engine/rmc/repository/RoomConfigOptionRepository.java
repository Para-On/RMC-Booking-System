package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RoomConfigOption;
import RMC_Booking_Engine.rmc.domain.enums.RoomConfigOptionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomConfigOptionRepository extends JpaRepository<RoomConfigOption, Long> {

    List<RoomConfigOption> findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(
            RoomConfigOptionType optionType);

    Optional<RoomConfigOption> findByIdAndOptionTypeAndActiveTrue(Long id, RoomConfigOptionType optionType);

    boolean existsByOptionTypeAndLabelIgnoreCase(RoomConfigOptionType optionType, String label);

    boolean existsByOptionTypeAndLabelIgnoreCaseAndIdNot(
            RoomConfigOptionType optionType, String label, Long id);
}
