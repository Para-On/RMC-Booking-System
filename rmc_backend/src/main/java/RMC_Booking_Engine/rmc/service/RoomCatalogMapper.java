package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.RoomConfigOption;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.RoomCatalogCardDto;
import RMC_Booking_Engine.rmc.repository.RoomTypeImageRepository;
import RMC_Booking_Engine.rmc.util.JsonStringListConverter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCatalogMapper {

    private final RoomTypeImageRepository roomTypeImageRepository;

    public RoomCatalogCardDto toCard(RoomType roomType) {
        List<String> imageUrls = roomTypeImageRepository
                .findByRoomTypeIdOrderBySortOrderAscIdAsc(roomType.getId())
                .stream()
                .map(image -> image.getImageUrl())
                .toList();

        return new RoomCatalogCardDto(
                roomType.getName(),
                roomType.getDescription(),
                roomType.getMaxAdults(),
                roomType.getMaxChildren(),
                roomType.getSquareMeters(),
                imageUrls,
                JsonStringListConverter.fromJson(roomType.getAmenities()),
                label(roomType.getRoomCategory()),
                label(roomType.getRoomView()),
                label(roomType.getBedType()),
                Boolean.TRUE.equals(roomType.getRefundable()),
                roomType.getFreeCancellation() == null || roomType.getFreeCancellation());
    }

    private String label(RoomConfigOption option) {
        return option != null ? option.getLabel() : null;
    }
}
