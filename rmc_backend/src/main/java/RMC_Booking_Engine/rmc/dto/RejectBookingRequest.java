package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Size;

public record RejectBookingRequest(@Size(max = 500) String reason) {}
