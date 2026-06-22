package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.dto.HealthResponse;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/api/health")
    public HealthResponse health() {
        try (var conn = dataSource.getConnection()) {
            return new HealthResponse("ok", conn.isValid(2) ? "connected" : "unknown");
        } catch (Exception ex) {
            return new HealthResponse("degraded", "disconnected");
        }
    }
}
