package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import RMC_Booking_Engine.rmc.domain.enums.RoomConfigOptionType;

public record CreateRoomConfigOptionRequest(
        @NotNull RoomConfigOptionType optionType,
        @NotBlank @Size(max = 100) String label) {
}
