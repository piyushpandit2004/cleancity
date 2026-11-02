package com.cleancity.cleancity.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class ComplaintPhotoService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // 1. Citizen ki original complaint photo save karna
    public String savePhoto(MultipartFile photo) {
        return savePhotoToFolder(photo, "original");
    }

    // 2. Worker ki cleaning ke baad wali photo save karna
    public String saveCompletionPhoto(MultipartFile photo) {
        return savePhotoToFolder(photo, "completion");
    }

    // 3. Photo validation aur save karne ka common logic
    private String savePhotoToFolder(
            MultipartFile photo,
            String folderName
    ) {

        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a photo"
            );
        }

        if (photo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Photo size must not exceed 2 MB"
            );
        }

        if (!ALLOWED_TYPES.contains(photo.getContentType())) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG and WEBP photos are allowed"
            );
        }

        try {
            // Main upload directory
            Path baseUploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            // Separate folder for each type of photo
            Path uploadPath = baseUploadPath
                    .resolve(folderName)
                    .normalize();

            if (!uploadPath.startsWith(baseUploadPath)) {
                throw new IllegalArgumentException(
                        "Invalid upload folder"
                );
            }

            Files.createDirectories(uploadPath);

            // Generate a unique filename
            String originalFilename = photo.getOriginalFilename();
            String extension = getExtension(originalFilename);

            String filename = UUID.randomUUID() + extension;

            Path destination = uploadPath.resolve(filename)
                    .normalize();

            if (!destination.startsWith(uploadPath)) {
                throw new IllegalArgumentException(
                        "Invalid filename"
                );
            }

            // Unique filename; no need to replace an existing file
            Files.copy(
                    photo.getInputStream(),
                    destination
            );

            // Save relative path in the database
            return uploadDir + "/" + folderName + "/" + filename;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to save photo",
                    e
            );
        }
    }

    // 4. Safe extension selection
    private String getExtension(String originalFilename) {

        if (originalFilename == null) {
            throw new IllegalArgumentException(
                    "Original filename is missing"
            );
        }

        int dotIndex = originalFilename.lastIndexOf('.');

        if (dotIndex < 0) {
            throw new IllegalArgumentException(
                    "Photo must have a valid extension"
            );
        }

        String extension = originalFilename
                .substring(dotIndex)
                .toLowerCase();

        if (!Set.of(".jpg", ".jpeg", ".png", ".webp")
                .contains(extension)) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG and WEBP photos are allowed"
            );
        }

        return extension;
    }

    // 5. Read a saved photo
    public Resource loadPhoto(String photoPath) {

        if (photoPath == null || photoPath.isBlank()) {
            throw new IllegalArgumentException(
                    "Photo path is missing"
            );
        }

        try {
            Path baseUploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Path savedPath = Paths.get(photoPath)
                    .toAbsolutePath()
                    .normalize();

            // Ensure the photo is inside the configured upload folder
            if (!savedPath.startsWith(baseUploadPath)) {
                throw new IllegalArgumentException(
                        "Invalid photo path"
                );
            }

            Resource resource = new UrlResource(
                    savedPath.toUri()
            );

            if (resource.exists() && resource.isReadable()) {
                return resource;
            }

            throw new IllegalArgumentException(
                    "Photo not found"
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to read photo",
                    e
            );
        }
    }
}