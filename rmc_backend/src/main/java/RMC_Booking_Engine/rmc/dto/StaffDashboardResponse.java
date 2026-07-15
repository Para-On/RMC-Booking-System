package RMC_Booking_Engine.rmc.dto;

import java.math.BigDecimal;
import java.util.List;

public record StaffDashboardResponse(
        String fromDate,
        String toDate,
        int totalRooms,
        int availableRooms,
        int reservedRooms,
        int occupiedRooms,
        int outOfOrderRooms,
        int occupancyPercent,
        int checkInsCount,
        int checkOutsCount,
        BigDecimal revenueTotal,
        BigDecimal grossPayments,
        BigDecimal refundsTotal,
        String currency,
        List<ArrivalItemDto> bookings,
        List<StaffDashboardSeriesPointDto> series) {
}
