package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.JwtProperties;
import RMC_Booking_Engine.rmc.domain.entity.RefreshToken;
import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.dto.AuthResponse;
import RMC_Booking_Engine.rmc.dto.LoginRequest;
import RMC_Booking_Engine.rmc.dto.RefreshRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.RefreshTokenRepository;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import RMC_Booking_Engine.rmc.security.JwtService;
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

        return issueTokens(user);
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

    private AuthResponse issueTokens(StaffUser user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken entity = new RefreshToken();
        entity.setStaffUser(user);
        entity.setTokenHash(hashToken(refreshToken));
        entity.setExpiresAt(Instant.now().plusSeconds(jwtService.refreshTokenDays() * 86400L));
        entity.setCreatedAt(Instant.now());
        refreshTokenRepository.save(entity);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.accessTokenSeconds(),
                user.getRole().name(),
                user.getFullName(),
                user.getEmail());
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
