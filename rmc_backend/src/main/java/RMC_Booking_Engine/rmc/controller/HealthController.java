package RMC_Booking_Engine.rmc.controller;

import RMC_Booking_Engine.rmc.config.RedisProperties;
import RMC_Booking_Engine.rmc.dto.HealthResponse;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final RedisProperties redisProperties;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;

    @GetMapping("/api/health")
    public HealthResponse health() {
        String database = checkDatabase();
        String redis = checkRedis();
        String status = "connected".equals(database) ? "ok" : "degraded";
        if (redisProperties.enabled() && !"connected".equals(redis)) {
            status = "degraded";
        }
        return new HealthResponse(status, database, redis);
    }

    private String checkDatabase() {
        try (var conn = dataSource.getConnection()) {
            return conn.isValid(2) ? "connected" : "unknown";
        } catch (Exception ex) {
            return "disconnected";
        }
    }

    private String checkRedis() {
        if (!redisProperties.enabled()) {
            return "disabled";
        }
        RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
        if (factory == null) {
            return "disconnected";
        }
        try (RedisConnection connection = factory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping()) ? "connected" : "unknown";
        } catch (Exception ex) {
            return "disconnected";
        }
    }
}
