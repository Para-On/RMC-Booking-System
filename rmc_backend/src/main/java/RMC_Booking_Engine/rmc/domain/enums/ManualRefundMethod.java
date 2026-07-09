package RMC_Booking_Engine.rmc.domain.enums;

public enum ManualRefundMethod {
    GCASH,
    BANK_TRANSFER,
    CASH,
    OTHER;

    public static ManualRefundMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Manual refund method is required");
        }
        return ManualRefundMethod.valueOf(value.trim().toUpperCase());
    }
}
