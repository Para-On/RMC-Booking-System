package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.Booking;
import RMC_Booking_Engine.rmc.domain.entity.PromoCode;
import RMC_Booking_Engine.rmc.domain.entity.RatePlan;
import RMC_Booking_Engine.rmc.domain.enums.PromoCodeType;
import RMC_Booking_Engine.rmc.domain.enums.PromoDiscountType;
import RMC_Booking_Engine.rmc.dto.CreatePromoCodeRequest;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.PromoCodeRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;
    @Mock
    private RatePlanRepository ratePlanRepository;

    @InjectMocks
    private PromoCodeService promoCodeService;

    private RatePlan ratePlan;
    private PromoCode special;
    private PromoCode corporate;

    @BeforeEach
    void setUp() {
        ratePlan = new RatePlan();
        ratePlan.setId(10L);
        ratePlan.setName("Flexible");

        special = baseCode(PromoCodeType.SPECIAL_RATE, "SUMMER10", null);
        corporate = baseCode(PromoCodeType.CORPORATE, "OFFER99", "ACME");
    }

    private PromoCode baseCode(PromoCodeType type, String offer, String org) {
        PromoCode pc = new PromoCode();
        pc.setId(1L);
        pc.setName("Test");
        pc.setPromoType(type);
        pc.setOfferCode(offer);
        pc.setOrganizationCode(org);
        pc.setDiscountType(PromoDiscountType.PERCENT);
        pc.setDiscountValue(new BigDecimal("10"));
        pc.setStartsOn(LocalDate.now().minusDays(1));
        pc.setEndsOn(LocalDate.now().plusDays(30));
        pc.setMaxUses(5);
        pc.setUsedCount(0);
        pc.setActive(true);
        pc.setRatePlans(Set.of(ratePlan));
        return pc;
    }

    @Test
    void createRejectsCorporateWithoutOrganizationCode() {
        CreatePromoCodeRequest request = new CreatePromoCodeRequest(
                "Corp",
                null,
                "CORPORATE",
                "OFFER1",
                null,
                "PERCENT",
                new BigDecimal("10"),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                10,
                true,
                List.of(10L));
        assertThrows(BusinessException.class, () -> promoCodeService.create(request));
    }

    @Test
    void resolveSpecialAppliesPercentDiscount() {
        when(promoCodeRepository.findByOfferCodeIgnoreCase(anyString())).thenReturn(Optional.of(special));
        var match = promoCodeService.resolveForRatePlan(
                10L, new BigDecimal("1000.00"), "SPECIAL_RATE", "summer10", null);
        assertTrue(match.isPresent());
        assertEquals(new BigDecimal("100.00"), match.get().amountOff());
    }

    @Test
    void resolveRejectsTypeMismatch() {
        when(promoCodeRepository.findByOfferCodeIgnoreCase(anyString())).thenReturn(Optional.of(special));
        assertThrows(
                BusinessException.class,
                () -> promoCodeService.resolveForRatePlan(
                        10L, new BigDecimal("1000"), "CORPORATE", "SUMMER10", "ACME"));
    }

    @Test
    void resolveCorporateRequiresOrganizationCode() {
        when(promoCodeRepository.findByOfferCodeIgnoreCase(anyString())).thenReturn(Optional.of(corporate));
        assertThrows(
                BusinessException.class,
                () -> promoCodeService.resolveForRatePlan(
                        10L, new BigDecimal("1000"), "CORPORATE", "OFFER99", null));
    }

    @Test
    void resolveCorporateAcceptsMatchingOrg() {
        when(promoCodeRepository.findByOfferCodeIgnoreCase(anyString())).thenReturn(Optional.of(corporate));
        var match = promoCodeService.resolveForRatePlan(
                10L, new BigDecimal("1000.00"), "CORPORATE", "OFFER99", "acme");
        assertTrue(match.isPresent());
    }

    @Test
    void resolveRejectsExhaustedCode() {
        special.setUsedCount(5);
        when(promoCodeRepository.findByOfferCodeIgnoreCase(anyString())).thenReturn(Optional.of(special));
        assertThrows(
                BusinessException.class,
                () -> promoCodeService.resolveForRatePlan(
                        10L, new BigDecimal("1000"), "SPECIAL_RATE", "SUMMER10", null));
    }

    @Test
    void consumeUsageFailsWhenLimitReached() {
        when(promoCodeRepository.tryIncrementUsedCount(1L)).thenReturn(0);
        assertThrows(BusinessException.class, () -> promoCodeService.consumeUsage(special));
    }

    @Test
    void releaseUsageDecrements() {
        Booking booking = new Booking();
        booking.setPromoCode(special);
        when(promoCodeRepository.tryDecrementUsedCount(1L)).thenReturn(1);
        promoCodeService.releaseUsageIfAttached(booking);
        verify(promoCodeRepository).tryDecrementUsedCount(1L);
        assertEquals(null, booking.getPromoCode());
    }

    @Test
    void createPersistsSpecialRate() {
        when(ratePlanRepository.findById(10L)).thenReturn(Optional.of(ratePlan));
        when(promoCodeRepository.findByOfferCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(inv -> {
            PromoCode saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        CreatePromoCodeRequest request = new CreatePromoCodeRequest(
                "Summer",
                null,
                "SPECIAL_RATE",
                "summer10",
                null,
                "PERCENT",
                new BigDecimal("15"),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                20,
                true,
                List.of(10L));

        var dto = promoCodeService.create(request);
        ArgumentCaptor<PromoCode> captor = ArgumentCaptor.forClass(PromoCode.class);
        verify(promoCodeRepository).save(captor.capture());
        assertEquals("SUMMER10", captor.getValue().getOfferCode());
        assertEquals(PromoCodeType.SPECIAL_RATE, captor.getValue().getPromoType());
        assertEquals(99L, dto.id());
    }
}
