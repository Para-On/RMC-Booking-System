package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.GuestItemAddonDto;
import RMC_Booking_Engine.rmc.dto.GuestServiceAddonDto;
import RMC_Booking_Engine.rmc.service.RoomExtrasService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest/extras")
@RequiredArgsConstructor
public class GuestRoomExtrasController {

    private final RoomExtrasService roomExtrasService;

    @GetMapping("/services")
    public List<GuestServiceAddonDto> listServices() {
        return roomExtrasService.listGuestServices();
    }

    @GetMapping("/items")
    public List<GuestItemAddonDto> listItems() {
        return roomExtrasService.listGuestItems();
    }
}
