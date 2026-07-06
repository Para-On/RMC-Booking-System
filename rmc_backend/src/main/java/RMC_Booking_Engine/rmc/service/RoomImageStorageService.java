package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RoomImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    private final Path uploadRoot;

    public RoomImageStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("Only JPEG, PNG, WebP, or GIF images are allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("Image must be 5 MB or smaller");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };

        try {
            Path directory = uploadRoot.resolve("room-images");
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + extension;
            Path target = directory.resolve(filename);
            file.transferTo(target);
            return "/uploads/room-images/" + filename;
        } catch (IOException ex) {
            throw new BusinessException("Failed to store image");
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }
}
