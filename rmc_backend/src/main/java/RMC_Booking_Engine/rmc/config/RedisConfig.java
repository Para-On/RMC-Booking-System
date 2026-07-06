package RMC_Booking_Engine.rmc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redisProperties.host(), redisProperties.port());
        return new LettuceConnectionFactory(config);
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
