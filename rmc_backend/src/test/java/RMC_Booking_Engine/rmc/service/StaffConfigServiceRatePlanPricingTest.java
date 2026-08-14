package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.DailyRate;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.dto.UpdateDailyRatesRequest;
import RMC_Booking_Engine.rmc.dto.UpdateRatePlanRequest;
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
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffConfigServiceRatePlanPricingTest {

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
    void updatePrimaryRate_skipsOverriddenNights() {
        RoomType roomType = new RoomType();
        roomType.setId(5L);
        roomType.setName("Deluxe");
        RefundPolicy policy = new RefundPolicy();
        policy.setId(7L);
        policy.setName("Flexible");

        RatePlan plan = new RatePlan();
        plan.setId(20L);
        plan.setName("Flexible");
        plan.setRoomType(roomType);
        plan.setRefundPolicy(policy);
        plan.setActive(true);
        plan.setBaseNightlyRate(new BigDecimal("3000.00"));
        plan.setHoldTtlMinutes(30);
        plan.setPolicyEnabled(true);
        plan.setRefundable(true);

        LocalDate today = LocalDate.now();
        DailyRate overridden = new DailyRate();
        overridden.setRatePlan(plan);
        overridden.setRateDate(today);
        overridden.setAmount(new BigDecimal("9999.00"));
        overridden.setOverridden(true);
        overridden.setCurrency("PHP");

        when(ratePlanRepository.findByIdWithRoomType(20L)).thenReturn(Optional.of(plan));
        when(dailyRateRepository.findByRatePlanIdAndRateDate(eq(20L), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate d = inv.getArgument(1);
                    if (d.equals(today)) {
                        return Optional.of(overridden);
                    }
                    return Optional.empty();
                });
        when(ratePlanRepository.save(any(RatePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateRatePlan(
                20L,
                new UpdateRatePlanRequest(null, null, null, null, null, new BigDecimal("4000.00")),
                staff);

        ArgumentCaptor<DailyRate> rateCaptor = ArgumentCaptor.forClass(DailyRate.class);
        verify(dailyRateRepository, org.mockito.Mockito.atLeastOnce()).save(rateCaptor.capture());
        assertThat(rateCaptor.getAllValues())
                .noneMatch(r -> today.equals(r.getRateDate()));
        assertThat(overridden.getAmount()).isEqualByComparingTo("9999.00");
        assertThat(plan.getBaseNightlyRate()).isEqualByComparingTo("4000.00");
    }

    @Test
    void updateDailyRates_marksOverride() {
        RoomType roomType = new RoomType();
        roomType.setId(5L);
        RatePlan plan = new RatePlan();
        plan.setId(20L);
        plan.setRoomType(roomType);

        LocalDate day = LocalDate.now().plusDays(3);
        when(ratePlanRepository.findByIdWithRoomType(20L)).thenReturn(Optional.of(plan));
        when(dailyRateRepository.findByRatePlanIdAndRateDate(20L, day)).thenReturn(Optional.empty());

        int updated = service.updateDailyRates(
                20L,
                new UpdateDailyRatesRequest(day, day, new BigDecimal("5500.00")),
                staff);

        assertThat(updated).isEqualTo(1);
        ArgumentCaptor<DailyRate> captor = ArgumentCaptor.forClass(DailyRate.class);
        verify(dailyRateRepository).save(captor.capture());
        assertThat(captor.getValue().isOverridden()).isTrue();
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("5500.00");
    }
}
