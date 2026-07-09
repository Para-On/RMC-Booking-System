package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record StaffNotificationsResponse(List<StaffNotificationDto> notifications, long unreadCount) {}
