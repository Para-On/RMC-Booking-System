package RMC_Booking_Engine.rmc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBrandingRequest(
        @NotBlank @Size(max = 120) String fontFamily,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String secondaryColor,
        @Size(max = 500) String footerText,
        @Size(max = 120) String footerContactEmail,
        @Size(max = 40) String footerContactPhone,
        @Size(max = 200) String footerCopyright) {}
