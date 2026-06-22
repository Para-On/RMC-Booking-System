package RMC_Booking_Engine.rmc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @Column(nullable = false)
    private String name;

    @Column(name = "cancellation_policy")
    private String cancellationPolicy;

    @Column(name = "refund_window_hours", nullable = false)
    private Integer refundWindowHours = 24;

    @Column(name = "hold_ttl_minutes", nullable = false)
    private Integer holdTtlMinutes = 15;

    @Column(name = "pay_later_cutoff_hours")
    private Integer payLaterCutoffHours;

    @Column(nullable = false)
    private Boolean active = true;
}
