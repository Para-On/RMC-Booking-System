package RMC_Booking_Engine.rmc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MayaCheckoutCreated(
        String checkoutId,
        String redirectUrl) {
}
