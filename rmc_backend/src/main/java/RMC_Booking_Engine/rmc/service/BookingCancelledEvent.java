package RMC_Booking_Engine.rmc.service;

public record BookingCancelledEvent(Long bookingId, boolean refundPending) {}
