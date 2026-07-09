package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record GuestServiceAddonDto(
        Long id,
        String title,
        String subtitle,
        String details,
        String imageUrl,
        boolean free,
        BigDecimal price) {
}
