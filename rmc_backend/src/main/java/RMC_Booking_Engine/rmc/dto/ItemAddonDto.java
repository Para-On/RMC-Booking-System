package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record ItemAddonDto(
        Long id,
        String name,
        String subtitle,
        String details,
        String imageUrl,
        boolean free,
        BigDecimal price,
        boolean active,
        int sortOrder) {
}
