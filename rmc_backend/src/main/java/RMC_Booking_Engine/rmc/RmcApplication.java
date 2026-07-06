package RMC_Booking_Engine.rmc;

import RMC_Booking_Engine.rmc.config.AppMailProperties;
import RMC_Booking_Engine.rmc.config.JwtProperties;
import RMC_Booking_Engine.rmc.config.MayaProperties;
import RMC_Booking_Engine.rmc.config.RateLimitProperties;
import RMC_Booking_Engine.rmc.config.RedisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({
        JwtProperties.class,
        MayaProperties.class,
        AppMailProperties.class,
        RateLimitProperties.class,
        RedisProperties.class})
public class RmcApplication {

	public static void main(String[] args) {
		SpringApplication.run(RmcApplication.class, args);
	}

}
