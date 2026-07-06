package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.dto.StaffSearchResultDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffSearchService {

    private static final int MAX_RESULTS = 10;

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<StaffSearchResultDto> search(String query) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.length() < 2) {
            throw new BusinessException("Enter at least 2 characters to search");
        }

        return bookingRepository.searchBookings(trimmed, PageRequest.of(0, MAX_RESULTS)).stream()
                .map(booking -> new StaffSearchResultDto(
                        booking.getId(),
                        booking.getReference(),
                        booking.getGuest().getFullName(),
                        booking.getGuest().getEmail(),
                        booking.getRoomType().getName(),
                        booking.getStatus().name(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate()))
                .toList();
    }
}
