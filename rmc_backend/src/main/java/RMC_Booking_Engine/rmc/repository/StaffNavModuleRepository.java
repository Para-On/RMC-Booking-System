package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.StaffNavModule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffNavModuleRepository extends JpaRepository<StaffNavModule, Long> {

    List<StaffNavModule> findAllByOrderBySortOrderAsc();

    boolean existsByModuleKey(String moduleKey);

    boolean existsByModuleKeyAndIdNot(String moduleKey, Long id);

    long countByParentId(Long parentId);

    Optional<StaffNavModule> findByModuleKey(String moduleKey);

    Optional<StaffNavModule> findByPathAndEnabledTrue(String path);
}
