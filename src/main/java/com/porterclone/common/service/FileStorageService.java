package com.porterclone.common.service;

import com.porterclone.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Path VEHICLE_TYPE_UPLOAD_DIR =
            Paths.get("uploads", "vehicle-types");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public String storeVehicleTypeImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(
                    "IMAGE_REQUIRED",
                    "Vehicle type image cannot be empty"
            );
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest(
                    "INVALID_IMAGE_TYPE",
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }

        try {
            Files.createDirectories(VEHICLE_TYPE_UPLOAD_DIR);

            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() == null
                            ? "image"
                            : file.getOriginalFilename()
            );

            String extension = "";

            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex).toLowerCase();
            }

            String fileName =
                    UUID.randomUUID() + extension;

            Path targetPath =
                    VEHICLE_TYPE_UPLOAD_DIR.resolve(fileName).normalize();

            if (!targetPath.startsWith(
                    VEHICLE_TYPE_UPLOAD_DIR.toAbsolutePath().normalize()
            )) {
                throw ApiException.badRequest(
                        "INVALID_FILE",
                        "Invalid file name"
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath
            );

            return "/uploads/vehicle-types/" + fileName;

        } catch (IOException e) {
            throw ApiException.badRequest(
                    "FILE_UPLOAD_FAILED",
                    "Unable to save vehicle type image"
            );
        }
    }
}