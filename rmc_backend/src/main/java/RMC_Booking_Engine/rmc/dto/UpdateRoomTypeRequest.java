package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRoomTypeRequest(
        @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Min(1) Integer maxAdults,
        @Min(0) Integer maxChildren,
        @Min(1) Integer totalCapacity,
        @Min(0) Integer overbookingBuffer,
        @Min(0) Integer minAdvanceBookingHours,
        @Min(1) Integer maxAdvanceBookingDays,
        Boolean active) {
}
