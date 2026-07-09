package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record ItemAddonDto(Long id, String name, BigDecimal price, boolean active, int sortOrder) {
}
