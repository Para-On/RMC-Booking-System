package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.service.MayaPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/maya")
@RequiredArgsConstructor
@Slf4j
public class MayaWebhookController {

    private final MayaPaymentService mayaPaymentService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody MayaCheckoutStatus payload) {
        try {
            mayaPaymentService.handleWebhookPayload(payload);
        } catch (Exception ex) {
            log.error("Maya webhook processing error", ex);
        }
        return ResponseEntity.ok().build();
    }
}
