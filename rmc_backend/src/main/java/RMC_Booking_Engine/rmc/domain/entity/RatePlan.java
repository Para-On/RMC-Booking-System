package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.CutoffUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rate_plan")
@Getter
@Setter
public class RatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_policy_id", nullable = false)
    private RefundPolicy refundPolicy;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults = 2;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren = 0;

    @Column(name = "square_meters")
    private BigDecimal squareMeters;

    @Column(columnDefinition = "json")
    private String amenities;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_category_id")
    private RoomConfigOption roomCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_view_id")
    private RoomConfigOption roomView;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_type_id")
    private RoomConfigOption bedType;

    @Column(name = "cancellation_policy")
    private String cancellationPolicy;

    @Column(name = "policy_enabled", nullable = false)
    private Boolean policyEnabled = true;

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

    @Column(name = "policy_description", columnDefinition = "TEXT")
    private String policyDescription;

    @Column(nullable = false)
    private Boolean refundable = false;

    /** @deprecated Prefer fullCutoffValue; retained for DB compatibility. */
    @Column(name = "refund_window_hours", nullable = false)
    private Integer refundWindowHours = 24;

    /** @deprecated Prefer partialRefundPercent; retained for DB compatibility. */
    @Column(name = "late_cancel_refund_percent", nullable = false)
    private Integer lateCancelRefundPercent = 50;

    /** @deprecated Prefer partialEnabled; retained for DB compatibility. */
    @Column(name = "allow_late_cancellation", nullable = false)
    private Boolean allowLateCancellation = true;

    @Column(name = "hold_ttl_minutes", nullable = false)
    private Integer holdTtlMinutes = 15;

    @Column(name = "pay_later_cutoff_hours")
    private Integer payLaterCutoffHours;

    @Column(nullable = false)
    private Boolean active = true;
}
