package RMC_Booking_Engine.rmc.security;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisGuestBookingRateLimiter implements GuestBookingRateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryConsume(String clientKey, int limitPerMinute) {
        long windowStart = Instant.now().getEpochSecond() / 60;
        String key = "rate:guest-booking:" + clientKey + ":" + windowStart;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }
        return count != null && count <= limitPerMinute;
    }
}
