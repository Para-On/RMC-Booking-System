package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.config.MayaProperties;
import RMC_Booking_Engine.rmc.domain.entity.Guest;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutCreated;
import RMC_Booking_Engine.rmc.dto.MayaCheckoutStatus;
import RMC_Booking_Engine.rmc.dto.MayaRefundResponse;
import RMC_Booking_Engine.rmc.exception.BusinessException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class MayaCheckoutClient {

    private final MayaProperties properties;

    public MayaCheckoutCreated createCheckout(
            String bookingReference,
            BigDecimal totalAmount,
            String currency,
            String itemName,
            Guest guest) {

        Map<String, Object> body = buildCreateBody(bookingReference, totalAmount, currency, itemName, guest);

        try {
            return restClient(true).post()
                    .uri("/checkout/v1/checkouts")
                    .body(body)
                    .retrieve()
                    .body(MayaCheckoutCreated.class);
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Unable to start Maya checkout: " + ex.getStatusCode().value());
        }
    }

    public MayaCheckoutStatus getCheckout(String checkoutId) {
        try {
            return restClient(false).get()
                    .uri("/checkout/v1/checkouts/{checkoutId}", checkoutId)
                    .retrieve()
                    .body(MayaCheckoutStatus.class);
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Unable to verify Maya checkout: " + ex.getStatusCode().value());
        }
    }

    public MayaRefundResponse refundCheckout(
            String checkoutId,
            BigDecimal amount,
            String currency,
            String reason) {
        Map<String, Object> body = Map.of(
                "reason", reason,
                "amount", Map.of(
                        "value", amount.toPlainString(),
                        "currency", currency));

        try {
            return restClient(false).post()
                    .uri("/checkout/v1/checkouts/{checkoutId}/refunds", checkoutId)
                    .body(body)
                    .retrieve()
                    .body(MayaRefundResponse.class);
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Unable to process Maya refund: " + ex.getStatusCode().value());
        }
    }

    private Map<String, Object> buildCreateBody(
            String bookingReference,
            BigDecimal totalAmount,
            String currency,
            String itemName,
            Guest guest) {

        String[] names = splitName(guest.getFullName());
        Map<String, Object> contact = Map.of(
                "phone", guest.getPhone(),
                "email", guest.getEmail());
        Map<String, Object> address = defaultAddress(guest);

        Map<String, Object> buyer = new LinkedHashMap<>();
        buyer.put("firstName", names[0]);
        buyer.put("lastName", names[1]);
        buyer.put("contact", contact);
        buyer.put("billingAddress", address);
        buyer.put("shippingAddress", address);

        Map<String, Object> item = Map.of(
                "name", itemName,
                "quantity", "1",
                "totalAmount", Map.of("value", totalAmount.toPlainString()));

        String successUrl = properties.frontendBaseUrl() + "/booking/success?reference=" + bookingReference;
        String failureUrl = properties.frontendBaseUrl() + "/booking/success?reference="
                + bookingReference + "&status=failed";
        String cancelUrl = properties.frontendBaseUrl() + "/booking/success?reference="
                + bookingReference + "&status=cancelled";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalAmount", Map.of(
                "value", totalAmount.toPlainString(),
                "currency", currency));
        body.put("requestReferenceNumber", bookingReference);
        body.put("buyer", buyer);
        body.put("items", List.of(item));
        body.put("redirectUrl", Map.of(
                "success", successUrl,
                "failure", failureUrl,
                "cancel", cancelUrl));
        return body;
    }

    private Map<String, Object> defaultAddress(Guest guest) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("firstName", splitName(guest.getFullName())[0]);
        address.put("lastName", splitName(guest.getFullName())[1]);
        address.put("phone", guest.getPhone());
        address.put("email", guest.getEmail());
        address.put("line1", "Ramada Manila Central");
        address.put("city", "Manila");
        address.put("state", "Metro Manila");
        address.put("zipCode", "1000");
        address.put("countryCode", "PH");
        return address;
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space <= 0) {
            return new String[] {trimmed, "Guest"};
        }
        return new String[] {trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }

    private RestClient restClient(boolean usePublicKey) {
        String apiKey = usePublicKey ? properties.publicKey() : properties.secretKey();
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(apiKey))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static String basicAuth(String apiKey) {
        String raw = apiKey + ":";
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
