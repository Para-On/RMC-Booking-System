package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomNumberRequest(
        @NotBlank @Size(max = 20) String roomNumber,
        @Size(max = 20) String floorLabel) {
}
