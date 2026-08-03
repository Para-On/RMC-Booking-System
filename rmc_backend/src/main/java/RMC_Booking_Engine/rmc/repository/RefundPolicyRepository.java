package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {

    Optional<RefundPolicy> findFirstByActiveTrueOrderByIdAsc();

    List<RefundPolicy> findByActiveTrueOrderByNameAsc();

    Optional<RefundPolicy> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCaseAndActiveTrueAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);
}
