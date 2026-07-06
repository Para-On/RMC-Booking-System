package RMC_Booking_Engine.rmc.dto;

public record RoomDailyStatusDto(
        Long roomUnitId,
        String roomNumber,
        String floorLabel,
        Long roomTypeId,
        String roomTypeName,
        String unitStatus,
        String dayStatus,
        String statusLabel,
        Long bookingId,
        String bookingReference,
        String guestName,
        LocalDateRange stay,
        String checkoutAlert,
        boolean canCheckOut) {

    public record LocalDateRange(String checkIn, String checkOut) {
    }
}
