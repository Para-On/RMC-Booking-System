package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "room_unit")
@Getter
@Setter
public class RoomUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @Column(name = "floor_label")
    private String floorLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomUnitStatus status = RoomUnitStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_option_id")
    private RoomConfigOption statusOption;
}
