package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingLedgerRepository extends JpaRepository<BookingLedger, Long> {

    List<BookingLedger> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    boolean existsByBookingIdAndEntryType(Long bookingId, LedgerEntryType entryType);

    @Query("""
            SELECT COALESCE(SUM(l.amount), 0) FROM BookingLedger l
            WHERE l.entryType = :entryType
            AND l.createdAt >= :start
            AND l.createdAt < :end
            """)
    BigDecimal sumAmountByEntryTypeBetween(
            @Param("entryType") LedgerEntryType entryType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT COALESCE(SUM(l.amount), 0) FROM BookingLedger l
            WHERE l.entryType IN :entryTypes
            AND l.createdAt >= :start
            AND l.createdAt < :end
            """)
    BigDecimal sumAmountByEntryTypesBetween(
            @Param("entryTypes") Collection<LedgerEntryType> entryTypes,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
