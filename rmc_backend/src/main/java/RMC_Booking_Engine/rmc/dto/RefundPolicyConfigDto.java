package RMC_Booking_Engine.rmc.dto;

public record RefundPolicyConfigDto(
        Long id,
        String name,
        boolean enabled,
        int fullCutoffValue,
        String fullCutoffUnit,
        boolean partialEnabled,
        int partialRefundPercent,
        int deductionPercent,
        boolean nightsDeductionEnabled,
        int nightsDeducted,
        String checkInTime,
        String timezone,
        String description,
        boolean refundable,
        boolean active,
        boolean manualRefundEnabled) {

    public static RefundPolicyConfigDto empty() {
        return new RefundPolicyConfigDto(
                null,
                "Default",
                false,
                48,
                "HOURS",
                false,
                0,
                100,
                false,
                1,
                "14:00",
                "Asia/Manila",
                null,
                true,
                true,
                false);
    }
}
