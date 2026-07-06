package RMC_Booking_Engine.rmc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MayaRefundResponse(
        String id,
        String status,
        String reason) {
}
