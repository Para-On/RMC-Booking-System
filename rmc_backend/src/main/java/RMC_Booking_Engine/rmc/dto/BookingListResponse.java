package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record BookingListResponse(
        List<BookingListItemDto> bookings,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
