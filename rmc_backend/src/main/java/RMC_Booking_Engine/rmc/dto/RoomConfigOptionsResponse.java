package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record RoomConfigOptionsResponse(
        List<RoomConfigOptionDto> categories,
        List<RoomConfigOptionDto> views,
        List<RoomConfigOptionDto> bedTypes,
        List<RoomConfigOptionDto> statuses,
        List<RoomConfigOptionDto> amenities) {
}
