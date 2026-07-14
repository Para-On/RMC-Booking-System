package RMC_Booking_Engine.rmc.service;

import RMC_Booking_Engine.rmc.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BrandingStorageService {

    private static final Set<String> LOGO_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/svg+xml");

    private static final Set<String> ALLOWED_UPLOAD_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/svg+xml",
            "application/octet-stream");

    private final Path uploadRoot;

    public BrandingStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeLogo(MultipartFile file) {
        return store(file, "branding/logos", 5 * 1024 * 1024, "Logo");
    }

    public String storeServiceAddonImage(MultipartFile file) {
        return store(file, "addons/services", 5 * 1024 * 1024, "Service image");
    }

    public String storeItemAddonImage(MultipartFile file) {
        return store(file, "addons/items", 5 * 1024 * 1024, "Item image");
    }

    public String storeStaffProfileImage(MultipartFile file) {
        return store(file, "staff/profiles", 5 * 1024 * 1024, "Profile photo");
    }

    private String store(MultipartFile file, String subdirectory, long maxBytes, String label) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(label + " file is required");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(label + " must be " + (maxBytes / (1024 * 1024)) + " MB or smaller");
        }

        String contentType = resolveContentType(file);
        String extension = extensionFor(contentType, file.getOriginalFilename());

        try {
            Path directory = uploadRoot.resolve(subdirectory);
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + extension;
            Path target = directory.resolve(filename).toAbsolutePath().normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + subdirectory + "/" + filename;
        } catch (IOException ex) {
            throw new BusinessException("Failed to store " + label.toLowerCase());
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && LOGO_IMAGE_TYPES.contains(contentType)) {
            return contentType;
        }
        if (contentType == null || ALLOWED_UPLOAD_TYPES.contains(contentType)) {
            return contentTypeFromFilename(file.getOriginalFilename());
        }
        throw new BusinessException("Unsupported logo file type. Use PNG, JPG, WebP, or SVG.");
    }

    private String contentTypeFromFilename(String filename) {
        if (filename == null) {
            throw new BusinessException("Unsupported logo file type");
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        throw new BusinessException("Unsupported logo file type. Use PNG, JPG, WebP, or SVG.");
    }

    private String extensionFor(String contentType, String filename) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            case "image/jpeg" -> lowerEndsWith(filename, ".jpeg") ? ".jpeg" : ".jpg";
            default -> ".jpg";
        };
    }

    private boolean lowerEndsWith(String filename, String suffix) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(suffix);
    }
}
