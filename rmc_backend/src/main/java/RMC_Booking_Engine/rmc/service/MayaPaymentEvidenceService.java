package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.domain.entity.MayaPaymentEvent;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.obs.LogRedaction;
import RMC_Booking_Engine.rmc.obs.RequestCorrelation;
import RMC_Booking_Engine.rmc.repository.MayaPaymentEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MayaPaymentEvidenceService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MayaPaymentEventRepository mayaPaymentEventRepository;

    public void record(MayaCheckoutStatus payload, String source, String fallbackReference) {
        if (payload == null) {
            return;
        }
        try {
            mayaPaymentEventRepository.save(toEvent(payload, source, fallbackReference));
        } catch (Exception ex) {
            log.error(
                    "Failed to retain Maya payment evidence [{}]",
                    RequestCorrelation.describe(),
                    LogRedaction.forLogging(ex));
        }
    }

    MayaPaymentEvent toEvent(MayaCheckoutStatus payload, String source, String fallbackReference)
            throws Exception {
        String json = LogRedaction.redact(OBJECT_MAPPER.writeValueAsString(payload));
        MayaPaymentEvent event = new MayaPaymentEvent();
        event.setSource(source == null || source.isBlank() ? "UNKNOWN" : source);
        event.setBookingReference(firstNonBlank(payload.requestReferenceNumber(), fallbackReference));
        event.setCheckoutId(payload.resolvedId());
        event.setCheckoutStatus(payload.status());
        event.setPaymentStatus(payload.paymentStatus());
        event.setAmount(payload.resolvedAmount());
        event.setCurrency(payload.currency());
        event.setPayloadJson(json);
        event.setPayloadSha256(sha256(json));
        String correlationId = RequestCorrelation.currentId();
        event.setCorrelationId("-".equals(correlationId) ? null : correlationId);
        event.setReceivedAt(Instant.now());
        return event;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
