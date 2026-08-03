package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePromoCodeRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotBlank String promoType,
        @NotBlank @Size(max = 64) String offerCode,
        @Size(max = 64) String organizationCode,
        @NotBlank String discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @Min(1) int maxUses,
        boolean active,
        List<Long> ratePlanIds) {
}
