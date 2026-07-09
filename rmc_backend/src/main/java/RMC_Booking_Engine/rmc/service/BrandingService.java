package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.BrandingConfig;
import RMC_Booking_Engine.rmc.domain.entity.ConfigurationAuditLog;
import RMC_Booking_Engine.rmc.dto.BrandingResponse;
import RMC_Booking_Engine.rmc.dto.UpdateBrandingRequest;
import RMC_Booking_Engine.rmc.repository.BrandingConfigRepository;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import RMC_Booking_Engine.rmc.util.BrandingColorUtil;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandingService {

    private final BrandingConfigRepository brandingConfigRepository;
    private final ConfigurationAuditLogRepository configurationAuditLogRepository;

    @Transactional(readOnly = true)
    public BrandingResponse getBranding() {
        return toResponse(loadConfig());
    }

    @Transactional
    public BrandingResponse updateBranding(UpdateBrandingRequest request, StaffPrincipal staff) {
        BrandingConfig config = loadConfig();
        String previous = summarize(config);

        config.setFontFamily(request.fontFamily().trim());
        config.setPrimaryColor(request.primaryColor());
        config.setSecondaryColor(request.secondaryColor());

        String primaryForeground = BrandingColorUtil.contrastingForeground(request.primaryColor());
        String secondaryForeground = BrandingColorUtil.contrastingForeground(request.secondaryColor());
        config.setPrimaryForegroundColor(primaryForeground);
        config.setSecondaryForegroundColor(secondaryForeground);
        config.setAccentColor(request.primaryColor());
        config.setAccentForegroundColor(primaryForeground);
        config.setHeaderBackgroundColor(request.secondaryColor());
        config.setHeaderForegroundColor(secondaryForeground);
        config.setFooterBackgroundColor(request.secondaryColor());
        config.setFooterForegroundColor(secondaryForeground);
        config.setFooterText(trimOrNull(request.footerText()));
        config.setFooterContactEmail(trimOrNull(request.footerContactEmail()));
        config.setFooterContactPhone(trimOrNull(request.footerContactPhone()));
        config.setFooterCopyright(trimOrNull(request.footerCopyright()));
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(staff.id());
        brandingConfigRepository.save(config);

        recordAudit(config.getId(), previous, summarize(config), staff.id());
        return toResponse(config);
    }

    @Transactional
    public BrandingResponse updateLogoUrl(String logoUrl, StaffPrincipal staff) {
        BrandingConfig config = loadConfig();
        String previous = config.getLogoUrl();
        config.setLogoUrl(logoUrl);
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(staff.id());
        brandingConfigRepository.save(config);
        recordAudit(config.getId(), previous, logoUrl, staff.id());
        return toResponse(config);
    }

    private BrandingConfig loadConfig() {
        return brandingConfigRepository.findAll().stream()
                .findFirst()
                .orElseGet(this::createDefault);
    }

    private BrandingConfig createDefault() {
        BrandingConfig config = new BrandingConfig();
        return brandingConfigRepository.save(config);
    }

    private BrandingResponse toResponse(BrandingConfig config) {
        String primaryForeground = BrandingColorUtil.contrastingForeground(config.getPrimaryColor());
        String secondaryForeground = BrandingColorUtil.contrastingForeground(config.getSecondaryColor());
        return new BrandingResponse(
                config.getLogoUrl(),
                config.getFontFamily(),
                config.getPrimaryColor(),
                primaryForeground,
                config.getSecondaryColor(),
                secondaryForeground,
                config.getPrimaryColor(),
                primaryForeground,
                config.getBorderColor(),
                config.getRingColor(),
                config.getDestructiveColor(),
                config.getSecondaryColor(),
                secondaryForeground,
                config.getSecondaryColor(),
                secondaryForeground,
                config.getFooterText(),
                config.getFooterContactEmail(),
                config.getFooterContactPhone(),
                config.getFooterCopyright());
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String summarize(BrandingConfig config) {
        return config.getFontFamily()
                + "|" + config.getPrimaryColor()
                + "|" + config.getLogoUrl();
    }

    private void recordAudit(Long entityId, String oldValue, String newValue, Long staffUserId) {
        if (oldValue != null && oldValue.equals(newValue)) {
            return;
        }
        ConfigurationAuditLog log = new ConfigurationAuditLog();
        log.setEntityType("BRANDING");
        log.setEntityId(entityId);
        log.setConfigKey("branding");
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setStaffUserId(staffUserId);
        configurationAuditLogRepository.save(log);
    }
}
