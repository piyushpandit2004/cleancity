
        package com.cleancity.cleancity;

import com.cleancity.cleancity.storage.ComplaintPhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintPhotoServiceTest {

    private ComplaintPhotoService complaintPhotoService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        complaintPhotoService = new ComplaintPhotoService();
        org.springframework.test.util.ReflectionTestUtils.setField(
                complaintPhotoService,
                "uploadDir",
                tempDir.toString()
        );
    }

    @Test
    void savePhoto_shouldSaveValidJpegPhoto() throws IOException {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "complaint.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        String savedPath = complaintPhotoService.savePhoto(photo);

        assertNotNull(savedPath);
        assertTrue(savedPath.contains("/original/"));
        assertTrue(Files.exists(Path.of(savedPath)));
    }

    @Test
    void saveCompletionPhoto_shouldSavePhotoInCompletionFolder()
            throws IOException {

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "cleaned.png",
                "image/png",
                "cleaned image".getBytes()
        );

        String savedPath =
                complaintPhotoService.saveCompletionPhoto(photo);

        assertNotNull(savedPath);
        assertTrue(savedPath.contains("/completion/"));
        assertTrue(Files.exists(Path.of(savedPath)));
    }

    @Test
    void savePhoto_shouldRejectNullPhoto() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.savePhoto(null)
        );

        assertEquals("Please select a photo", exception.getMessage());
    }

    @Test
    void savePhoto_shouldRejectEmptyPhoto() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.savePhoto(photo)
        );
    }

    @Test
    void savePhoto_shouldRejectFileLargerThan2MB() {
        byte[] largeContent = new byte[2 * 1024 * 1024 + 1];

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.savePhoto(photo)
        );

        assertEquals(
                "Photo size must not exceed 2 MB",
                exception.getMessage()
        );
    }

    @Test
    void savePhoto_shouldRejectUnsupportedContentType() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.savePhoto(photo)
        );

        assertEquals(
                "Only JPG, PNG and WEBP photos are allowed",
                exception.getMessage()
        );
    }

    @Test
    void savePhoto_shouldRejectInvalidFileExtension() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "image.gif",
                "image/gif",
                "gif content".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.savePhoto(photo)
        );
    }

    @Test
    void loadPhoto_shouldLoadExistingPhoto() throws IOException {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "complaint.webp",
                "image/webp",
                "webp content".getBytes()
        );

        String savedPath = complaintPhotoService.savePhoto(photo);

        Resource resource = complaintPhotoService.loadPhoto(savedPath);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadPhoto_shouldRejectMissingPath() {
        assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.loadPhoto(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.loadPhoto(" ")
        );
    }

    @Test
    void loadPhoto_shouldRejectPathOutsideUploadDirectory() {
        Path outsidePath = tempDir.getParent().resolve("outside.jpg");

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.loadPhoto(
                        outsidePath.toString()
                )
        );
    }

    @Test
    void loadPhoto_shouldRejectNonExistingPhoto() {
        Path missingPhoto = tempDir.resolve("missing.jpg");

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintPhotoService.loadPhoto(
                        missingPhoto.toString()
                )
        );
    }


    @Test
    void savePhoto_shouldAcceptJpegExtensionInUppercase() throws IOException {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "complaint.JPG",
                "image/jpeg",
                "test image content".getBytes()
        );

        String savedPath = complaintPhotoService.savePhoto(photo);

        assertNotNull(savedPath);
        assertTrue(savedPath.endsWith(".jpg"));
        assertTrue(Files.exists(Path.of(savedPath)));
    }

}
