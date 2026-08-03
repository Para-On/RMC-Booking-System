package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.Guest;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.BookingStatus;
import RMC_Booking_Engine.rmc.domain.enums.PaymentMethod;
import RMC_Booking_Engine.rmc.dto.RoomUnitBookingPageResponse;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.RoomUnitRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class StaffRoomOccupancyServiceBookingsTest {

    @Mock
    private RoomUnitRepository roomUnitRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RoomDayStatusResolver roomDayStatusResolver;

    private StaffRoomOccupancyService service;

    @BeforeEach
    void setUp() {
        service = new StaffRoomOccupancyService(roomUnitRepository, bookingRepository, roomDayStatusResolver);
    }

    @Test
    void getRoomUnitBookings_returnsPagedHistoryForUnit() {
        RoomUnit unit = new RoomUnit();
        unit.setId(5L);
        unit.setRoomNumber("501");

        Guest guest = new Guest();
        guest.setFullName("Jane Guest");
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setReference("RMC-1");
        booking.setGuest(guest);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.of(2026, 7, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 7, 3));
        booking.setPaymentMethod(PaymentMethod.ONLINE_MAYA);

        when(roomUnitRepository.findById(5L)).thenReturn(Optional.of(unit));
        when(bookingRepository.findByRoomUnitIdOrderByCheckInDateDesc(eq(5L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 20), 1));

        RoomUnitBookingPageResponse response = service.getRoomUnitBookings(5L, 0, 20);

        assertThat(response.roomUnitId()).isEqualTo(5L);
        assertThat(response.roomNumber()).isEqualTo("501");
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).reference()).isEqualTo("RMC-1");
        assertThat(response.content().get(0).guestName()).isEqualTo("Jane Guest");
        assertThat(response.content().get(0).status()).isEqualTo("CONFIRMED");
    }
}
