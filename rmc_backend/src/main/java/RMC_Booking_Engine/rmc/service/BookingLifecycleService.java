package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingLifecycleService {

    private static final Set<BookingStatus> NO_SHOW_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER);

    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingHoldService bookingHoldService;

    @Transactional
    public int expirePendingPayments() {
        Instant now = Instant.now();
        int count = 0;
        for (Booking booking : bookingRepository.findExpiredByStatus(BookingStatus.PENDING_PAYMENT, now)) {
            transition(booking, BookingStatus.FAILED, "SCHEDULER_HOLD_EXPIRY", "Payment hold expired");
            count++;
        }
        if (count > 0) {
            log.info("Expired {} pending-payment booking(s)", count);
        }
        return count;
    }

    @Transactional
    public int applyPayLaterCutoffs() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Booking booking : bookingRepository.findUncheckedInByStatus(BookingStatus.CONFIRMED_PAY_LATER)) {
            if (!booking.getCheckInDate().isBefore(today)) {
                continue;
            }
            transition(booking, BookingStatus.CANCELLED, "SCHEDULER_PAY_LATER_CUTOFF", "Pay-later booking past check-in date");
            count++;
        }
        if (count > 0) {
            log.info("Cancelled {} pay-later booking(s) past check-in", count);
        }
        return count;
    }

    @Transactional
    public int markNoShows() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Booking booking : bookingRepository.findUncheckedInBefore(NO_SHOW_STATUSES, today)) {
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            transition(booking, BookingStatus.NO_SHOW, "SCHEDULER_NO_SHOW", "Guest did not check in");
            addNoShowLedgerEntry(booking);
            count++;
        }
        if (count > 0) {
            log.info("Marked {} booking(s) as no-show", count);
        }
        return count;
    }

    private void transition(Booking booking, BookingStatus targetStatus, String trigger, String reason) {
        if (booking.getStatus() == targetStatus) {
            return;
        }
        BookingStatus previous = booking.getStatus();
        booking.setStatus(targetStatus);
        bookingRepository.save(booking);
        bookingHoldService.releaseActiveHolds(booking);
        bookingHoldService.writeAuditLog(booking, previous.name(), targetStatus.name(), trigger, null, reason);
    }

    private void addNoShowLedgerEntry(Booking booking) {
        String key = "noshow-" + booking.getReference();
        if (bookingLedgerRepository.existsByIdempotencyKey(key)) {
            return;
        }
        BookingLedger entry = new BookingLedger();
        entry.setBooking(booking);
        entry.setEntryType(LedgerEntryType.NO_SHOW_RECORD);
        entry.setAmount(BigDecimal.ZERO);
        entry.setIdempotencyKey(key);
        entry.setCreatedAt(Instant.now());
        bookingLedgerRepository.save(entry);
    }
}
