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

    private static final Path RIDER_DOCUMENT_UPLOAD_DIR =
            Paths.get("uploads", "rider-documents");

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /** Rider KYC docs (license, RC, Aadhar, PAN, insurance, photo) can be images or PDFs. */
    private static final Set<String> ALLOWED_RIDER_DOCUMENT_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    public String storeVehicleTypeImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(
                    "IMAGE_REQUIRED",
                    "Vehicle type image cannot be empty"
            );
        }

        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest(
                    "INVALID_IMAGE_TYPE",
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }

        return storeFile(file, VEHICLE_TYPE_UPLOAD_DIR, "vehicle-types",
                "FILE_UPLOAD_FAILED", "Unable to save vehicle type image");
    }

    /**
     * Stores a rider KYC document (license, RC, Aadhar, PAN, insurance, photo) uploaded
     * as multipart/form-data and returns the relative path to persist as fileUrl.
     */
    public String storeRiderDocument(Long riderId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(
                    "DOCUMENT_REQUIRED",
                    "Rider document file cannot be empty"
            );
        }

        if (!ALLOWED_RIDER_DOCUMENT_CONTENT_TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest(
                    "INVALID_DOCUMENT_TYPE",
                    "Only JPG, PNG, WEBP images or PDF files are allowed"
            );
        }

        return storeFile(file, RIDER_DOCUMENT_UPLOAD_DIR, "rider-documents",
                "FILE_UPLOAD_FAILED", "Unable to save rider document");
    }

    private String storeFile(MultipartFile file,
                             Path uploadDir,
                             String publicFolderName,
                             String ioErrorCode,
                             String ioErrorMessage) {
        try {
            Files.createDirectories(uploadDir);

            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() == null
                            ? "file"
                            : file.getOriginalFilename()
            );

            String extension = "";

            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex).toLowerCase();
            }

            String fileName =
                    UUID.randomUUID() + extension;

            Path absoluteTargetDir = uploadDir.toAbsolutePath().normalize();
            Path targetPath = absoluteTargetDir.resolve(fileName).normalize();

            if (!targetPath.startsWith(absoluteTargetDir)) {
                throw ApiException.badRequest(
                        "INVALID_FILE",
                        "Invalid file name"
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath
            );

            return "/uploads/" + publicFolderName + "/" + fileName;

        } catch (IOException e) {
            throw ApiException.badRequest(
                    ioErrorCode,
                    ioErrorMessage
            );
        }
    }
}