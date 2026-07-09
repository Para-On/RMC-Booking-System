package RMC_Booking_Engine.rmc.dto;

public record BrandingResponse(
        String logoUrl,
        String fontFamily,
        String primaryColor,
        String primaryForegroundColor,
        String secondaryColor,
        String secondaryForegroundColor,
        String accentColor,
        String accentForegroundColor,
        String borderColor,
        String ringColor,
        String destructiveColor,
        String headerBackgroundColor,
        String headerForegroundColor,
        String footerBackgroundColor,
        String footerForegroundColor,
        String footerText,
        String footerContactEmail,
        String footerContactPhone,
        String footerCopyright) {
}
