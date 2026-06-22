package RMC_Booking_Engine.rmc.dto;

public record RatePlanConfigDto(
        Long id,
        Long roomTypeId,
        String roomTypeName,
        String name,
        String cancellationPolicy,
        Integer refundWindowHours,
        Integer holdTtlMinutes,
        Integer payLaterCutoffHours,
        boolean active) {
}
