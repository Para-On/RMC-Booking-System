package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.DailyRate;
import RMC_Booking_Engine.rmc.dto.NightlyRateDto;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.DailyRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final DailyRateRepository dailyRateRepository;
    private final ConfigService configService;

    public List<NightlyRateDto> calculateStayPricing(Long ratePlanId, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new BusinessException("Check-out must be after check-in");
        }

        List<DailyRate> rates = dailyRateRepository.findRatesForStay(ratePlanId, checkIn, checkOut);
        if (rates.size() != nights) {
            throw new BusinessException("Rates are not available for the full stay period");
        }

        BigDecimal serviceChargePercent = configService.getServiceChargePercent();
        BigDecimal vatPercent = configService.getVatPercent();

        List<NightlyRateDto> nightlyRates = new ArrayList<>();
        for (DailyRate rate : rates) {
            nightlyRates.add(buildNightly(rate.getRateDate(), rate.getAmount(), serviceChargePercent, vatPercent));
        }
        return nightlyRates;
    }

    public BigDecimal sumBase(List<NightlyRateDto> nightlyRates) {
        return nightlyRates.stream()
                .map(NightlyRateDto::baseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumTaxInclusive(List<NightlyRateDto> nightlyRates) {
        return nightlyRates.stream()
                .map(NightlyRateDto::taxInclusiveTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NightlyRateDto buildNightly(
            LocalDate date,
            BigDecimal base,
            BigDecimal serviceChargePercent,
            BigDecimal vatPercent) {
        BigDecimal serviceCharge = base
                .multiply(serviceChargePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal subtotal = base.add(serviceCharge);
        BigDecimal vat = subtotal
                .multiply(vatPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal taxInclusive = subtotal.add(vat);
        return new NightlyRateDto(date, base, serviceCharge, vat, taxInclusive);
    }
}
