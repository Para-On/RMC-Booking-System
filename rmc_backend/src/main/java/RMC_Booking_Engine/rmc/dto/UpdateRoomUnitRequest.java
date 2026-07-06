package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRoomUnitRequest(@NotNull Long statusOptionId) {
}
