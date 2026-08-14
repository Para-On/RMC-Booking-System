package RMC_Booking_Engine.rmc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import RMC_Booking_Engine.rmc.domain.entity.MayaPaymentEvent;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.obs.RequestCorrelation;
import RMC_Booking_Engine.rmc.repository.MayaPaymentEventRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MayaPaymentEvidenceServiceTest {

    @Mock
    private MayaPaymentEventRepository mayaPaymentEventRepository;

    private MayaPaymentEvidenceService evidenceService;

    @BeforeEach
    void setUp() {
        evidenceService = new MayaPaymentEvidenceService(mayaPaymentEventRepository);
        RequestCorrelation.setId("corr-evidence-1");
        RequestCorrelation.setBookingReference("RMC-EVID-1");
    }

    @AfterEach
    void clearCorrelation() {
        RequestCorrelation.clear();
    }

    @Test
    void record_storesRedactedJsonHashAndBookingReference() {
        MayaCheckoutStatus payload = new MayaCheckoutStatus(
                "pk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                new BigDecimal("2128.00"),
                null,
                "PHP",
                "RMC-EVID-1");

        evidenceService.record(payload, "MAYA_WEBHOOK", null);

        ArgumentCaptor<MayaPaymentEvent> captor = ArgumentCaptor.forClass(MayaPaymentEvent.class);
        verify(mayaPaymentEventRepository).save(captor.capture());
        MayaPaymentEvent stored = captor.getValue();
        assertThat(stored.getSource()).isEqualTo("MAYA_WEBHOOK");
        assertThat(stored.getBookingReference()).isEqualTo("RMC-EVID-1");
        assertThat(stored.getCheckoutId()).startsWith("pk-").isEqualTo(payload.id());
        assertThat(stored.getAmount()).isEqualByComparingTo("2128.00");
        assertThat(stored.getCorrelationId()).isEqualTo("corr-evidence-1");
        assertThat(stored.getPayloadSha256()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(stored.getPayloadJson()).contains("RMC-EVID-1");
        assertThat(stored.getPayloadJson()).contains("[REDACTED]");
        assertThat(stored.getPayloadJson()).doesNotContain("pk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim");
    }

    @Test
    void record_duplicateReceiptsAreStored() {
        MayaCheckoutStatus payload = new MayaCheckoutStatus(
                "chk-dup-1",
                null,
                "COMPLETED",
                "PAYMENT_SUCCESS",
                new BigDecimal("1500.00"),
                null,
                "PHP",
                "RMC-EVID-1");

        evidenceService.record(payload, "MAYA_WEBHOOK", null);
        evidenceService.record(payload, "MAYA_WEBHOOK", null);

        verify(mayaPaymentEventRepository, times(2)).save(any(MayaPaymentEvent.class));
    }

    @Test
    void record_repositoryFailureDoesNotThrow() {
        MayaCheckoutStatus payload = new MayaCheckoutStatus(
                "chk-fail-1",
                null,
                "PENDING",
                "PAYMENT_PENDING",
                new BigDecimal("1500.00"),
                null,
                "PHP",
                "RMC-EVID-1");
        when(mayaPaymentEventRepository.save(any(MayaPaymentEvent.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> evidenceService.record(payload, "MAYA_CONFIRM_POLL", "RMC-EVID-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void record_nullPayloadIsIgnored() {
        evidenceService.record(null, "MAYA_WEBHOOK", "RMC-EVID-1");
        verify(mayaPaymentEventRepository, times(0)).save(any());
    }
}
