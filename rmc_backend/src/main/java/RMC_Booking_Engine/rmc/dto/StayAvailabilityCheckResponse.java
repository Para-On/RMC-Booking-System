package RMC_Booking_Engine.rmc.dto;

import java.time.LocalDate;
import java.util.List;

public record StayAvailabilityCheckResponse(
        boolean available,
        String message,
        List<LocalDate> unavailableDates,
        RoomAvailabilityDto room) {
}
