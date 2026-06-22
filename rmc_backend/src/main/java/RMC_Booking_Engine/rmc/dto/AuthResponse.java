package RMC_Booking_Engine.rmc.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        int expiresInSeconds,
        String role,
        String fullName,
        String email) {
}
