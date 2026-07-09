package RMC_Booking_Engine.rmc.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "branding_config")
@Getter
@Setter
public class BrandingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName = "RMC Booking";

    @Column(name = "staff_portal_title", nullable = false)
    private String staffPortalTitle = "RMC Staff";

    @Column(name = "staff_portal_subtitle")
    private String staffPortalSubtitle = "Booking portal";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "font_family", nullable = false)
    private String fontFamily = "Geist Variable";

    @Column(name = "primary_color", nullable = false)
    private String primaryColor = "#1a1a1a";

    @Column(name = "primary_foreground_color", nullable = false)
    private String primaryForegroundColor = "#fafafa";

    @Column(name = "secondary_color", nullable = false)
    private String secondaryColor = "#f4f4f5";

    @Column(name = "secondary_foreground_color", nullable = false)
    private String secondaryForegroundColor = "#1a1a1a";

    @Column(name = "accent_color", nullable = false)
    private String accentColor = "#f4f4f5";

    @Column(name = "accent_foreground_color", nullable = false)
    private String accentForegroundColor = "#1a1a1a";

    @Column(name = "border_color", nullable = false)
    private String borderColor = "#e4e4e7";

    @Column(name = "ring_color", nullable = false)
    private String ringColor = "#a1a1aa";

    @Column(name = "destructive_color", nullable = false)
    private String destructiveColor = "#dc2626";

    @Column(name = "header_background_color", nullable = false)
    private String headerBackgroundColor = "#0f172a";

    @Column(name = "header_foreground_color", nullable = false)
    private String headerForegroundColor = "#f8fafc";

    @Column(name = "footer_background_color", nullable = false)
    private String footerBackgroundColor = "#0f172a";

    @Column(name = "footer_foreground_color", nullable = false)
    private String footerForegroundColor = "#f8fafc";

    @Column(name = "footer_text", length = 500)
    private String footerText;

    @Column(name = "footer_contact_email", length = 120)
    private String footerContactEmail;

    @Column(name = "footer_contact_phone", length = 40)
    private String footerContactPhone;

    @Column(name = "footer_copyright", length = 200)
    private String footerCopyright;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
