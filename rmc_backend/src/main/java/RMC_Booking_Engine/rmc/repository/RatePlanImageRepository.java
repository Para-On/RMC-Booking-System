package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RatePlanImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatePlanImageRepository extends JpaRepository<RatePlanImage, Long> {

    List<RatePlanImage> findByRatePlanIdOrderBySortOrderAscIdAsc(Long ratePlanId);

    void deleteByRatePlanId(Long ratePlanId);
}
