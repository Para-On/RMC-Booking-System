package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.RoomConfigOption;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import org.springframework.stereotype.Component;

@Component
public class RoomDayStatusResolver {

    public ResolvedDayStatus resolve(RoomUnit unit, Booking booking) {
        if (unit.getStatus() == RoomUnitStatus.OUT_OF_ORDER) {
            String label = unit.getStatusOption() != null
                    ? unit.getStatusOption().getLabel()
                    : "Out of order";
            return new ResolvedDayStatus("OUT_OF_ORDER", label);
        }
        if (booking != null) {
            String dayStatus = booking.getCheckedInAt() != null ? "OCCUPIED" : "RESERVED";
            return new ResolvedDayStatus(dayStatus, humanize(dayStatus));
        }
        return new ResolvedDayStatus("AVAILABLE", "Available");
    }

    public boolean isOperationalAvailable(RoomConfigOption statusOption) {
        return statusOption != null && "Available".equalsIgnoreCase(statusOption.getLabel().trim());
    }

    private String humanize(String dayStatus) {
        return switch (dayStatus) {
            case "AVAILABLE" -> "Available";
            case "RESERVED" -> "Reserved";
            case "OCCUPIED" -> "Occupied";
            case "OUT_OF_ORDER" -> "Out of order";
            default -> dayStatus;
        };
    }

    public record ResolvedDayStatus(String dayStatus, String statusLabel) {
    }
}
