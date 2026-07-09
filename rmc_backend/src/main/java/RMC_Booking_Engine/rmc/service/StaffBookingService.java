package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingAuditLog;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.HoldStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import RMC_Booking_Engine.rmc.dto.ArrivalItemDto;
import RMC_Booking_Engine.rmc.dto.ArrivalsResponse;
import RMC_Booking_Engine.rmc.dto.AuditEntryDto;
import RMC_Booking_Engine.rmc.dto.BookingListItemDto;
import RMC_Booking_Engine.rmc.dto.BookingListResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffBookingService {

    private static final Set<BookingStatus> ARRIVAL_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER,
            BookingStatus.NO_SHOW);

    private static final Set<BookingStatus> ACTIVE_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER);

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_OVERRIDES = Map.of(
            BookingStatus.CONFIRMED_PAY_LATER, Set.of(BookingStatus.NO_SHOW, BookingStatus.CANCELLED),
            BookingStatus.CONFIRMED, Set.of(BookingStatus.CANCELLED));

    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;
    private final BookingAuditLogRepository bookingAuditLogRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final InventoryHoldRepository inventoryHoldRepository;
    private final StaffRefundService staffRefundService;
    private final RefundPolicyService refundPolicyService;
    private final BookingRefundPolicySnapshotService bookingRefundPolicySnapshotService;
    private final MayaRefundService mayaRefundService;
    private final ConfigService configService;
    private final BookingHoldService bookingHoldService;
    private final RoomDayStatusResolver roomDayStatusResolver;

    @Transactional(readOnly = true)
    public ArrivalsResponse getArrivals(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        List<ArrivalItemDto> items = bookingRepository.findArrivals(target, ARRIVAL_STATUSES).stream()
                .map(this::toArrivalItem)
                .toList();
        return new ArrivalsResponse(target, items);
    }

    @Transactional(readOnly = true)
    public BookingListResponse listBookings(BookingStatus status, String query, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String trimmedQuery = query != null ? query.trim() : null;
        if (trimmedQuery != null && trimmedQuery.isEmpty()) {
            trimmedQuery = null;
        }
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Booking> result = bookingRepository.findStaffBookingList(status, trimmedQuery, pageable);
        List<BookingListItemDto> items =
                result.getContent().stream().map(this::toListItem).toList();
        return new BookingListResponse(
                items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
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
            assertRoomAvailableForStay(unit, booking);
            booking.setRoomUnit(unit);
            roomUnitRepository.save(unit);
        } else if (booking.getRoomUnit() != null) {
            roomUnitRepository.save(booking.getRoomUnit());
        }

        booking.setCheckedInAt(Instant.now());
        bookingRepository.save(booking);
        writeAuditLog(booking, booking.getStatus().name(), booking.getStatus().name(),
                "STAFF_CHECK_IN", staff.id(), null);

        return toDetailResponse(booking);
    }

    @Transactional
    public StaffBookingDetailResponse transferRoom(
            Long bookingId, Long roomUnitId, String reason, StaffPrincipal staff) {
        if (roomUnitId == null) {
            throw new BusinessException("Room is required");
        }

        Booking booking = findBooking(bookingId);

        if (booking.getCheckedInAt() == null) {
            throw new BusinessException("Guest must be checked in before transferring rooms");
        }
        if (booking.getCheckedOutAt() != null) {
            throw new BusinessException("Guest has already checked out");
        }

        RoomUnit current = booking.getRoomUnit();
        if (current != null && current.getId().equals(roomUnitId)) {
            return toDetailResponse(booking);
        }

        RoomUnit unit = roomUnitRepository
                .findByIdAndRoomTypeId(roomUnitId, booking.getRoomType().getId())
                .orElseThrow(() -> new BusinessException("Room unit not found for this room type"));
        assertRoomAvailableForStay(unit, booking);

        String previousRoom = current != null ? current.getRoomNumber() : "unassigned";
        booking.setRoomUnit(unit);
        bookingRepository.save(booking);

        String trimmedReason = reason != null ? reason.trim() : "";
        String auditReason = trimmedReason.isEmpty()
                ? "Room " + previousRoom + " → " + unit.getRoomNumber()
                : trimmedReason + " (" + previousRoom + " → " + unit.getRoomNumber() + ")";
        writeAuditLog(
                booking,
                booking.getStatus().name(),
                booking.getStatus().name(),
                "STAFF_ROOM_TRANSFER",
                staff.id(),
                auditReason);

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

        Set<BookingStatus> allowed = allowedOverrideTargets(booking);
        if (!allowed.contains(targetStatus)) {
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
        if (previous == BookingStatus.NO_SHOW
                && (targetStatus == BookingStatus.CONFIRMED
                        || targetStatus == BookingStatus.CONFIRMED_PAY_LATER)) {
            bookingHoldService.restoreHolds(booking);
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

    private BookingListItemDto toListItem(Booking booking) {
        String refundStatus = booking.getRefundStatus() != null ? booking.getRefundStatus().name() : null;
        return new BookingListItemDto(
                booking.getId(),
                booking.getGuest().getId(),
                booking.getReference(),
                booking.getGuest().getFullName(),
                booking.getGuest().getEmail(),
                booking.getRoomType().getName(),
                booking.getStatus().name(),
                booking.getPaymentMethod().name(),
                refundStatus,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getQuotedTotal(),
                booking.getCurrency(),
                booking.getCreatedAt());
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

        List<RoomUnitOptionDto> rooms = findAvailableRoomsForStay(booking);

        BigDecimal balance = calculateBalance(ledgerEntries);
        BigDecimal refundableAmount = staffRefundService.refundableAmount(ledgerEntries);
        BigDecimal pendingRefundAmount = mayaRefundService.remainingRefundDue(booking, ledgerEntries);
        boolean refundEligible = isRefundEligible(booking, refundableAmount, pendingRefundAmount);
        boolean manualRefundEnabled = configService.isManualRefundEnabled();
        boolean manualRefundAllowed = isManualRefundAllowed(booking, manualRefundEnabled, pendingRefundAmount);
        var mayaTiming = staffRefundService.assessMayaTiming(booking, ledgerEntries, pendingRefundAmount);
        boolean mayaRefundBlocked = refundEligible && !mayaTiming.canProcessNow();
        BigDecimal totalRefunded = mayaRefundService.sumPolicyRefunds(ledgerEntries);
        BigDecimal mayaRefunded = sumByType(ledgerEntries, LedgerEntryType.REFUND);
        BigDecimal manualRefunded = mayaRefundService.sumManualRefunds(ledgerEntries);

        var policySnapshot = bookingRefundPolicySnapshotService.resolveSnapshot(booking);
        String policyDescription = policySnapshot.description();
        var preview = refundPolicyService.previewCancellation(booking, refundableAmount);
        String refundPreview = preview.cancellationAllowedNow()
                ? buildStaffRefundPreview(preview)
                : preview.blockReason();

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
                booking.getRoomUnit() != null ? booking.getRoomUnit().getId() : null,
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                balance,
                refundableAmount,
                refundEligible,
                booking.getCancellationTier() != null ? booking.getCancellationTier().name() : null,
                booking.getRefundStatus() != null ? booking.getRefundStatus().name() : null,
                booking.getRefundEligibleAmount(),
                pendingRefundAmount,
                policyDescription,
                refundPreview,
                booking.getRefundPercentApplied(),
                booking.getDeductionAmount(),
                booking.getCancellationReason(),
                preview.fullCutoffAt(),
                preview.checkInAt(),
                manualRefundEnabled,
                manualRefundAllowed,
                mayaRefundBlocked,
                mayaTiming.blockedReason(),
                mayaTiming.availableAt(),
                totalRefunded,
                mayaRefunded,
                manualRefunded,
                ledgerEntries.stream()
                        .map(entry -> new LedgerEntryDto(
                                entry.getEntryType().name(),
                                entry.getAmount(),
                                entry.getCreatedAt(),
                                entry.getMayaReference()))
                        .toList(),
                audit,
                rooms,
                allowedOverrideTargets(booking).stream().map(BookingStatus::name).toList());
    }

    private List<RoomUnitOptionDto> findAvailableRoomsForStay(Booking booking) {
        List<RoomUnit> units = roomUnitRepository.findByRoomTypeIdOrderByRoomNumberAsc(
                booking.getRoomType().getId());
        List<Long> unitIds = units.stream().map(RoomUnit::getId).toList();
        Set<Long> blockedUnitIds = findBlockedUnitIds(
                unitIds, booking.getCheckInDate(), booking.getCheckOutDate(), booking.getId());

        List<RoomUnitOptionDto> available = units.stream()
                .filter(unit -> unit.getStatus() != RoomUnitStatus.OUT_OF_ORDER)
                .filter(unit -> !blockedUnitIds.contains(unit.getId()))
                .map(unit -> toRoomUnitOption(unit, booking))
                .toList();

        if (booking.getRoomUnit() != null) {
            Long assignedId = booking.getRoomUnit().getId();
            boolean alreadyListed = available.stream().anyMatch(option -> option.id().equals(assignedId));
            if (!alreadyListed) {
                RoomUnit assigned = booking.getRoomUnit();
                if (assigned.getStatus() != RoomUnitStatus.OUT_OF_ORDER
                        && !blockedUnitIds.contains(assignedId)) {
                    available = new java.util.ArrayList<>(available);
                    available.add(toRoomUnitOption(assigned, booking));
                    available.sort(java.util.Comparator.comparing(RoomUnitOptionDto::roomNumber));
                }
            }
        }

        return available;
    }

    private void assertRoomAvailableForStay(RoomUnit unit, Booking booking) {
        if (unit.getStatus() == RoomUnitStatus.OUT_OF_ORDER) {
            throw new BusinessException("Selected room is out of order");
        }
        Set<Long> blocked = findBlockedUnitIds(
                List.of(unit.getId()),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getId());
        if (!blocked.isEmpty()) {
            throw new BusinessException("Selected room is not available for this stay");
        }
    }

    private Set<Long> findBlockedUnitIds(
            List<Long> unitIds, LocalDate checkIn, LocalDate checkOut, Long excludeBookingId) {
        if (unitIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(bookingRepository.findBlockedUnitIdsForStay(
                unitIds, checkIn, checkOut, excludeBookingId, ACTIVE_STATUSES));
    }

    private RoomUnitOptionDto toRoomUnitOption(RoomUnit unit, Booking forBooking) {
        Booking activeBooking = bookingRepository
                .findActiveBookingsForUnitOnDate(
                        unit.getId(), forBooking.getCheckInDate(), ACTIVE_STATUSES)
                .stream()
                .filter(booking -> !booking.getId().equals(forBooking.getId()))
                .findFirst()
                .orElse(null);
        var resolved = roomDayStatusResolver.resolve(unit, activeBooking);
        return new RoomUnitOptionDto(
                unit.getId(),
                unit.getRoomNumber(),
                unit.getFloorLabel(),
                resolved.dayStatus(),
                resolved.statusLabel());
    }

    private Set<BookingStatus> allowedOverrideTargets(Booking booking) {
        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            if (booking.getPaymentMethod() == PaymentMethod.ONLINE_MAYA) {
                return Set.of(BookingStatus.CONFIRMED);
            }
            return Set.of(BookingStatus.CONFIRMED_PAY_LATER);
        }
        Set<BookingStatus> allowed = ALLOWED_OVERRIDES.get(booking.getStatus());
        return allowed != null ? allowed : Set.of();
    }

    private CheckOutResponse buildCheckOutResponse(Booking booking) {
        List<BookingLedger> ledgerEntries =
                bookingLedgerRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal debits = sumByType(ledgerEntries, LedgerEntryType.DEBIT);
        BigDecimal credits = sumByType(ledgerEntries, LedgerEntryType.CREDIT);
        BigDecimal refunds = sumByType(ledgerEntries, LedgerEntryType.REFUND)
                .add(sumByType(ledgerEntries, LedgerEntryType.MANUAL_REFUND));
        BigDecimal balance = debits.subtract(credits).subtract(refunds);
        BigDecimal netPaid = credits.subtract(refunds);

        return new CheckOutResponse(
                booking.getId(),
                booking.getReference(),
                booking.getCheckedOutAt(),
                booking.getQuotedTotal(),
                netPaid,
                balance,
                booking.getCurrency(),
                ledgerEntries.stream()
                        .map(entry -> new LedgerEntryDto(
                                entry.getEntryType().name(),
                                entry.getAmount(),
                                entry.getCreatedAt(),
                                entry.getMayaReference()))
                        .toList());
    }

    private BigDecimal calculateBalance(List<BookingLedger> entries) {
        BigDecimal debits = sumByType(entries, LedgerEntryType.DEBIT);
        BigDecimal credits = sumByType(entries, LedgerEntryType.CREDIT);
        BigDecimal refunds = sumByType(entries, LedgerEntryType.REFUND)
                .add(sumByType(entries, LedgerEntryType.MANUAL_REFUND));
        return debits.subtract(credits).subtract(refunds);
    }

    private boolean isRefundEligible(Booking booking, BigDecimal refundableAmount, BigDecimal pendingRefundAmount) {
        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA
                || booking.getCheckedOutAt() != null
                || refundableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            RefundStatus refundStatus = booking.getRefundStatus();
            return (refundStatus == RefundStatus.PENDING || refundStatus == RefundStatus.FAILED)
                    && pendingRefundAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        return booking.getStatus() == BookingStatus.CONFIRMED
                && refundPolicyService.isWithinRefundWindow(booking);
    }

    private boolean isManualRefundAllowed(
            Booking booking, boolean manualRefundEnabled, BigDecimal pendingRefundAmount) {
        if (!manualRefundEnabled || pendingRefundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (booking.getPaymentMethod() != PaymentMethod.ONLINE_MAYA || booking.getCheckedOutAt() != null) {
            return false;
        }
        return booking.getStatus() == BookingStatus.CANCELLED
                && (booking.getRefundStatus() == RefundStatus.PENDING
                        || booking.getRefundStatus() == RefundStatus.FAILED);
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

    private String buildStaffRefundPreview(RMC_Booking_Engine.rmc.dto.RefundPolicyPreviewDto preview) {
        if (preview.policySummary() != null && !preview.policySummary().isBlank()) {
            return preview.policySummary();
        }
        if (preview.refundAmountIfCancelledNow() == null) {
            return null;
        }
        return preview.tierIfCancelledNow() + " — "
                + preview.refundPercentIfCancelledNow() + "% refund ("
                + preview.refundAmountIfCancelledNow() + ")";
    }
}
