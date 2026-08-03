package RMC_Booking_Engine.rmc.domain.entity;

import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.CancellationTier;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.domain.enums.RefundStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "booking")
@Getter
@Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "quoted_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal quotedTotal;

    @Column(nullable = false, length = 3)
    private String currency = "PHP";

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_unit_id")
    private RoomUnit roomUnit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "maya_checkout_id", length = 100)
    private String mayaCheckoutId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_tier", length = 20)
    private CancellationTier cancellationTier;

    @Column(name = "refund_eligible_amount", precision = 12, scale = 2)
    private BigDecimal refundEligibleAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 20)
    private RefundStatus refundStatus;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "refund_percent_applied")
    private Integer refundPercentApplied;

    @Column(name = "deduction_amount", precision = 12, scale = 2)
    private BigDecimal deductionAmount;

    @Column(name = "amount_paid_at_cancel", precision = 12, scale = 2)
    private BigDecimal amountPaidAtCancel;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private BookingRefundPolicySnapshot refundPolicySnapshot;

    @Column(name = "custom_extras_request", length = 500)
    private String customExtrasRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_id")
    private Promo promo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    @Column(name = "promo_name", length = 120)
    private String promoName;

    @Column(name = "promo_discount_amount", precision = 12, scale = 2)
    private BigDecimal promoDiscountAmount;

    @Column(name = "room_total_before_promo", precision = 12, scale = 2)
    private BigDecimal roomTotalBeforePromo;
}
