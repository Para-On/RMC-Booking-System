package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoomConfigOptionRequest(
        @NotBlank @Size(max = 100) String label) {
}
