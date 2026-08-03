package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import RMC_Booking_Engine.rmc.dto.RoomCalendarBlockDto;
import RMC_Booking_Engine.rmc.dto.RoomCalendarResponse;
import RMC_Booking_Engine.rmc.dto.RoomDailyStatusDto;
import RMC_Booking_Engine.rmc.dto.RoomDailyStatusPageResponse;
import RMC_Booking_Engine.rmc.dto.RoomUnitBookingDto;
import RMC_Booking_Engine.rmc.dto.RoomUnitBookingPageResponse;
import RMC_Booking_Engine.rmc.dto.UnassignedReservationDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.RoomUnitRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffRoomOccupancyService {

    private static final Set<BookingStatus> ACTIVE_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER);

    private final RoomUnitRepository roomUnitRepository;
    private final BookingRepository bookingRepository;
    private final RoomDayStatusResolver roomDayStatusResolver;

    @Transactional(readOnly = true)
    public RoomDailyStatusPageResponse getDailyStatus(
            LocalDate date,
            int page,
            int size,
            String dayStatusFilter,
            Long roomTypeId,
            String search) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        List<RoomUnit> units = roomUnitRepository.findAllByOrderByRoomNumberAsc();
        List<Long> unitIds = units.stream().map(RoomUnit::getId).toList();
        Map<Long, Booking> bookingByUnit = unitIds.isEmpty()
                ? Map.of()
                : bookingRepository
                        .findActiveBookingsForUnitsOnDate(unitIds, targetDate, ACTIVE_STATUSES)
                        .stream()
                        .collect(Collectors.toMap(b -> b.getRoomUnit().getId(), b -> b, (a, b) -> a));

        List<RoomDailyStatusDto> rows = new ArrayList<>();
        for (RoomUnit unit : units) {
            RoomDailyStatusDto row = toDailyStatus(unit, bookingByUnit.get(unit.getId()), targetDate);
            if (!matchesFilters(row, dayStatusFilter, roomTypeId, search)) {
                continue;
            }
            rows.add(row);
        }

        int totalElements = rows.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        List<RoomDailyStatusDto> pageContent = rows.subList(fromIndex, toIndex);

        int available = 0;
        int reserved = 0;
        int occupied = 0;
        int outOfOrder = 0;
        for (RoomDailyStatusDto row : rows) {
            switch (row.dayStatus()) {
                case "AVAILABLE" -> available++;
                case "RESERVED" -> reserved++;
                case "OCCUPIED" -> occupied++;
                case "OUT_OF_ORDER" -> outOfOrder++;
                default -> {
                }
            }
        }

        List<UnassignedReservationDto> unassigned = bookingRepository
                .countUnassignedReservationsByRoomTypeOnDate(targetDate, ACTIVE_STATUSES)
                .stream()
                .map(row -> new UnassignedReservationDto(
                        (Long) row[0],
                        (String) row[1],
                        ((Long) row[2]).intValue()))
                .toList();

        return new RoomDailyStatusPageResponse(
                targetDate.toString(),
                pageContent,
                safePage,
                safeSize,
                totalElements,
                totalPages,
                available,
                reserved,
                occupied,
                outOfOrder,
                unassigned);
    }

    @Transactional(readOnly = true)
    public RoomCalendarResponse getRoomCalendar(Long roomUnitId, LocalDate from, LocalDate to) {
        RoomUnit unit = roomUnitRepository.findById(roomUnitId)
                .orElseThrow(() -> new BusinessException("Room unit not found"));

        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : YearMonth.from(fromDate).atEndOfMonth().plusDays(1);

        if (!toDate.isAfter(fromDate)) {
            throw new BusinessException("toDate must be after fromDate");
        }

        List<Booking> bookings = bookingRepository.findActiveBookingsForUnitBetweenDates(
                roomUnitId, fromDate, toDate, ACTIVE_STATUSES);

        Set<String> occupiedDates = new HashSet<>();
        List<RoomCalendarBlockDto> blocks = new ArrayList<>();

        for (Booking booking : bookings) {
            String blockType = booking.getCheckedInAt() != null ? "OCCUPIED" : "RESERVED";
            blocks.add(new RoomCalendarBlockDto(
                    booking.getId(),
                    booking.getReference(),
                    booking.getGuest().getFullName(),
                    booking.getCheckInDate().toString(),
                    booking.getCheckOutDate().toString(),
                    blockType));

            for (LocalDate cursor = booking.getCheckInDate();
                    cursor.isBefore(booking.getCheckOutDate()) && cursor.isBefore(toDate);
                    cursor = cursor.plusDays(1)) {
                if (!cursor.isBefore(fromDate)) {
                    occupiedDates.add(cursor.toString());
                }
            }
        }

        return new RoomCalendarResponse(
                unit.getId(),
                unit.getRoomNumber(),
                unit.getFloorLabel(),
                unit.getRoomType() != null ? unit.getRoomType().getId() : null,
                unit.getRoomType() != null ? unit.getRoomType().getName() : null,
                fromDate.toString(),
                toDate.toString(),
                occupiedDates,
                blocks);
    }

    @Transactional(readOnly = true)
    public RoomUnitBookingPageResponse getRoomUnitBookings(Long roomUnitId, int page, int size) {
        RoomUnit unit = roomUnitRepository.findById(roomUnitId)
                .orElseThrow(() -> new BusinessException("Room unit not found"));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Booking> result = bookingRepository.findByRoomUnitIdOrderByCheckInDateDesc(
                roomUnitId, PageRequest.of(safePage, safeSize));
        List<RoomUnitBookingDto> content = result.getContent().stream()
                .map(b -> new RoomUnitBookingDto(
                        b.getId(),
                        b.getReference(),
                        b.getGuest().getFullName(),
                        b.getStatus().name(),
                        b.getCheckInDate().toString(),
                        b.getCheckOutDate().toString(),
                        b.getPaymentMethod() != null ? b.getPaymentMethod().name() : null))
                .toList();
        return new RoomUnitBookingPageResponse(
                unit.getId(),
                unit.getRoomNumber(),
                content,
                safePage,
                safeSize,
                (int) result.getTotalElements(),
                result.getTotalPages());
    }

    private RoomDailyStatusDto toDailyStatus(RoomUnit unit, Booking booking, LocalDate viewDate) {
        var resolved = roomDayStatusResolver.resolve(unit, booking);
        String dayStatus = resolved.dayStatus();
        String statusLabel = resolved.statusLabel();
        Long bookingId = null;
        String reference = null;
        String guestName = null;
        RoomDailyStatusDto.LocalDateRange stay = null;
        String checkoutAlert = null;
        boolean canCheckOut = false;

        if (booking != null) {
            bookingId = booking.getId();
            reference = booking.getReference();
            guestName = booking.getGuest().getFullName();
            stay = new RoomDailyStatusDto.LocalDateRange(
                    booking.getCheckInDate().toString(),
                    booking.getCheckOutDate().toString());
            canCheckOut = booking.getCheckedInAt() != null && booking.getCheckedOutAt() == null;
            if (canCheckOut) {
                LocalDate checkout = booking.getCheckOutDate();
                if (checkout.equals(viewDate)) {
                    checkoutAlert = "TODAY";
                } else if (checkout.equals(viewDate.plusDays(1))) {
                    checkoutAlert = "TOMORROW";
                }
            }
        }

        return new RoomDailyStatusDto(
                unit.getId(),
                unit.getRoomNumber(),
                unit.getFloorLabel(),
                unit.getRoomType() != null ? unit.getRoomType().getId() : null,
                unit.getRoomType() != null ? unit.getRoomType().getName() : null,
                unit.getStatus().name(),
                dayStatus,
                statusLabel,
                bookingId,
                reference,
                guestName,
                stay,
                checkoutAlert,
                canCheckOut);
    }

    private boolean matchesFilters(
            RoomDailyStatusDto row,
            String dayStatusFilter,
            Long roomTypeId,
            String search) {
        if (dayStatusFilter != null && !dayStatusFilter.isBlank()
                && !dayStatusFilter.equalsIgnoreCase(row.dayStatus())) {
            return false;
        }
        if (roomTypeId != null && !roomTypeId.equals(row.roomTypeId())) {
            return false;
        }
        if (search != null && !search.isBlank()) {
            String needle = search.trim().toLowerCase();
            boolean matchesNumber = row.roomNumber() != null
                    && row.roomNumber().toLowerCase().contains(needle);
            boolean matchesGuest = row.guestName() != null
                    && row.guestName().toLowerCase().contains(needle);
            boolean matchesReference = row.bookingReference() != null
                    && row.bookingReference().toLowerCase().contains(needle);
            if (!matchesNumber && !matchesGuest && !matchesReference) {
                return false;
            }
        }
        return true;
    }
}
