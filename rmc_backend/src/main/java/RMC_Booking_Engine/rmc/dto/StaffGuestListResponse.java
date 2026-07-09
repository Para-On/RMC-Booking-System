package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record StaffGuestListResponse(
        List<StaffGuestListItemDto> guests,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
