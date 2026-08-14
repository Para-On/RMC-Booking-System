package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.RoomConfigOption;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.RoomConfigOptionType;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.dto.CreateRoomConfigOptionRequest;
import RMC_Booking_Engine.rmc.dto.RoomConfigOptionDto;
import RMC_Booking_Engine.rmc.dto.RoomConfigOptionsResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.BookingRepository;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.DailyRateRepository;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanImageRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RefundPolicyRepository;
import RMC_Booking_Engine.rmc.repository.RoomConfigOptionRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeImageRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import RMC_Booking_Engine.rmc.repository.RoomUnitRepository;
import RMC_Booking_Engine.rmc.repository.StaffUserRepository;
import RMC_Booking_Engine.rmc.repository.SystemConfigRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.util.JsonStringListConverter;
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
class StaffConfigServiceAmenityConfigTest {

    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private RatePlanRepository ratePlanRepository;
    @Mock private RefundPolicyRepository refundPolicyRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private RoomConfigOptionRepository roomConfigOptionRepository;
    @Mock private RoomUnitRepository roomUnitRepository;
    @Mock private RoomTypeImageRepository roomTypeImageRepository;
    @Mock private RatePlanImageRepository ratePlanImageRepository;
    @Mock private DailyRateRepository dailyRateRepository;
    @Mock private ConfigurationAuditLogRepository configurationAuditLogRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private InventoryHoldRepository inventoryHoldRepository;
    @Mock private StaffUserRepository staffUserRepository;
    @Mock private RoomDayStatusResolver roomDayStatusResolver;

    @InjectMocks
    private StaffConfigService service;

    private StaffPrincipal staff;

    @BeforeEach
    void setUp() {
        staff = new StaffPrincipal(1L, "mgr@test.com", "Manager", StaffRole.MANAGER);
    }

    @Test
    void getRoomConfigOptions_includesAmenities() {
        RoomConfigOption wifi = amenityOption(9L, "Wi-Fi", 1);
        when(roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(
                        RoomConfigOptionType.ROOM_CATEGORY))
                .thenReturn(List.of());
        when(roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(
                        RoomConfigOptionType.ROOM_VIEW))
                .thenReturn(List.of());
        when(roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(
                        RoomConfigOptionType.BED_TYPE))
                .thenReturn(List.of());
        when(roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(
                        RoomConfigOptionType.ROOM_STATUS))
                .thenReturn(List.of());
        when(roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(
                        RoomConfigOptionType.AMENITY))
                .thenReturn(List.of(wifi));

        RoomConfigOptionsResponse response = service.getRoomConfigOptions();

        assertThat(response.amenities()).extracting(RoomConfigOptionDto::label).containsExactly("Wi-Fi");
    }

    @Test
    void createAmenityOption_persistsAmenityType() {
        when(roomConfigOptionRepository.existsByOptionTypeAndLabelIgnoreCase(
                        RoomConfigOptionType.AMENITY, "Mini bar"))
                .thenReturn(false);
        when(roomConfigOptionRepository.findByOptionTypeAndActiveTrueOrderBySortOrderAscLabelAsc(
                        RoomConfigOptionType.AMENITY))
                .thenReturn(List.of());
        when(roomConfigOptionRepository.save(any(RoomConfigOption.class))).thenAnswer(inv -> {
            RoomConfigOption saved = inv.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        RoomConfigOptionDto dto = service.createRoomConfigOption(
                new CreateRoomConfigOptionRequest(RoomConfigOptionType.AMENITY, "Mini bar"), staff);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.optionType()).isEqualTo("AMENITY");
        assertThat(dto.label()).isEqualTo("Mini bar");

        ArgumentCaptor<RoomConfigOption> captor = ArgumentCaptor.forClass(RoomConfigOption.class);
        verify(roomConfigOptionRepository).save(captor.capture());
        assertThat(captor.getValue().getOptionType()).isEqualTo(RoomConfigOptionType.AMENITY);
    }

    @Test
    void deleteAmenityOption_blockedWhenRoomTypeUsesLabel() {
        RoomConfigOption wifi = amenityOption(9L, "Wi-Fi", 1);
        RoomType roomType = new RoomType();
        roomType.setId(3L);
        roomType.setAmenities(JsonStringListConverter.toJson(List.of("Wi-Fi", "TV")));

        when(roomConfigOptionRepository.findById(9L)).thenReturn(Optional.of(wifi));
        when(roomTypeRepository.findAll()).thenReturn(List.of(roomType));

        assertThatThrownBy(() -> service.deleteRoomConfigOption(9L, staff))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Wi-Fi")
                .hasMessageContaining("room type");
    }

    @Test
    void deleteAmenityOption_allowedWhenUnused() {
        RoomConfigOption wifi = amenityOption(9L, "Wi-Fi", 1);
        RoomType roomType = new RoomType();
        roomType.setId(3L);
        roomType.setAmenities(JsonStringListConverter.toJson(List.of("TV")));

        when(roomConfigOptionRepository.findById(9L)).thenReturn(Optional.of(wifi));
        when(roomTypeRepository.findAll()).thenReturn(List.of(roomType));
        when(roomConfigOptionRepository.save(any(RoomConfigOption.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteRoomConfigOption(9L, staff);

        assertThat(wifi.getActive()).isFalse();
        verify(roomConfigOptionRepository).save(wifi);
    }

    private static RoomConfigOption amenityOption(Long id, String label, int sortOrder) {
        RoomConfigOption option = new RoomConfigOption();
        option.setId(id);
        option.setOptionType(RoomConfigOptionType.AMENITY);
        option.setLabel(label);
        option.setSortOrder(sortOrder);
        option.setActive(true);
        return option;
    }
}
