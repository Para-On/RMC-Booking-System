package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByReference(String reference);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            JOIN FETCH b.roomType rt
            JOIN FETCH b.ratePlan rp
            WHERE b.reference = :reference
            """)
    Optional<Booking> findByReferenceWithDetails(@Param("reference") String reference);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            JOIN FETCH b.roomType rt
            JOIN FETCH b.ratePlan rp
            LEFT JOIN FETCH b.roomUnit ru
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            JOIN FETCH b.roomType rt
            LEFT JOIN FETCH b.roomUnit ru
            WHERE b.checkInDate = :date
            AND b.status IN :statuses
            ORDER BY b.reference
            """)
    List<Booking> findArrivals(
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingStatus> statuses);

    boolean existsByReference(String reference);

    Optional<Booking> findByMayaCheckoutId(String mayaCheckoutId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM Booking b
            WHERE b.reference = :reference
            """)
    Optional<Booking> findByReferenceForUpdate(@Param("reference") String reference);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = :status
            AND b.expiresAt IS NOT NULL
            AND b.expiresAt <= :now
            """)
    List<Booking> findExpiredByStatus(
            @Param("status") BookingStatus status,
            @Param("now") Instant now);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = :status
            AND b.checkedInAt IS NULL
            """)
    List<Booking> findUncheckedInByStatus(@Param("status") BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status IN :statuses
            AND b.checkInDate < :beforeDate
            AND b.checkedInAt IS NULL
            """)
    List<Booking> findUncheckedInBefore(
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("beforeDate") LocalDate beforeDate);
}
