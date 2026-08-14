package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.entity.RoomType;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.dto.RoomAvailabilityDto;
import RMC_Booking_Engine.rmc.dto.RoomCatalogCardDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityMapperFromPlanTest {

    @Mock
    private PricingService pricingService;
    @Mock
    private PromoService promoService;
    @Mock
    private PromoCodeService promoCodeService;
    @Mock
    private RoomCatalogMapper roomCatalogMapper;

    private RoomAvailabilityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RoomAvailabilityMapper(pricingService, promoService, promoCodeService, roomCatalogMapper);
    }

    @Test
    void buildForStayMultiPlan_cardMediaFromRoomType_fromPriceFromCheapestPlan() {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("Garden Wing");
        roomType.setDescription("Type description");
        roomType.setMaxAdults(4);
        roomType.setMaxChildren(2);

        RatePlan cheap = new RatePlan();
        cheap.setId(10L);
        cheap.setName("Saver");
        cheap.setActive(true);
        cheap.setRefundable(false);
        cheap.setPolicyEnabled(false);
        cheap.setCancellationPolicy("Non-refundable");

        RatePlan expensive = new RatePlan();
        expensive.setId(11L);
        expensive.setName("Flex");
        expensive.setActive(true);
        expensive.setRefundable(true);
        expensive.setPolicyEnabled(true);
        expensive.setFullCutoffValue(48);
        expensive.setPolicyDescription("Free cancellation until 48 hours before check-in.");
        expensive.setCancellationPolicy("Flexible");

        LocalDate checkIn = LocalDate.of(2026, 8, 1);
        LocalDate checkOut = LocalDate.of(2026, 8, 2);

        when(pricingService.calculateStayPricing(eq(10L), eq(checkIn), eq(checkOut)))
                .thenReturn(List.of(night("2000", "2200")));
        when(pricingService.calculateStayPricing(eq(11L), eq(checkIn), eq(checkOut)))
                .thenReturn(List.of(night("3000", "3300")));
        when(pricingService.sumBase(any())).thenAnswer(inv -> {
            List<NightlyRateDto> rates = inv.getArgument(0);
            return rates.stream().map(NightlyRateDto::baseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        });
        when(pricingService.sumTaxInclusive(any())).thenAnswer(inv -> {
            List<NightlyRateDto> rates = inv.getArgument(0);
            return rates.stream()
                    .map(NightlyRateDto::taxInclusiveTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        });
        when(promoService.findBestAppliedPromo(any(), any())).thenReturn(java.util.Optional.empty());
        when(roomCatalogMapper.toCard(roomType)).thenReturn(new RoomCatalogCardDto(
                "Garden Wing",
                "Type description",
                4,
                2,
                new BigDecimal("28.00"),
                List.of("https://cdn.example/type.jpg"),
                List.of("WiFi", "Pool"),
                "Deluxe",
                "Garden",
                "King",
                false,
                true));

        RoomAvailabilityDto dto = mapper.buildForStayMultiPlan(
                roomType, List.of(cheap, expensive), checkIn, checkOut, 2);

        assertThat(dto.name()).isEqualTo("Garden Wing");
        assertThat(dto.description()).isEqualTo("Type description");
        assertThat(dto.maxAdults()).isEqualTo(4);
        assertThat(dto.imageUrls()).containsExactly("https://cdn.example/type.jpg");
        assertThat(dto.amenities()).containsExactly("WiFi", "Pool");
        assertThat(dto.ratePlanId()).isEqualTo(10L);
        assertThat(dto.fromTotalTaxInclusive()).isEqualByComparingTo("2200");
        assertThat(dto.refundable()).isFalse();
        assertThat(dto.ratePlans()).hasSize(2);
        assertThat(dto.ratePlans().get(0).name()).isEqualTo("Saver");
        assertThat(dto.ratePlans().get(0).policySummary()).isEqualTo("Non-refundable");
        assertThat(dto.ratePlans().get(1).name()).isEqualTo("Flex");
        assertThat(dto.ratePlans().get(1).policySummary())
                .isEqualTo("Free cancellation until 48 hours before check-in.");
        assertThat(dto.policiesVary()).isTrue();
        assertThat(dto.policySummary()).isNull();
    }

    private static NightlyRateDto night(String base, String taxInclusive) {
        return new NightlyRateDto(
                LocalDate.of(2026, 8, 1),
                new BigDecimal(base),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(taxInclusive));
    }
}
