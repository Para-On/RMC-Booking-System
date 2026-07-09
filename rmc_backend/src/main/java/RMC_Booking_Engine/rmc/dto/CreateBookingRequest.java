package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateBookingRequest(
        @NotNull Long roomTypeId,
        @NotNull Long ratePlanId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotBlank String paymentMethod,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        boolean ageConfirmed,
        boolean dpaConsentAccepted,
        List<Long> serviceAddonIds,
        List<BookingItemAddonSelectionRequest> itemAddons,
        @jakarta.validation.constraints.Size(max = 500) String customExtrasRequest) {
}
