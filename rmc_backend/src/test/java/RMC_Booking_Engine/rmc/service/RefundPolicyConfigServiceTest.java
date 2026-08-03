package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.RefundPolicy;
import RMC_Booking_Engine.rmc.domain.enums.StaffRole;
import RMC_Booking_Engine.rmc.dto.RefundPolicyConfigDto;
import RMC_Booking_Engine.rmc.dto.UpdateRefundPolicyRequest;
import RMC_Booking_Engine.rmc.repository.ConfigurationAuditLogRepository;
import RMC_Booking_Engine.rmc.repository.RatePlanRepository;
import RMC_Booking_Engine.rmc.repository.RefundPolicyRepository;
import RMC_Booking_Engine.rmc.security.StaffPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundPolicyConfigServiceTest {

    @Mock
    private RefundPolicyRepository refundPolicyRepository;
    @Mock
    private RatePlanRepository ratePlanRepository;
    @Mock
    private ConfigurationAuditLogRepository configurationAuditLogRepository;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private RefundPolicyConfigService service;

    private StaffPrincipal staff;

    @BeforeEach
    void setUp() {
        staff = new StaffPrincipal(1L, "admin@rmc.local", "Admin", StaffRole.ADMIN);
        org.mockito.Mockito.lenient().when(configService.isManualRefundEnabled()).thenReturn(false);
    }

    @Test
    void createPolicy_persistsNightsFields() {
        when(refundPolicyRepository.existsByNameIgnoreCaseAndActiveTrue("Late cancel")).thenReturn(false);
        when(refundPolicyRepository.save(any(RefundPolicy.class))).thenAnswer(inv -> {
            RefundPolicy p = inv.getArgument(0);
            p.setId(3L);
            return p;
        });

        UpdateRefundPolicyRequest request = new UpdateRefundPolicyRequest(
                "Late cancel",
                true,
                48,
                "HOURS",
                true,
                50,
                true,
                2,
                "14:00",
                "Asia/Manila",
                "Keep up to 2 nights",
                true);

        RefundPolicyConfigDto dto = service.createPolicy(request, staff);

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.nightsDeductionEnabled()).isTrue();
        assertThat(dto.nightsDeducted()).isEqualTo(2);
        assertThat(dto.refundable()).isTrue();
    }

    @Test
    void deactivatePolicy_rejectedWhenActiveRatePlansReferenceIt() {
        RefundPolicy policy = new RefundPolicy();
        policy.setId(3L);
        policy.setActive(true);
        when(refundPolicyRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(policy));
        when(ratePlanRepository.countByRefundPolicyIdAndActiveTrue(3L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deactivatePolicy(3L, staff))
                .hasMessageContaining("active rate plan");
    }

    @Test
    void listPolicies_returnsActiveOnly() {
        RefundPolicy a = new RefundPolicy();
        a.setId(1L);
        a.setName("A");
        a.setActive(true);
        a.setEnabled(true);
        a.setFullCutoffValue(48);
        a.setFullCutoffUnit(RMC_Booking_Engine.rmc.domain.enums.CutoffUnit.HOURS);
        a.setPartialRefundPercent(50);
        a.setNightsDeducted(1);
        a.setCheckInTime(java.time.LocalTime.of(14, 0));
        a.setTimezone("Asia/Manila");
        a.setRefundable(true);
        when(refundPolicyRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(a));

        assertThat(service.listPolicies()).hasSize(1);
        verify(refundPolicyRepository).findByActiveTrueOrderByNameAsc();
    }
}
