package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.Promo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromoRepository extends JpaRepository<Promo, Long> {

    List<Promo> findAllByOrderByStartsOnDescNameAsc();

    @Query("""
            SELECT DISTINCT p FROM Promo p
            JOIN p.roomTypes rt
            WHERE p.active = TRUE
              AND rt.id = :roomTypeId
              AND p.startsOn <= :onDate
              AND p.endsOn >= :onDate
            """)
    List<Promo> findActiveForRoomTypeOnDate(
            @Param("roomTypeId") Long roomTypeId, @Param("onDate") LocalDate onDate);
}
