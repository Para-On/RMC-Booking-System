package RMC_Booking_Engine.rmc.dto;

public record BookingStatusResponse(
        String reference,
        String status,
        String paymentMethod) {
}
