package RMC_Booking_Engine.rmc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "room_type")
@Getter
@Setter
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren = 0;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "overbooking_buffer", nullable = false)
    private Integer overbookingBuffer = 0;

    @Column(name = "min_advance_booking_hours", nullable = false)
    private Integer minAdvanceBookingHours = 0;

    @Column(name = "max_advance_booking_days", nullable = false)
    private Integer maxAdvanceBookingDays = 365;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "square_meters", precision = 8, scale = 2)
    private BigDecimal squareMeters;

    @Column(columnDefinition = "JSON")
    private String amenities;

    @Column(nullable = false)
    private Boolean refundable = false;

    @Column(name = "free_cancellation", nullable = false)
    private Boolean freeCancellation = true;

    @ManyToOne
    @JoinColumn(name = "room_category_id")
    private RoomConfigOption roomCategory;

    @ManyToOne
    @JoinColumn(name = "room_view_id")
    private RoomConfigOption roomView;

    @ManyToOne
    @JoinColumn(name = "bed_type_id")
    private RoomConfigOption bedType;
}
