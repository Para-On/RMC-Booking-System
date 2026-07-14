package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.Guest;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    @Query("SELECT g FROM Guest g WHERE LOWER(g.email) = LOWER(:email)")
    Optional<Guest> findByEmailIgnoreCase(@Param("email") String email);

    @Query(
            value = """
            SELECT g.id, g.full_name, g.email, g.phone,
                   (
                       SELECT COUNT(DISTINCT b.id)
                       FROM booking b
                       LEFT JOIN booking_additional_guest bag
                           ON bag.booking_id = b.id AND bag.guest_id = g.id
                       WHERE b.guest_id = g.id OR bag.guest_id = g.id
                   ),
                   (
                       SELECT COALESCE(SUM(CASE WHEN b.checked_out_at IS NOT NULL THEN 1 ELSE 0 END), 0)
                       FROM booking b
                       WHERE b.guest_id = g.id
                   ),
                   (
                       SELECT COALESCE(SUM(CASE WHEN b.status IN ('CONFIRMED', 'CONFIRMED_PAY_LATER', 'CANCELLED', 'NO_SHOW')
                           THEN b.quoted_total ELSE 0 END), 0)
                       FROM booking b
                       WHERE b.guest_id = g.id
                   )
            FROM guest g
            WHERE EXISTS (SELECT 1 FROM booking b WHERE b.guest_id = g.id)
              AND (:query IS NULL OR :query = ''
                OR LOWER(g.full_name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR g.phone LIKE CONCAT('%', :query, '%'))
            ORDER BY g.full_name ASC, g.id ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM guest g
            WHERE EXISTS (SELECT 1 FROM booking b WHERE b.guest_id = g.id)
              AND (:query IS NULL OR :query = ''
                OR LOWER(g.full_name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR g.phone LIKE CONCAT('%', :query, '%'))
            """,
            nativeQuery = true)
    Page<Object[]> searchGuestSummaries(@Param("query") String query, Pageable pageable);
}
