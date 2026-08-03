package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.PromoCodeType;
import RMC_Booking_Engine.rmc.domain.enums.PromoDiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promo_code")
@Getter
@Setter
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 20)
    private PromoCodeType promoType;

    @Column(name = "offer_code", nullable = false, length = 64)
    private String offerCode;

    @Column(name = "organization_code", length = 64)
    private String organizationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private PromoDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(
            name = "promo_code_rate_plan",
            joinColumns = @JoinColumn(name = "promo_code_id"),
            inverseJoinColumns = @JoinColumn(name = "rate_plan_id"))
    private Set<RatePlan> ratePlans = new HashSet<>();
}
