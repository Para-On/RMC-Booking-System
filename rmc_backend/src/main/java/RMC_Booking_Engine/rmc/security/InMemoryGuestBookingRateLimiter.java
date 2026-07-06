package RMC_Booking_Engine.rmc.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryGuestBookingRateLimiter implements GuestBookingRateLimiter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String clientKey, int limitPerMinute) {
        long windowStart = Instant.now().getEpochSecond() / 60;
        String bucketKey = clientKey + ":" + windowStart;

        WindowCounter counter = counters.computeIfAbsent(bucketKey, key -> new WindowCounter());
        if (counter.count.incrementAndGet() > limitPerMinute) {
            return false;
        }

        if (counters.size() > 10_000) {
            counters.keySet().removeIf(key -> !key.endsWith(":" + windowStart));
        }
        return true;
    }

    private static final class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
    }
}
