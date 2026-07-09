package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Size;

public record BookingItemAddonSelectionRequest(
        Long itemId,
        boolean selected,
        @Size(max = 500) String guestNote) {
}
