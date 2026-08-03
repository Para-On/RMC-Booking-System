package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.PromoCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    List<PromoCode> findAllByOrderByStartsOnDescNameAsc();

    Optional<PromoCode> findByOfferCodeIgnoreCase(String offerCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PromoCode p
            SET p.usedCount = p.usedCount + 1
            WHERE p.id = :id AND p.usedCount < p.maxUses
            """)
    int tryIncrementUsedCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PromoCode p
            SET p.usedCount = p.usedCount - 1
            WHERE p.id = :id AND p.usedCount > 0
            """)
    int tryDecrementUsedCount(@Param("id") Long id);
}
