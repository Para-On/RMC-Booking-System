package RMC_Booking_Engine.rmc.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MayaCheckoutCreatedTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsCheckoutIdField() throws Exception {
        MayaCheckoutCreated created = mapper.readValue(
                """
                {"checkoutId":"abc-123","redirectUrl":"https://payments-web-sandbox.maya.ph/v2/checkout?id=abc-123"}
                """,
                MayaCheckoutCreated.class);
        assertEquals("abc-123", created.resolvedCheckoutId());
    }

    @Test
    void mapsIdAliasWhenCheckoutIdMissing() throws Exception {
        MayaCheckoutCreated created = mapper.readValue(
                """
                {"id":"from-id","redirectUrl":"https://payments-web-sandbox.maya.ph/v2/checkout?id=other"}
                """,
                MayaCheckoutCreated.class);
        assertEquals("from-id", created.resolvedCheckoutId());
    }

    @Test
    void fallsBackToRedirectUrlQueryId() {
        MayaCheckoutCreated created = new MayaCheckoutCreated(
                null,
                "https://payments-web-sandbox.maya.ph/v2/checkout?id=93fe89db-c00f-4d44-be8b-54906bbdf9b9");
        assertEquals("93fe89db-c00f-4d44-be8b-54906bbdf9b9", created.resolvedCheckoutId());
    }

    @Test
    void returnsNullWhenNoIdAvailable() {
        assertNull(new MayaCheckoutCreated(null, null).resolvedCheckoutId());
        assertNull(new MayaCheckoutCreated("  ", "https://example.com/no-id").resolvedCheckoutId());
    }
}
