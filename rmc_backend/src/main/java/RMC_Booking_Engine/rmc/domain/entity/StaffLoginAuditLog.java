package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.StaffLoginEvent;
import RMC_Booking_Engine.rmc.domain.enums.StaffLoginStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_login_audit_log")
@Getter
@Setter
public class StaffLoginAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_user_id")
    private Long staffUserId;

    @Column(name = "email_attempt")
    private String emailAttempt;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private StaffLoginEvent eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffLoginStatus status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
