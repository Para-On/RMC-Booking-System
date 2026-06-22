package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record ManagerConfigResponse(
        List<SystemConfigItemDto> systemConfig,
        List<RatePlanConfigDto> ratePlans,
        List<RoomTypeConfigDto> roomTypes) {
}
