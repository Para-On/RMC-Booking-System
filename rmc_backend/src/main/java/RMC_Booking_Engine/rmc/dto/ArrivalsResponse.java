package RMC_Booking_Engine.rmc.dto;

import java.time.LocalDate;
import java.util.List;

public record ArrivalsResponse(LocalDate date, List<ArrivalItemDto> arrivals) {
}
