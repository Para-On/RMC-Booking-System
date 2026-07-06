package RMC_Booking_Engine.rmc.security;

import RMC_Booking_Engine.rmc.config.JwtProperties;
import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String MFA_PENDING_CLAIM = "mfa_pending";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.signingSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(StaffUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.accessTokenMinutes() * 60L);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String createMfaPendingToken(StaffUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(300);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(MFA_PENDING_CLAIM, true)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Long parseMfaPendingUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!Boolean.TRUE.equals(claims.get(MFA_PENDING_CLAIM, Boolean.class))) {
            throw new IllegalArgumentException("Invalid MFA token");
        }
        return Long.parseLong(claims.getSubject());
    }

    public StaffPrincipal parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (Boolean.TRUE.equals(claims.get(MFA_PENDING_CLAIM, Boolean.class))) {
            throw new IllegalArgumentException("MFA token cannot be used as access token");
        }

        Long userId = Long.parseLong(claims.getSubject());
        StaffRole role = StaffRole.valueOf(claims.get("role", String.class));
        String email = claims.get("email", String.class);
        String name = claims.get("name", String.class);
        return new StaffPrincipal(userId, email, name, role);
    }

    public int accessTokenSeconds() {
        return properties.accessTokenMinutes() * 60;
    }

    public int refreshTokenDays() {
        return properties.refreshTokenDays();
    }
}
