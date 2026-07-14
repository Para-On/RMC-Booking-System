package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PromoDto(
        Long id,
        String name,
        String description,
        String discountType,
        BigDecimal discountValue,
        LocalDate startsOn,
        LocalDate endsOn,
        boolean active,
        boolean currentlyEffective,
        List<Long> roomTypeIds,
        List<String> roomTypeNames) {
}
