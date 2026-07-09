package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "refund_policy")
@Getter
@Setter
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name = "Default";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "full_cutoff_value", nullable = false)
    private Integer fullCutoffValue = 48;

    @Enumerated(EnumType.STRING)
    @Column(name = "full_cutoff_unit", nullable = false, length = 10)
    private CutoffUnit fullCutoffUnit = CutoffUnit.HOURS;

    @Column(name = "partial_enabled", nullable = false)
    private Boolean partialEnabled = true;

    @Column(name = "partial_refund_percent", nullable = false)
    private Integer partialRefundPercent = 50;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime = LocalTime.of(14, 0);

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Manila";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
