package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.RoomUnitStatus;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.DailyRateRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RefundPolicyRepository;
import RMC_Booking_Engine.rmc.repository.RoomConfigOptionRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeImageRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import RMC_Booking_Engine.rmc.repository.RoomUnitRepository;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import RMC_Booking_Engine.rmc.repository.SystemConfigRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffConfigServiceRoomUnitDeleteTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;
    @Mock
    private RatePlanRepository ratePlanRepository;
    @Mock
    private RefundPolicyRepository refundPolicyRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private RoomConfigOptionRepository roomConfigOptionRepository;
    @Mock
    private RoomUnitRepository roomUnitRepository;
    @Mock
    private RoomTypeImageRepository roomTypeImageRepository;
    @Mock
    private DailyRateRepository dailyRateRepository;
    @Mock
    private ConfigurationAuditLogRepository configurationAuditLogRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private InventoryHoldRepository inventoryHoldRepository;
    @Mock
    private StaffUserRepository staffUserRepository;
    @Mock
    private RoomDayStatusResolver roomDayStatusResolver;

    @InjectMocks
    private StaffConfigService service;

    private StaffPrincipal staff;

    @BeforeEach
    void setUp() {
        staff = new StaffPrincipal(1L, "admin@rmc.local", "Admin", StaffRole.ADMIN);
    }

    @Test
    void deleteRoomNumber_whenActiveBookingAssigned_rejects() {
        RoomUnit unit = new RoomUnit();
        unit.setId(50L);
        unit.setRoomNumber("101");

        when(roomUnitRepository.findById(50L)).thenReturn(Optional.of(unit));
        when(bookingRepository.existsByRoomUnitIdAndCheckedOutAtIsNull(50L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteRoomNumber(50L, staff))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("active booking");

        verify(bookingRepository, never()).clearRoomUnitAssignment(50L);
        verify(roomUnitRepository, never()).delete(unit);
    }

    @Test
    void deleteRoomNumber_whenHistoricalBookings_unlinksAndHardDeletes() {
        RoomType roomType = new RoomType();
        roomType.setId(5L);
        roomType.setTotalCapacity(3);

        RoomUnit unit = new RoomUnit();
        unit.setId(51L);
        unit.setRoomNumber("102");
        unit.setRoomType(roomType);
        unit.setStatus(RoomUnitStatus.AVAILABLE);

        when(roomUnitRepository.findById(51L)).thenReturn(Optional.of(unit));
        when(bookingRepository.existsByRoomUnitIdAndCheckedOutAtIsNull(51L)).thenReturn(false);
        when(bookingRepository.existsByRoomUnitId(51L)).thenReturn(true);

        service.deleteRoomNumber(51L, staff);

        verify(bookingRepository).clearRoomUnitAssignment(51L);
        assertThat(roomType.getTotalCapacity()).isEqualTo(2);
        verify(roomTypeRepository).save(roomType);
        verify(roomUnitRepository).delete(unit);
        verify(configurationAuditLogRepository).save(ArgumentMatchers.any());
    }
}
