package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.DailyRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyRateRepository extends JpaRepository<DailyRate, Long> {

    @Query("""
            SELECT dr FROM DailyRate dr
            WHERE dr.ratePlan.id = :ratePlanId
              AND dr.rateDate >= :checkIn
              AND dr.rateDate < :checkOut
            ORDER BY dr.rateDate
            """)
    List<DailyRate> findRatesForStay(
            @Param("ratePlanId") Long ratePlanId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    Optional<DailyRate> findByRatePlanIdAndRateDate(Long ratePlanId, LocalDate rateDate);

    Optional<DailyRate> findFirstByRatePlanIdOrderByRateDateDesc(Long ratePlanId);

    void deleteByRatePlanId(Long ratePlanId);
}
