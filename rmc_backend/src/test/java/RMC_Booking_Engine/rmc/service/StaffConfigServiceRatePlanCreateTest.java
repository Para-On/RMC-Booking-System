package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.dto.CreateRatePlanRequest;
import RMC_Booking_Engine.rmc.dto.RatePlanConfigDto;
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
import java.math.BigDecimal;
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
class StaffConfigServiceRatePlanCreateTest {

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
    private RatePlanImageRepository ratePlanImageRepository;
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
    void createRatePlan_linksRefundPolicyAndSeedsDailyRates() {
        RoomType roomType = new RoomType();
        roomType.setId(5L);
        roomType.setName("Deluxe");
        roomType.setMaxAdults(2);
        roomType.setMaxChildren(1);

        RefundPolicy policy = new RefundPolicy();
        policy.setId(7L);
        policy.setName("Flexible");
        policy.setEnabled(true);
        policy.setFullCutoffValue(48);
        policy.setPartialRefundPercent(50);
        policy.setRefundable(true);
        policy.setNightsDeductionEnabled(false);
        policy.setNightsDeducted(1);

        when(roomTypeRepository.findById(5L)).thenReturn(Optional.of(roomType));
        when(refundPolicyRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(policy));
        when(ratePlanRepository.save(any(RatePlan.class))).thenAnswer(inv -> {
            RatePlan p = inv.getArgument(0);
            p.setId(20L);
            return p;
        });
        when(dailyRateRepository.findByRatePlanIdAndRateDate(any(), any())).thenReturn(Optional.empty());
        when(dailyRateRepository.findFirstByRatePlanIdOrderByRateDateDesc(any())).thenReturn(Optional.empty());

        CreateRatePlanRequest request = new CreateRatePlanRequest(
                "Flexible",
                new BigDecimal("3500.00"),
                7L,
                30,
                24,
                true);

        RatePlanConfigDto dto = service.createRatePlan(5L, request, staff);

        ArgumentCaptor<RatePlan> planCaptor = ArgumentCaptor.forClass(RatePlan.class);
        verify(ratePlanRepository).save(planCaptor.capture());
        RatePlan saved = planCaptor.getValue();

        assertThat(saved.getRefundPolicy().getId()).isEqualTo(7L);
        assertThat(saved.getRefundable()).isTrue();
        assertThat(saved.getFullCutoffValue()).isEqualTo(48);
        assertThat(dto.name()).isEqualTo("Flexible");
        assertThat(dto.refundPolicyId()).isEqualTo(7L);
        assertThat(dto.roomTypeId()).isEqualTo(5L);
        assertThat(dto.baseNightlyRate()).isEqualByComparingTo("3500.00");
        assertThat(saved.getBaseNightlyRate()).isEqualByComparingTo("3500.00");
        verify(dailyRateRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    void createRatePlan_rejectsMissingPolicy() {
        RoomType roomType = new RoomType();
        roomType.setId(5L);
        when(roomTypeRepository.findById(5L)).thenReturn(Optional.of(roomType));
        when(refundPolicyRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        CreateRatePlanRequest request =
                new CreateRatePlanRequest("X", new BigDecimal("100.00"), 99L, 30, 24, true);

        assertThatThrownBy(() -> service.createRatePlan(5L, request, staff))
                .hasMessageContaining("Refund policy not found");
    }
}
