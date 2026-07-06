package RMC_Booking_Engine.rmc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_nav_module")
@Getter
@Setter
public class StaffNavModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_key", nullable = false, unique = true)
    private String moduleKey;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String icon;

    @Column(name = "allowed_roles", columnDefinition = "JSON")
    private String allowedRoles;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private StaffNavModule parent;

    @OneToMany(mappedBy = "parent")
    private List<StaffNavModule> children = new ArrayList<>();
}
