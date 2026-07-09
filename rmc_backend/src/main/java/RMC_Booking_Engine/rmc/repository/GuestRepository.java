package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.Guest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    @Query(
            value = """
            SELECT g.id, g.full_name, g.email, g.phone,
                   COUNT(b.id),
                   COALESCE(SUM(CASE WHEN b.checked_out_at IS NOT NULL THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN b.status IN ('CONFIRMED', 'CONFIRMED_PAY_LATER', 'CANCELLED', 'NO_SHOW')
                       THEN b.quoted_total ELSE 0 END), 0)
            FROM guest g
            LEFT JOIN booking b ON b.guest_id = g.id
            WHERE (:query IS NULL OR :query = ''
                OR LOWER(g.full_name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR g.phone LIKE CONCAT('%', :query, '%'))
            GROUP BY g.id, g.full_name, g.email, g.phone
            ORDER BY g.full_name ASC, g.id ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM guest g
            WHERE (:query IS NULL OR :query = ''
                OR LOWER(g.full_name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR g.phone LIKE CONCAT('%', :query, '%'))
            """,
            nativeQuery = true)
    Page<Object[]> searchGuestSummaries(@Param("query") String query, Pageable pageable);
}
