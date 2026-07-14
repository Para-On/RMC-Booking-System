package RMC_Booking_Engine.rmc.service;

public record BookingRejectedEvent(Long bookingId, String reason) {
}
