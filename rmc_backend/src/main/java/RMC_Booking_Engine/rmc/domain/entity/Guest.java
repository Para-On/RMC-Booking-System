package RMC_Booking_Engine.rmc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "guest")
@Getter
@Setter
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "consent_timestamp", nullable = false)
    private Instant consentTimestamp;

    @Column(name = "dpa_consent_version", nullable = false)
    private String dpaConsentVersion;

    @Column(name = "age_confirmed_at", nullable = false)
    private Instant ageConfirmedAt;
}
