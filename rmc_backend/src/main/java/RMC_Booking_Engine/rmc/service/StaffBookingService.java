package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import RMC_Booking_Engine.rmc.dto.ArrivalItemDto;
import RMC_Booking_Engine.rmc.dto.ArrivalsResponse;
import RMC_Booking_Engine.rmc.dto.AuditEntryDto;
import RMC_Booking_Engine.rmc.dto.CheckOutResponse;
import RMC_Booking_Engine.rmc.dto.LedgerEntryDto;
import RMC_Booking_Engine.rmc.dto.RoomUnitOptionDto;
import RMC_Booking_Engine.rmc.dto.StaffBookingDetailResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RoomUnitRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffBookingService {

    private static final Set<BookingStatus> ARRIVAL_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER,
            BookingStatus.NO_SHOW);

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_OVERRIDES = Map.of(
            BookingStatus.NO_SHOW, Set.of(BookingStatus.CONFIRMED_PAY_LATER),
            BookingStatus.CONFIRMED_PAY_LATER, Set.of(BookingStatus.NO_SHOW, BookingStatus.CANCELLED),
            BookingStatus.CONFIRMED, Set.of(BookingStatus.CANCELLED));

    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final InventoryHoldRepository inventoryHoldRepository;

    @Transactional(readOnly = true)
    public ArrivalsResponse getArrivals(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        List<ArrivalItemDto> items = bookingRepository.findArrivals(target, ARRIVAL_STATUSES).stream()
                .map(this::toArrivalItem)
                .toList();
        return new ArrivalsResponse(target, items);
    }

    @Transactional(readOnly = true)
    public StaffBookingDetailResponse getBookingDetail(Long bookingId) {
        Booking booking = findBooking(bookingId);
        return toDetailResponse(booking);
    }

    @Transactional
    public StaffBookingDetailResponse checkIn(Long bookingId, Long roomUnitId, StaffPrincipal staff) {
        Booking booking = findBooking(bookingId);

        if (booking.getCheckedOutAt() != null) {
            throw new BusinessException("Guest has already checked out");
        }
        if (booking.getCheckedInAt() != null) {
            return toDetailResponse(booking);
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.CONFIRMED_PAY_LATER) {
            throw new BusinessException("Only confirmed bookings can be checked in");
        }

        if (roomUnitId != null) {
            RoomUnit unit = roomUnitRepository
                    .findByIdAndRoomTypeId(roomUnitId, booking.getRoomType().getId())
                    .orElseThrow(() -> new BusinessException("Room unit not found for this room type"));
            if (unit.getStatus() != RoomUnitStatus.AVAILABLE) {
                throw new BusinessException("Selected room is not available");
            }
            booking.setRoomUnit(unit);
        }

        booking.setCheckedInAt(Instant.now());
        bookingRepository.save(booking);
        writeAuditLog(booking, booking.getStatus().name(), booking.getStatus().name(),
                "STAFF_CHECK_IN", staff.id(), null);

        return toDetailResponse(booking);
    }

    @Transactional
    public CheckOutResponse checkOut(Long bookingId, StaffPrincipal staff) {
        Booking booking = findBooking(bookingId);

        if (booking.getCheckedInAt() == null) {
            throw new BusinessException("Guest must be checked in before check-out");
        }

        if (booking.getCheckedOutAt() != null) {
            return buildCheckOutResponse(booking);
        }

        booking.setCheckedOutAt(Instant.now());
        bookingRepository.save(booking);
        writeAuditLog(booking, booking.getStatus().name(), booking.getStatus().name(),
                "STAFF_CHECK_OUT", staff.id(), null);

        return buildCheckOutResponse(booking);
    }

    @Transactional
    public StaffBookingDetailResponse overrideStatus(
            Long bookingId,
            BookingStatus targetStatus,
            String reason,
            StaffPrincipal staff) {

        Booking booking = findBooking(bookingId);

        if (booking.getStatus() == targetStatus) {
            return toDetailResponse(booking);
        }

        Set<BookingStatus> allowed = ALLOWED_OVERRIDES.get(booking.getStatus());
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new BusinessException("Status change from "
                    + booking.getStatus() + " to " + targetStatus + " is not allowed");
        }

        BookingStatus previous = booking.getStatus();
        booking.setStatus(targetStatus);
        bookingRepository.save(booking);

        if (targetStatus == BookingStatus.NO_SHOW) {
            addNoShowLedgerEntry(booking);
        }
        if (targetStatus == BookingStatus.CANCELLED) {
            releaseHolds(booking);
        }

        writeAuditLog(booking, previous.name(), targetStatus.name(), "STAFF_OVERRIDE", staff.id(), reason);

        return toDetailResponse(booking);
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
    }

    private ArrivalItemDto toArrivalItem(Booking booking) {
        String roomNumber = booking.getRoomUnit() != null ? booking.getRoomUnit().getRoomNumber() : null;
        return new ArrivalItemDto(
                booking.getId(),
                booking.getReference(),
                booking.getGuest().getFullName(),
                booking.getGuest().getEmail(),
                booking.getRoomType().getName(),
                booking.getStatus().name(),
                booking.getPaymentMethod().name(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                roomNumber,
                booking.getCheckedInAt(),
                booking.getCheckedOutAt());
    }

    private StaffBookingDetailResponse toDetailResponse(Booking booking) {
        List<BookingLedger> ledgerEntries =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        List<AuditEntryDto> audit = bookingAuditLogRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId())
                .stream()
                .map(entry -> new AuditEntryDto(
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        entry.getTriggerSource(),
                        entry.getReason(),
                        entry.getCreatedAt()))
                .toList();

        List<RoomUnitOptionDto> rooms = roomUnitRepository
                .findByRoomTypeIdAndStatusOrderByRoomNumberAsc(
                        booking.getRoomType().getId(), RoomUnitStatus.AVAILABLE)
                .stream()
                .map(unit -> new RoomUnitOptionDto(
                        unit.getId(),
                        unit.getRoomNumber(),
                        unit.getFloorLabel(),
                        unit.getStatus().name()))
                .toList();

        BigDecimal balance = calculateBalance(ledgerEntries);

        return new StaffBookingDetailResponse(
                booking.getId(),
                booking.getReference(),
                booking.getStatus().name(),
                booking.getPaymentMethod().name(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getQuotedTotal(),
                booking.getCurrency(),
                booking.getGuest().getFullName(),
                booking.getGuest().getEmail(),
                booking.getGuest().getPhone(),
                booking.getRoomType().getName(),
                booking.getRoomUnit() != null ? booking.getRoomUnit().getRoomNumber() : null,
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                balance,
                ledgerEntries.stream()
                        .map(entry -> new LedgerEntryDto(
                                entry.getEntryType().name(),
                                entry.getAmount(),
                                entry.getCreatedAt()))
                        .toList(),
                audit,
                rooms);
    }

    private CheckOutResponse buildCheckOutResponse(Booking booking) {
        List<BookingLedger> ledgerEntries =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal debits = sumByType(ledgerEntries, LedgerEntryType.DEBIT);
        BigDecimal credits = sumByType(ledgerEntries, LedgerEntryType.CREDIT);
        BigDecimal balance = debits.subtract(credits);

        return new CheckOutResponse(
                booking.getId(),
                booking.getReference(),
                booking.getCheckedOutAt(),
                booking.getQuotedTotal(),
                credits,
                balance,
                booking.getCurrency(),
                ledgerEntries.stream()
                        .map(entry -> new LedgerEntryDto(
                                entry.getEntryType().name(),
                                entry.getAmount(),
                                entry.getCreatedAt()))
                        .toList());
    }

    private BigDecimal calculateBalance(List<BookingLedger> entries) {
        BigDecimal debits = sumByType(entries, LedgerEntryType.DEBIT);
        BigDecimal credits = sumByType(entries, LedgerEntryType.CREDIT);
        return debits.subtract(credits);
    }

    private BigDecimal sumByType(List<BookingLedger> entries, LedgerEntryType type) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(BookingLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private void releaseHolds(Booking booking) {
        inventoryHoldRepository.findByBookingIdAndStatus(booking.getId(), HoldStatus.ACTIVE)
                .forEach(hold -> hold.setStatus(HoldStatus.RELEASED));
    }

    private void writeAuditLog(
            Booking booking,
            String fromStatus,
            String toStatus,
            String trigger,
            Long staffUserId,
            String reason) {
        BookingAuditLog log = new BookingAuditLog();
        log.setBooking(booking);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setTriggerSource(trigger);
        log.setStaffUserId(staffUserId);
        log.setReason(reason);
        log.setCreatedAt(Instant.now());
        bookingAuditLogRepository.save(log);
    }
}
