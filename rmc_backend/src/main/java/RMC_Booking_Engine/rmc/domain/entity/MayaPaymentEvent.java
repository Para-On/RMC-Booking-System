package RMC_Booking_Engine.rmc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "maya_payment_event")
@Getter
@Setter
public class MayaPaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "booking_reference", length = 64)
    private String bookingReference;

    @Column(name = "checkout_id", length = 100)
    private String checkoutId;

    @Column(name = "checkout_status", length = 40)
    private String checkoutStatus;

    @Column(name = "payment_status", length = 40)
    private String paymentStatus;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();
}
