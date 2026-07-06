package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revokedAt = :now
            WHERE rt.staffUser.id = :staffUserId AND rt.revokedAt IS NULL
            """)
    int revokeAllActiveForUser(@Param("staffUserId") Long staffUserId, @Param("now") Instant now);
}
