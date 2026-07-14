package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        @Size(max = 500) String customExtrasRequest,
        @Size(max = 20) List<@Valid AdditionalGuestRequest> additionalGuests) {}
