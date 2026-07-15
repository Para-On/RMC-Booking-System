package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;

public record StaffDashboardSeriesPointDto(
        String date,
        int checkIns,
        int checkOuts,
        BigDecimal revenue,
        BigDecimal grossPayments,
        BigDecimal refunds,
        int occupiedRooms,
        int occupancyPercent) {
}
