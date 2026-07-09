package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateItemAddonRequest(
        @NotBlank @Size(max = 120) String name,
        BigDecimal price,
        boolean active) {
}
