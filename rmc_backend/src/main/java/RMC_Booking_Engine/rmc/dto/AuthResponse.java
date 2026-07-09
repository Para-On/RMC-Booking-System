package RMC_Booking_Engine.rmc.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Integer expiresInSeconds,
        String role,
        String fullName,
        String email,
        String profileImageUrl,
        String themePreference,
        Boolean mfaRequired,
        String mfaToken) {

    public static AuthResponse withTokens(
            String accessToken,
            String refreshToken,
            int expiresInSeconds,
            String role,
            String fullName,
            String email,
            String profileImageUrl,
            String themePreference) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                expiresInSeconds,
                role,
                fullName,
                email,
                profileImageUrl,
                themePreference,
                false,
                null);
    }

    public static AuthResponse mfaChallenge(String mfaToken) {
        return new AuthResponse(null, null, null, null, null, null, null, null, true, mfaToken);
    }
}
