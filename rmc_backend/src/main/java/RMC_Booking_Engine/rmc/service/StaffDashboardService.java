package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.LedgerEntryType;
import RMC_Booking_Engine.rmc.dto.ArrivalItemDto;
import RMC_Booking_Engine.rmc.dto.StaffDashboardResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingLedgerRepository;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
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

        var rangeStart = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        var rangeEnd = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal revenueTotal = bookingLedgerRepository.sumAmountByEntryTypeBetween(
                LedgerEntryType.CREDIT, rangeStart, rangeEnd);
        if (revenueTotal == null) {
            revenueTotal = BigDecimal.ZERO;
        } else {
            revenueTotal = revenueTotal.setScale(2, RoundingMode.HALF_UP);
        }

        int totalRooms = occupancy.availableCount()
                + occupancy.reservedCount()
                + occupancy.occupiedCount()
                + occupancy.outOfOrderCount();

        List<ArrivalItemDto> bookings = bookingRepository
                .findBookingsOverlappingDateRange(from, to, DASHBOARD_BOOKING_STATUSES)
                .stream()
                .map(this::toBookingItem)
                .toList();

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
                "PHP",
                bookings);
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
