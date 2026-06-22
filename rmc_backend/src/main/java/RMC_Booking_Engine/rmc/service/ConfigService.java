package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.SystemConfigRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public BigDecimal getServiceChargePercent() {
        return new BigDecimal(getRequired("serviceChargePercent"));
    }

    public BigDecimal getVatPercent() {
        return new BigDecimal(getRequired("vatPercent"));
    }

    public String getDpaConsentVersion() {
        return getRequired("dpaConsentVersion");
    }

    private String getRequired(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(c -> c.getConfigValue())
                .orElseThrow(() -> new BusinessException("Missing system configuration: " + key));
    }
}
