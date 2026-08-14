package RMC_Booking_Engine.rmc.obs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        RequestCorrelation.clear();
    }

    @Test
    void echoesValidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/guest/bookings");
        request.addHeader(RequestCorrelation.HEADER, "checkout-abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertThat(RequestCorrelation.currentId()).isEqualTo("checkout-abc-123"));

        assertThat(response.getHeader(RequestCorrelation.HEADER)).isEqualTo("checkout-abc-123");
        assertThat(RequestCorrelation.currentId()).isEqualTo("-");
    }

    @Test
    void generatesCorrelationIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seen = new String[1];

        filter.doFilter(request, response, (req, res) -> {
            seen[0] = RequestCorrelation.currentId();
            assertThat(seen[0]).isNotEqualTo("-");
            assertThat(seen[0].length()).isGreaterThanOrEqualTo(8);
        });

        assertThat(response.getHeader(RequestCorrelation.HEADER)).isEqualTo(seen[0]);
        assertThat(RequestCorrelation.currentId()).isEqualTo("-");
    }

    @Test
    void rejectsUnsafeIncomingHeaderAndGeneratesNewId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelation.HEADER, "bad\nid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertThat(RequestCorrelation.currentId()).doesNotContain("\n"));

        assertThat(response.getHeader(RequestCorrelation.HEADER)).doesNotContain("\n");
        assertThat(response.getHeader(RequestCorrelation.HEADER)).isNotEqualTo("bad\nid");
    }

    @Test
    void bookingReferenceTiesCheckoutToWebhook() {
        RequestCorrelation.setId("corr-checkout-1");
        RequestCorrelation.setBookingReference("RMC-20260929-3989");
        assertThat(RequestCorrelation.describe())
                .contains("correlationId=corr-checkout-1")
                .contains("bookingReference=RMC-20260929-3989");

        RequestCorrelation.setId("corr-webhook-2");
        RequestCorrelation.setBookingReference("RMC-20260929-3989");
        assertThat(RequestCorrelation.describe())
                .contains("correlationId=corr-webhook-2")
                .contains("bookingReference=RMC-20260929-3989");
    }
}
