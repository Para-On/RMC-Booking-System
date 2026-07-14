package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record BookingItemSelectionDto(
        String itemName, boolean selected, String guestNote, BigDecimal unitPrice) {
}
