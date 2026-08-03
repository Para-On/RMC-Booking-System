package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.dto.PricingPolicyResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import RMC_Booking_Engine.rmc.repository.SystemConfigRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public boolean isServiceChargeEnabled() {
        return getBoolean("serviceChargeEnabled", true);
    }

    public boolean isVatEnabled() {
        return getBoolean("vatEnabled", true);
    }

    public boolean isMunicipalTaxEnabled() {
        return getBoolean("municipalTaxEnabled", false);
    }

    public boolean isManualRefundEnabled() {
        return getBoolean("manualRefundEnabled", false);
    }

    public PricingPolicyResponse getPricingPolicy() {
        return new PricingPolicyResponse(
                isServiceChargeEnabled(), isVatEnabled(), isMunicipalTaxEnabled());
    }

    public BigDecimal getServiceChargePercent() {
        return new BigDecimal(getRequired("serviceChargePercent"));
    }

    public BigDecimal getVatPercent() {
        return new BigDecimal(getRequired("vatPercent"));
    }

    public BigDecimal getMunicipalTaxPercent() {
        return new BigDecimal(getRequired("municipalTaxPercent"));
    }

    public String getDpaConsentVersion() {
        return getRequired("dpaConsentVersion");
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> parseBoolean(config.getConfigValue(), defaultValue))
                .orElse(defaultValue);
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("true") || normalized.equals("1")) {
            return true;
        }
        if (normalized.equalsIgnoreCase("false") || normalized.equals("0")) {
            return false;
        }
        return defaultValue;
    }

    private String getRequired(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(c -> c.getConfigValue())
                .orElseThrow(() -> new BusinessException("Missing system configuration: " + key));
    }
}
