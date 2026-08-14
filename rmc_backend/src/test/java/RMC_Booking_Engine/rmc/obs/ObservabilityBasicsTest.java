package RMC_Booking_Engine.rmc.obs;

import static org.assertj.core.api.Assertions.assertThat;

import RMC_Booking_Engine.rmc.dto.ApiError;
import RMC_Booking_Engine.rmc.exception.GlobalExceptionHandler;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ObservabilityBasicsTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        RequestCorrelation.setId("corr-obs-1");
        RequestCorrelation.setBookingReference("RMC-20260929-3989");
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        RequestCorrelation.clear();
    }

    @Test
    void unhandledFailureIsLoggedWithCorrelationAndGenericClientBody() {
        ResponseEntity<ApiError> response = handler.handleGeneral(
                new IllegalStateException("secretKey=sk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().message()).doesNotContain("sk-");

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains("Unhandled server failure")
                .contains("correlationId=corr-obs-1")
                .contains("bookingReference=RMC-20260929-3989");
        assertThat(event.getFormattedMessage()).doesNotContain("sk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim");
        assertThat(event.getThrowableProxy().getMessage())
                .doesNotContain("sk-Pm9kco5Wfh3gz94LF2PNsK3LeLMYsVtqBJS9pD49Sim")
                .contains("[REDACTED]");
    }
}
