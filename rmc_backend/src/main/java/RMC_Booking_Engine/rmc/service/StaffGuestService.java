package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.Guest;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.dto.StaffGuestBookingHistoryItemDto;
import RMC_Booking_Engine.rmc.dto.StaffGuestListItemDto;
import RMC_Booking_Engine.rmc.dto.StaffGuestListResponse;
import RMC_Booking_Engine.rmc.dto.StaffGuestProfileResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.GuestRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffGuestService {

    private static final String DEFAULT_CURRENCY = "PHP";
    private static final Set<BookingStatus> SPEND_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CONFIRMED_PAY_LATER,
            BookingStatus.CANCELLED,
            BookingStatus.NO_SHOW);

    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;

    public StaffGuestService(GuestRepository guestRepository, BookingRepository bookingRepository) {
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public StaffGuestListResponse listGuests(String query, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String trimmedQuery = normalizeQuery(query);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Object[]> result = guestRepository.searchGuestSummaries(trimmedQuery, pageable);
        List<StaffGuestListItemDto> guests =
                result.getContent().stream().map(this::toListItem).toList();
        return new StaffGuestListResponse(
                guests, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StaffGuestProfileResponse getGuestProfile(Long guestId) {
        Guest guest = guestRepository
                .findById(guestId)
                .orElseThrow(() -> new BusinessException("Guest not found"));
        List<Booking> bookings = bookingRepository.findHistoryByGuestId(guestId);
        return toProfileResponse(guest, bookings);
    }

    private StaffGuestListItemDto toListItem(Object[] row) {
        return new StaffGuestListItemDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
        ((Number) row[4]).longValue(),
        row[5] == null ? 0L : ((Number) row[5]).longValue(),
                toBigDecimal(row[6]),
                DEFAULT_CURRENCY);
    }

    private StaffGuestProfileResponse toProfileResponse(Guest guest, List<Booking> bookings) {
        long completedStays = bookings.stream()
                .filter(booking -> booking.getCheckedOutAt() != null)
                .count();
        BigDecimal totalSpent = bookings.stream()
                .filter(booking -> SPEND_STATUSES.contains(booking.getStatus()))
                .map(Booking::getQuotedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal completedStaysSpent = bookings.stream()
                .filter(booking -> booking.getCheckedOutAt() != null)
                .map(Booking::getQuotedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = bookings.stream()
                .map(Booking::getCurrency)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(DEFAULT_CURRENCY);
        List<StaffGuestBookingHistoryItemDto> history =
                bookings.stream().map(this::toHistoryItem).toList();
        return new StaffGuestProfileResponse(
                guest.getId(),
                guest.getFullName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getConsentTimestamp(),
                guest.getDpaConsentVersion(),
                guest.getAgeConfirmedAt(),
                bookings.size(),
                completedStays,
                totalSpent,
                completedStaysSpent,
                currency,
                history);
    }

    private StaffGuestBookingHistoryItemDto toHistoryItem(Booking booking) {
        return new StaffGuestBookingHistoryItemDto(
                booking.getId(),
                booking.getReference(),
                booking.getStatus().name(),
                booking.getRoomType().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getQuotedTotal(),
                booking.getCurrency(),
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                booking.getCreatedAt());
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }
}
