package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.BookingLedger;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.dto.ArrivalItemDto;
import RMC_Booking_Engine.rmc.dto.StaffDashboardResponse;
import RMC_Booking_Engine.rmc.dto.StaffDashboardSeriesPointDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffDashboardService {

    private static final Set<BookingStatus> ACTIVE_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER);

    private static final Set<BookingStatus> DASHBOARD_BOOKING_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER,
            BookingStatus.NO_SHOW);

    private static final Set<LedgerEntryType> MONEY_TYPES = EnumSet.of(
            LedgerEntryType.CREDIT,
            LedgerEntryType.REFUND,
            LedgerEntryType.MANUAL_REFUND);

    private final StaffRoomOccupancyService staffRoomOccupancyService;
    private final BookingRepository bookingRepository;
    private final BookingLedgerRepository bookingLedgerRepository;

    @Transactional(readOnly = true)
    public StaffDashboardResponse getDashboard(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : today;
        LocalDate to = toDate != null ? toDate : from;
        if (to.isBefore(from)) {
            throw new BusinessException("toDate must be on or after fromDate");
        }

        var occupancy = staffRoomOccupancyService.getDailyStatus(to, 0, Integer.MAX_VALUE, null, null, null);

        int sellable = occupancy.availableCount() + occupancy.reservedCount() + occupancy.occupiedCount();
        int occupancyPercent = sellable == 0
                ? 0
                : (int) Math.round((occupancy.occupiedCount() * 100.0) / sellable);

        long checkIns = bookingRepository.countCheckInsBetween(from, to, ACTIVE_STATUSES);
        long checkOuts = bookingRepository.countCheckOutsBetween(from, to, ACTIVE_STATUSES);

        ZoneId zone = ZoneId.systemDefault();
        var rangeStart = from.atStartOfDay(zone).toInstant();
        var rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant();
        BigDecimal grossPayments = scaleMoney(bookingLedgerRepository.sumAmountByEntryTypeBetween(
                LedgerEntryType.CREDIT, rangeStart, rangeEnd));
        BigDecimal refundsTotal = scaleMoney(bookingLedgerRepository.sumAmountByEntryTypesBetween(
                EnumSet.of(LedgerEntryType.REFUND, LedgerEntryType.MANUAL_REFUND), rangeStart, rangeEnd));
        BigDecimal revenueTotal = grossPayments.subtract(refundsTotal);

        int totalRooms = occupancy.availableCount()
                + occupancy.reservedCount()
                + occupancy.occupiedCount()
                + occupancy.outOfOrderCount();

        List<ArrivalItemDto> bookings = bookingRepository
                .findBookingsOverlappingDateRange(from, to, DASHBOARD_BOOKING_STATUSES)
                .stream()
                .map(this::toBookingItem)
                .toList();

        List<StaffDashboardSeriesPointDto> series = buildSeries(from, to, zone, sellable);

        return new StaffDashboardResponse(
                from.toString(),
                to.toString(),
                totalRooms,
                occupancy.availableCount(),
                occupancy.reservedCount(),
                occupancy.occupiedCount(),
                occupancy.outOfOrderCount(),
                occupancyPercent,
                (int) checkIns,
                (int) checkOuts,
                revenueTotal,
                grossPayments,
                refundsTotal,
                "PHP",
                bookings,
                series);
    }

    private List<StaffDashboardSeriesPointDto> buildSeries(
            LocalDate from, LocalDate to, ZoneId zone, int sellableBaseline) {
        List<Booking> activeOverlapping = bookingRepository.findBookingsOverlappingDateRange(
                from, to, ACTIVE_STATUSES);

        Map<LocalDate, Integer> checkInsByDay = new HashMap<>();
        Map<LocalDate, Integer> checkOutsByDay = new HashMap<>();
        for (Booking booking : activeOverlapping) {
            LocalDate checkIn = booking.getCheckInDate();
            if (!checkIn.isBefore(from) && !checkIn.isAfter(to)) {
                checkInsByDay.merge(checkIn, 1, Integer::sum);
            }
            LocalDate checkOut = booking.getCheckOutDate();
            if (booking.getCheckedInAt() != null
                    && !checkOut.isBefore(from)
                    && !checkOut.isAfter(to)) {
                checkOutsByDay.merge(checkOut, 1, Integer::sum);
            }
        }

        var rangeStart = from.atStartOfDay(zone).toInstant();
        var rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<BookingLedger> ledgerRows = bookingLedgerRepository.findByCreatedAtBetweenAndEntryTypeIn(
                rangeStart, rangeEnd, MONEY_TYPES);

        Map<LocalDate, BigDecimal> grossByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> refundsByDay = new HashMap<>();
        for (BookingLedger row : ledgerRows) {
            LocalDate day = row.getCreatedAt().atZone(zone).toLocalDate();
            if (row.getEntryType() == LedgerEntryType.CREDIT) {
                grossByDay.merge(day, row.getAmount(), BigDecimal::add);
            } else {
                refundsByDay.merge(day, row.getAmount(), BigDecimal::add);
            }
        }

        List<StaffDashboardSeriesPointDto> series = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            final LocalDate current = day;
            int occupied = (int) activeOverlapping.stream()
                    .filter(b -> !b.getCheckInDate().isAfter(current) && b.getCheckOutDate().isAfter(current))
                    .filter(b -> b.getCheckedInAt() != null || b.getRoomUnit() != null)
                    .count();
            // Fall back to all overlapping stays when none are checked in yet (reserved inventory).
            if (occupied == 0) {
                occupied = (int) activeOverlapping.stream()
                        .filter(b -> !b.getCheckInDate().isAfter(current) && b.getCheckOutDate().isAfter(current))
                        .count();
            }

            int sellable = Math.max(sellableBaseline, occupied);
            int dayOccupancyPercent = sellable == 0
                    ? 0
                    : (int) Math.round((occupied * 100.0) / sellable);

            BigDecimal gross = scaleMoney(grossByDay.getOrDefault(day, BigDecimal.ZERO));
            BigDecimal refunds = scaleMoney(refundsByDay.getOrDefault(day, BigDecimal.ZERO));
            BigDecimal revenue = gross.subtract(refunds);

            series.add(new StaffDashboardSeriesPointDto(
                    day.toString(),
                    checkInsByDay.getOrDefault(day, 0),
                    checkOutsByDay.getOrDefault(day, 0),
                    revenue,
                    gross,
                    refunds,
                    occupied,
                    dayOccupancyPercent));
        }
        return series;
    }

    private BigDecimal scaleMoney(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private ArrivalItemDto toBookingItem(Booking booking) {
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
}
