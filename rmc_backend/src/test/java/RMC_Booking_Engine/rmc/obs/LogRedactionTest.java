package RMC_Booking_Engine.rmc.obs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogRedactionTest {

    @Test
    void redactsMayaKeysBearerBasicAndNamedSecrets() {
        String raw = "secretKey=sk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim "
                + "public pk-6fqJ3tdQaenewRcxp2Ogid1R2vRpCw26ZPXh23JHK5G "
                + "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig "
                + "Basic c2stc2VjcmV0Og== password=hunter2";

        String redacted = LogRedaction.redact(raw);

        assertThat(redacted).doesNotContain("sk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim");
        assertThat(redacted).doesNotContain("pk-6fqJ3tdQaenewRcxp2Ogid1R2vRpCw26ZPXh23JHK5G");
        assertThat(redacted).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        assertThat(redacted).doesNotContain("c2stc2VjcmV0Og==");
        assertThat(redacted).doesNotContain("hunter2");
        assertThat(redacted).contains("[REDACTED]");
    }

    @Test
    void redactsEmailPanAndCvv() {
        String raw = "guest jane.doe@hotel.example paid 4111111111111111 cvv 123";

        String redacted = LogRedaction.redact(raw);

        assertThat(redacted).doesNotContain("jane.doe@hotel.example");
        assertThat(redacted).doesNotContain("4111111111111111");
        assertThat(redacted).doesNotContain("cvv 123");
        assertThat(redacted).contains("[redacted-email]");
        assertThat(redacted).contains("[REDACTED]");
    }

    @Test
    void keepsBookingReference() {
        String reference = "RMC-20260929-3989";
        assertThat(LogRedaction.redact("hold expired for " + reference)).contains(reference);
    }

    @Test
    void forLoggingRedactsExceptionMessage() {
        Exception logged = LogRedaction.forLogging(
                new IllegalStateException("Maya secretKey=sk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim"));

        assertThat(logged.getMessage()).doesNotContain("sk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim");
        assertThat(logged.getMessage()).contains("[REDACTED]");
    }
}
