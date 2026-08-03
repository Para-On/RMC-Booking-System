package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.GuestRatePlanOfferDto;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.dto.RoomAvailabilityDto;
import RMC_Booking_Engine.rmc.repository.InventoryHoldRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RoomTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceMultiPlanTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private RatePlanRepository ratePlanRepository;
    @Mock
    private InventoryHoldRepository inventoryHoldRepository;
    @Mock
    private RoomAvailabilityMapper roomAvailabilityMapper;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(
                roomTypeRepository,
                ratePlanRepository,
                inventoryHoldRepository,
                roomAvailabilityMapper);
    }

    @Test
    void search_usesAllActivePlansAndReturnsFromMinTaxInclusive() {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setActive(true);
        roomType.setTotalCapacity(2);
        roomType.setOverbookingBuffer(0);

        RatePlan flexible = new RatePlan();
        flexible.setId(10L);
        flexible.setName("Flexible");
        flexible.setActive(true);

        RatePlan nonRefund = new RatePlan();
        nonRefund.setId(11L);
        nonRefund.setName("Non-refundable");
        nonRefund.setActive(true);

        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = checkIn.plusDays(2);

        when(roomTypeRepository.findByActiveTrue()).thenReturn(List.of(roomType));
        when(ratePlanRepository.findByRoomTypeIdAndActiveTrue(1L))
                .thenReturn(List.of(flexible, nonRefund));
        when(inventoryHoldRepository.countActiveHeldUnits(eq(1L), any(LocalDate.class))).thenReturn(0);

        GuestRatePlanOfferDto cheap = new GuestRatePlanOfferDto(
                11L,
                "Non-refundable",
                new BigDecimal("2000"),
                new BigDecimal("2200"),
                null,
                null,
                List.<NightlyRateDto>of(),
                false,
                false,
                "Non-refundable",
                0,
                null,
                false,
                0);
        GuestRatePlanOfferDto expensive = new GuestRatePlanOfferDto(
                10L,
                "Flexible",
                new BigDecimal("3000"),
                new BigDecimal("3300"),
                null,
                null,
                List.of(),
                true,
                true,
                "Flexible",
                48,
                null,
                true,
                50);

        RoomAvailabilityDto dto = new RoomAvailabilityDto(
                1L,
                11L,
                "Standard",
                "desc",
                2,
                0,
                2,
                new BigDecimal("2000"),
                new BigDecimal("2200"),
                new BigDecimal("2200"),
                null,
                null,
                "PHP",
                List.of(),
                List.of(cheap, expensive),
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                false,
                false,
                true);

        when(roomAvailabilityMapper.buildForStayMultiPlan(
                        eq(roomType), eq(List.of(flexible, nonRefund)), eq(checkIn), eq(checkOut), eq(2)))
                .thenReturn(dto);

        List<RoomAvailabilityDto> results = availabilityService.search(checkIn, checkOut, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).fromTotalTaxInclusive()).isEqualByComparingTo("2200");
        assertThat(results.get(0).ratePlanId()).isEqualTo(11L);
        assertThat(results.get(0).ratePlans()).hasSize(2);
        assertThat(results.get(0).policiesVary()).isTrue();
    }
}
