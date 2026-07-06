package RMC_Booking_Engine.rmc.dto;

public record CreateRoomTypeResponse(
        RoomTypeDetailDto roomType,
        RatePlanConfigDto ratePlan,
        int dailyRatesSeeded,
        int unitsCreated) {
}
