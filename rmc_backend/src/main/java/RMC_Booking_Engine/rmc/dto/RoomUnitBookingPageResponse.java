package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record RoomUnitBookingPageResponse(
        Long roomUnitId,
        String roomNumber,
        List<RoomUnitBookingDto> content,
        int page,
        int size,
        int totalElements,
        int totalPages) {}
