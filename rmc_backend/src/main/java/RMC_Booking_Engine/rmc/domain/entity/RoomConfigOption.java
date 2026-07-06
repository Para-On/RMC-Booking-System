package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.RoomConfigOptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "room_config_option")
@Getter
@Setter
public class RoomConfigOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 32)
    private RoomConfigOptionType optionType;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean active = true;
}
