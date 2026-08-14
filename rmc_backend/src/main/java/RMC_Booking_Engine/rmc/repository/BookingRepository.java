package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

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

    boolean existsByRoomUnitIdAndCheckedOutAtIsNull(Long roomUnitId);

    boolean existsByRoomUnitId(Long roomUnitId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.roomUnit = null WHERE b.roomUnit.id = :roomUnitId")
    int clearRoomUnitAssignment(@Param("roomUnitId") Long roomUnitId);

    boolean existsByRoomTypeId(Long roomTypeId);

    @Query(
            value = """
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            WHERE b.roomUnit.id = :roomUnitId
            ORDER BY b.checkInDate DESC, b.id DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b
            WHERE b.roomUnit.id = :roomUnitId
            """)
    Page<Booking> findByRoomUnitIdOrderByCheckInDateDesc(
            @Param("roomUnitId") Long roomUnitId, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            WHERE b.roomUnit.id = :roomUnitId
            AND b.status IN :statuses
            AND b.checkInDate <= :date
            AND b.checkOutDate > :date
            AND b.checkedOutAt IS NULL
            """)
    List<Booking> findActiveBookingsForUnitOnDate(
            @Param("roomUnitId") Long roomUnitId,
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            WHERE b.roomUnit.id = :roomUnitId
            AND b.status IN :statuses
            AND b.checkInDate < :toDate
            AND b.checkOutDate > :fromDate
            AND b.checkedOutAt IS NULL
            ORDER BY b.checkInDate ASC
            """)
    List<Booking> findActiveBookingsForUnitBetweenDates(
            @Param("roomUnitId") Long roomUnitId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            JOIN FETCH b.roomType rt
            WHERE b.roomUnit.id IN :roomUnitIds
            AND b.status IN :statuses
            AND b.checkInDate <= :date
            AND b.checkOutDate > :date
            AND b.checkedOutAt IS NULL
            """)
    List<Booking> findActiveBookingsForUnitsOnDate(
            @Param("roomUnitIds") Collection<Long> roomUnitIds,
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT b.roomType.id, b.roomType.name, COUNT(b)
            FROM Booking b
            WHERE b.roomUnit IS NULL
            AND b.status IN :statuses
            AND b.checkInDate <= :date
            AND b.checkOutDate > :date
            AND b.checkedOutAt IS NULL
            GROUP BY b.roomType.id, b.roomType.name
            ORDER BY b.roomType.name
            """)
    List<Object[]> countUnassignedReservationsByRoomTypeOnDate(
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT DISTINCT b.roomUnit.id FROM Booking b
            WHERE b.roomUnit.id IN :roomUnitIds
            AND b.id <> :excludeBookingId
            AND b.status IN :statuses
            AND b.checkInDate < :checkOut
            AND b.checkOutDate > :checkIn
            AND b.checkedOutAt IS NULL
            """)
    List<Long> findBlockedUnitIdsForStay(
            @Param("roomUnitIds") Collection<Long> roomUnitIds,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeBookingId") Long excludeBookingId,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.checkOutDate = :date
            AND b.checkedInAt IS NOT NULL
            AND b.checkedOutAt IS NULL
            AND b.status IN :statuses
            """)
    long countPendingCheckOutsOnDate(
            @Param("date") LocalDate date,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.checkInDate >= :fromDate
            AND b.checkInDate <= :toDate
            AND b.status IN :statuses
            """)
    long countCheckInsBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.checkOutDate >= :fromDate
            AND b.checkOutDate <= :toDate
            AND b.checkedInAt IS NOT NULL
            AND b.status IN :statuses
            """)
    long countCheckOutsBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            JOIN FETCH b.roomType rt
            LEFT JOIN FETCH b.roomUnit ru
            WHERE b.checkInDate <= :toDate
            AND b.checkOutDate > :fromDate
            AND b.status IN :statuses
            ORDER BY b.checkInDate ASC, b.reference ASC
            """)
    List<Booking> findBookingsOverlappingDateRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            JOIN FETCH b.roomType rt
            WHERE LOWER(b.reference) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(g.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY b.createdAt DESC
            """)
    List<Booking> searchBookings(@Param("query") String query, Pageable pageable);

    @Query(
            value = """
            SELECT b FROM Booking b
            JOIN FETCH b.guest g
            JOIN FETCH b.roomType rt
            LEFT JOIN FETCH b.roomUnit ru
            WHERE (:status IS NULL OR b.status = :status)
            AND (
                :query IS NULL OR :query = ''
                OR LOWER(b.reference) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            ORDER BY b.createdAt DESC, b.id DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b
            JOIN b.guest g
            WHERE (:status IS NULL OR b.status = :status)
            AND (
                :query IS NULL OR :query = ''
                OR LOWER(b.reference) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            """)
    Page<Booking> findStaffBookingList(
            @Param("status") BookingStatus status,
            @Param("query") String query,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.guest
            JOIN FETCH b.roomType
            WHERE b.guest.id = :guestId
               OR EXISTS (
                    SELECT 1 FROM BookingAdditionalGuest bag
                    WHERE bag.booking = b AND bag.guest.id = :guestId
               )
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    List<Booking> findHistoryByGuestId(@Param("guestId") Long guestId);

    @Query("""
            SELECT b.id FROM Booking b
            WHERE b.status = :status
            AND b.paymentMethod = :paymentMethod
            AND b.refundStatus = :refundStatus
            """)
    List<Long> findIdsByStatusAndPaymentMethodAndRefundStatus(
            @Param("status") BookingStatus status,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("refundStatus") RefundStatus refundStatus);
}
