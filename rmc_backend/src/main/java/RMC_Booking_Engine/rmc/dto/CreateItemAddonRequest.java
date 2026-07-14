package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateItemAddonRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 200) String subtitle,
        @Size(max = 5000) String details,
        String imageUrl,
        boolean free,
        BigDecimal price,
        boolean active) {
}
