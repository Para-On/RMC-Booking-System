package RMC_Booking_Engine.rmc.dto;

import java.util.List;

public record RoomCatalogResponse(List<GuestRoomCatalogItemDto> rooms) {
}
