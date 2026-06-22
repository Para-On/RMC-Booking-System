package RMC_Booking_Engine.rmc.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MayaCheckoutStatusTest {

    @Test
    void resolvedAmount_parsesStringTotalAmount() {
        var status = new MayaCheckoutStatus(
                null,
                "checkout-id",
                "COMPLETED",
                "PAYMENT_SUCCESS",
                null,
                new MayaCheckoutStatus.TotalAmount("3080.00", "PHP"),
                "PHP",
                "RMC-20260627-8226");

        assertTrue(status.isPaymentSuccessful());
        assertEquals(new BigDecimal("3080.00"), status.resolvedAmount());
        assertEquals("checkout-id", status.resolvedId());
    }

    @Test
    void isPaymentSuccessful_whenPaymentStatusIsPaymentSuccess() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                null,
                null,
                "PHP",
                "RMC-20260620-6848");

        assertTrue(status.isPaymentSuccessful());
    }

    @Test
    void isPaymentSuccessful_whenOnlyCheckoutStatusIsCompleted() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "COMPLETED",
                null,
                new BigDecimal("15400.00"),
                null,
                "PHP",
                "RMC-20260620-6848");

        assertTrue(status.isPaymentSuccessful());
    }

    @Test
    void isPaymentSuccessful_whenPaymentFailed() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "COMPLETED",
                "PAYMENT_FAILED",
                null,
                null,
                "PHP",
                "RMC-20260620-6848");

        assertFalse(status.isPaymentSuccessful());
    }

    @Test
    void isPaymentSuccessful_whenStillPending() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "PENDING",
                null,
                null,
                null,
                "PHP",
                "RMC-20260620-6848");

        assertFalse(status.isPaymentSuccessful());
        assertFalse(status.isPaymentFailed());
        assertFalse(status.isCheckoutCancelled());
    }

    @Test
    void isPaymentFailed_whenPaymentStatusFailed() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "COMPLETED",
                "PAYMENT_FAILED",
                null,
                null,
                "PHP",
                "RMC-20260622-5937");

        assertFalse(status.isPaymentSuccessful());
        assertTrue(status.isPaymentFailed());
        assertEquals("PAYMENT_FAILED", status.resolvedFailureReason());
    }

    @Test
    void isPaymentFailed_whenPaymentExpired() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "EXPIRED",
                "PAYMENT_EXPIRED",
                null,
                null,
                "PHP",
                "RMC-TEST");

        assertTrue(status.isPaymentFailed());
    }

    @Test
    void isCheckoutCancelled_whenStatusCancelled() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "CANCELLED",
                null,
                null,
                null,
                "PHP",
                "RMC-TEST");

        assertTrue(status.isCheckoutCancelled());
        assertFalse(status.isPaymentFailed());
    }

    @Test
    void resolvedAmount_returnsNullOnInvalidString() {
        var status = new MayaCheckoutStatus(
                "checkout-id",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                null,
                new MayaCheckoutStatus.TotalAmount("not-a-number", "PHP"),
                "PHP",
                "RMC-TEST");

        assertNull(status.resolvedAmount());
    }
}
