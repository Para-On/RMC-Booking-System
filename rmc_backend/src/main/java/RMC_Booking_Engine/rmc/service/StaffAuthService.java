package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.JwtProperties;
import RMC_Booking_Engine.rmc.domain.entity.RefreshToken;
import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.dto.AuthResponse;
import RMC_Booking_Engine.rmc.dto.LoginRequest;
import RMC_Booking_Engine.rmc.dto.MfaConfirmRequest;
import RMC_Booking_Engine.rmc.dto.MfaSetupResponse;
import RMC_Booking_Engine.rmc.dto.MfaVerifyRequest;
import RMC_Booking_Engine.rmc.dto.RefreshRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.RefreshTokenRepository;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import RMC_Booking_Engine.rmc.security.JwtService;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffAuthService {

    private final StaffUserRepository staffUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final MfaService mfaService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        StaffUser user = staffUserRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BusinessException("Account is disabled");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessException("Account is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new BusinessException("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        staffUserRepository.save(user);

        if (user.isMfaEnabled()) {
            return AuthResponse.mfaChallenge(jwtService.createMfaPendingToken(user));
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse verifyMfa(MfaVerifyRequest request) {
        Long userId;
        try {
            userId = jwtService.parseMfaPendingUserId(request.mfaToken());
        } catch (Exception ex) {
            throw new BusinessException("MFA session expired. Please sign in again.");
        }

        StaffUser user = staffUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Account not found"));

        if (!user.isActive() || !user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new BusinessException("MFA is not enabled for this account");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.code())) {
            throw new BusinessException("Invalid authentication code");
        }

        user.setLastLoginAt(Instant.now());
        staffUserRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public MfaSetupResponse startMfaSetup(StaffPrincipal staff) {
        StaffUser user = staffUserRepository.findById(staff.id())
                .orElseThrow(() -> new BusinessException("Account not found"));

        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        staffUserRepository.save(user);

        String otpAuthUrl = mfaService.buildOtpAuthUrl(user.getEmail(), secret);
        return new MfaSetupResponse(secret, otpAuthUrl);
    }

    @Transactional
    public void confirmMfaSetup(StaffPrincipal staff, MfaConfirmRequest request) {
        StaffUser user = staffUserRepository.findById(staff.id())
                .orElseThrow(() -> new BusinessException("Account not found"));

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new BusinessException("Start MFA setup first");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.code())) {
            throw new BusinessException("Invalid authentication code");
        }

        user.setMfaEnabled(true);
        staffUserRepository.save(user);
    }

    @Transactional
    public void disableMfa(StaffPrincipal staff) {
        StaffUser user = staffUserRepository.findById(staff.id())
                .orElseThrow(() -> new BusinessException("Account not found"));
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        staffUserRepository.save(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token expired");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        StaffUser user = stored.getStaffUser();
        if (!user.isActive()) {
            throw new BusinessException("Account is disabled");
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request, StaffPrincipal staff) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String hash = hashToken(request.refreshToken());
            refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        }
        if (staff != null) {
            refreshTokenRepository.revokeAllActiveForUser(staff.id(), Instant.now());
        }
    }

    private AuthResponse issueTokens(StaffUser user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken entity = new RefreshToken();
        entity.setStaffUser(user);
        entity.setTokenHash(hashToken(refreshToken));
        entity.setExpiresAt(Instant.now().plusSeconds(jwtService.refreshTokenDays() * 86400L));
        entity.setCreatedAt(Instant.now());
        refreshTokenRepository.save(entity);

        String themePreference =
                user.getThemePreference() != null ? user.getThemePreference().name() : "LIGHT";
        return AuthResponse.withTokens(
                accessToken,
                refreshToken,
                jwtService.accessTokenSeconds(),
                user.getRole().name(),
                user.getFullName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                themePreference);
    }

    private void registerFailedAttempt(StaffUser user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= jwtProperties.lockoutThreshold()) {
            user.setLockedUntil(Instant.now().plusSeconds(jwtProperties.lockoutMinutes() * 60L));
            user.setFailedLoginAttempts(0);
        }
        staffUserRepository.save(user);
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
