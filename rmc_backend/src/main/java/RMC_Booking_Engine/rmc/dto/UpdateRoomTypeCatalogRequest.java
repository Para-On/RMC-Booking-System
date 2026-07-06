package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record UpdateRoomTypeCatalogRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @NotNull @Min(1) Integer maxAdults,
        @NotNull @Min(0) Integer maxChildren,
        @NotNull @Min(1) Integer totalCapacity,
        @Min(0) Integer overbookingBuffer,
        @Min(0) Integer minAdvanceBookingHours,
        @Min(1) Integer maxAdvanceBookingDays,
        Boolean active,
        @NotBlank @Size(max = 100) String ratePlanName,
        @Size(max = 255) String cancellationPolicy,
        @Min(0) Integer refundWindowHours,
        @Min(1) Integer holdTtlMinutes,
        @NotNull @DecimalMin("0.01") BigDecimal baseNightlyRate,
        @DecimalMin("0.01") BigDecimal squareMeters,
        List<@NotBlank @Size(max = 80) String> amenities,
        Boolean refundable,
        Boolean freeCancellation,
        @NotEmpty List<@NotNull Long> roomUnitIds,
        List<@NotBlank @Size(max = 512) String> imageUrls,
        @NotNull Long roomCategoryId,
        @NotNull Long roomViewId,
        @NotNull Long bedTypeId) {
}
