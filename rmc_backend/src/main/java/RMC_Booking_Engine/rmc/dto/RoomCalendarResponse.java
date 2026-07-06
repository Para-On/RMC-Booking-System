package RMC_Booking_Engine.rmc.dto;

import java.util.List;
import java.util.Set;

public record RoomCalendarResponse(
        Long roomUnitId,
        String roomNumber,
        String floorLabel,
        Long roomTypeId,
        String roomTypeName,
        String fromDate,
        String toDate,
        Set<String> occupiedDates,
        List<RoomCalendarBlockDto> blocks) {
}
