package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.entity.RoomUnit;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.dto.RoomTypeDeleteResult;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffConfigServiceRoomTypeDeleteTest {

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
    void deleteRoomType_whenReferencedByBooking_deactivatesInsteadOfDeleting() {
        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setName("Deluxe");
        roomType.setActive(true);
        RatePlan ratePlan = new RatePlan();
        ratePlan.setId(20L);
        ratePlan.setActive(true);

        when(roomTypeRepository.findById(10L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.existsByRoomTypeId(10L)).thenReturn(true);
        when(ratePlanRepository.findByRoomTypeId(10L)).thenReturn(List.of(ratePlan));

        RoomTypeDeleteResult result = service.deleteRoomType(10L, staff);

        assertThat(result.deactivated()).isTrue();
        assertThat(result.message()).contains("hidden from guests");
        assertThat(roomType.getActive()).isFalse();
        assertThat(ratePlan.getActive()).isFalse();
        verify(roomTypeRepository).save(roomType);
        verify(ratePlanRepository).save(ratePlan);
        verify(roomTypeRepository, never()).delete(any());
        verify(configurationAuditLogRepository).save(any());
    }

    @Test
    void deleteRoomType_whenUnused_hardDeletesOwnedChildren() {
        RoomType roomType = new RoomType();
        roomType.setId(11L);
        roomType.setName("Standard");
        roomType.setActive(true);
        RatePlan ratePlan = new RatePlan();
        ratePlan.setId(21L);
        RoomUnit unit = new RoomUnit();
        unit.setId(31L);
        unit.setRoomType(roomType);

        when(roomTypeRepository.findById(11L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.existsByRoomTypeId(11L)).thenReturn(false);
        when(inventoryHoldRepository.existsByRoomTypeId(11L)).thenReturn(false);
        when(roomUnitRepository.findByRoomTypeIdOrderByRoomNumberAsc(11L)).thenReturn(List.of(unit));
        when(ratePlanRepository.findByRoomTypeId(11L)).thenReturn(List.of(ratePlan));

        RoomTypeDeleteResult result = service.deleteRoomType(11L, staff);

        assertThat(result.deactivated()).isFalse();
        assertThat(result.message()).isEqualTo("Room type deleted");
        assertThat(unit.getRoomType()).isNull();
        verify(roomUnitRepository).save(unit);
        verify(dailyRateRepository).deleteByRatePlanId(21L);
        verify(ratePlanRepository).delete(ratePlan);
        verify(roomTypeImageRepository).deleteByRoomTypeId(11L);
        verify(roomTypeRepository).delete(roomType);
        ArgumentCaptor<RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog> audit =
                ArgumentCaptor.forClass(RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog.class);
        verify(configurationAuditLogRepository).save(audit.capture());
        assertThat(audit.getValue().getConfigKey()).isEqualTo("deleted");
    }
}
