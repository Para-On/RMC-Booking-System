package RMC_Booking_Engine.rmc.dto;

public record MfaSetupResponse(
        String secret,
        String otpAuthUrl) {
}
