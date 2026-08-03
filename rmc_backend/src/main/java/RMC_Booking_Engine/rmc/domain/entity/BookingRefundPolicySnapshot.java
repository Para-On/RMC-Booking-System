package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "booking_refund_policy_snapshot")
@Getter
@Setter
public class BookingRefundPolicySnapshot {

    @Id
    private Long bookingId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "refund_policy_id")
    private Long refundPolicyId;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "full_cutoff_value", nullable = false)
    private Integer fullCutoffValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "full_cutoff_unit", nullable = false, length = 10)
    private CutoffUnit fullCutoffUnit;

    @Column(name = "partial_enabled", nullable = false)
    private Boolean partialEnabled;

    @Column(name = "partial_refund_percent", nullable = false)
    private Integer partialRefundPercent;

    @Column(name = "nights_deduction_enabled", nullable = false)
    private Boolean nightsDeductionEnabled = false;

    @Column(name = "nights_deducted", nullable = false)
    private Integer nightsDeducted = 1;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "room_refundable", nullable = false)
    private Boolean roomRefundable = true;

    @Column(name = "snapshotted_at", nullable = false)
    private Instant snapshottedAt = Instant.now();
}
