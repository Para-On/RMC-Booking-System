package RMC_Booking_Engine.rmc.config;

import RMC_Booking_Engine.rmc.service.RoomImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UploadResourceConfig implements WebMvcConfigurer {

    private final RoomImageStorageService roomImageStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = roomImageStorageService.getUploadRoot().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
