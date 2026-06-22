package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Min;

public record UpdateRoomTypeRequest(
        @Min(0) Integer overbookingBuffer,
        @Min(0) Integer minAdvanceBookingHours,
        @Min(1) Integer maxAdvanceBookingDays,
        Boolean active) {
}
