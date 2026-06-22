package RMC_Booking_Engine.rmc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MayaCheckoutStatus(
        String id,
        @JsonProperty("checkoutId") String checkoutId,
        String status,
        @JsonProperty("paymentStatus") String paymentStatus,
        BigDecimal amount,
        @JsonProperty("totalAmount") TotalAmount totalAmount,
        String currency,
        String requestReferenceNumber) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TotalAmount(String value, String currency) {
    }

    public String resolvedId() {
        return id != null && !id.isBlank() ? id : checkoutId;
    }

    /** Checkout session state (e.g. COMPLETED). Not the same as payment outcome. */
    public String resolvedStatus() {
        return status != null ? status : paymentStatus;
    }

    /** True when Maya reports a successful payment (poll or webhook). */
    public boolean isPaymentSuccessful() {
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            return "PAYMENT_SUCCESS".equalsIgnoreCase(paymentStatus);
        }
        if (status == null || status.isBlank()) {
            return false;
        }
        return "PAYMENT_SUCCESS".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status);
    }

    /** True when Maya reports a terminal failed/expired payment. */
    public boolean isPaymentFailed() {
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            return matchesAny(paymentStatus, "PAYMENT_FAILED", "PAYMENT_EXPIRED", "PAYMENT_CANCELLED");
        }
        if (status == null || status.isBlank()) {
            return false;
        }
        return matchesAny(status, "PAYMENT_FAILED", "PAYMENT_EXPIRED", "EXPIRED", "FAILED");
    }

    /** True when the guest cancelled checkout without paying. */
    public boolean isCheckoutCancelled() {
        return status != null && matchesAny(status, "CANCELLED", "DROPOUT");
    }

    public String resolvedFailureReason() {
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            return paymentStatus;
        }
        return status;
    }

    public BigDecimal resolvedAmount() {
        if (amount != null) {
            return amount;
        }
        if (totalAmount != null && totalAmount.value() != null && !totalAmount.value().isBlank()) {
            try {
                return new BigDecimal(totalAmount.value().trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static boolean matchesAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
