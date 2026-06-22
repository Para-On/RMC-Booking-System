package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record AvailabilityResponse(List<RoomAvailabilityDto> rooms) {
}
