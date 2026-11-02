package com.cleancity.cleancity.controller;

import com.cleancity.cleancity.dto.ComplaintResponseDto;
import com.cleancity.cleancity.dto.CreateComplaintRequestDto;
import com.cleancity.cleancity.dto.DuplicateComplaintResponseDto;
import com.cleancity.cleancity.dto.PublicComplaintResponseDto;
import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.service.ComplaintService;
import com.cleancity.cleancity.storage.ComplaintPhotoService;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/citizen/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintPhotoService complaintPhotoService;

    public ComplaintController(
            ComplaintService complaintService,
            ComplaintPhotoService complaintPhotoService) {

        this.complaintService = complaintService;
        this.complaintPhotoService = complaintPhotoService;
    }

    // 1. Create a complaint with duplicate detection and optional photo
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DuplicateComplaintResponseDto> createComplaint(

            @Valid @ModelAttribute CreateComplaintRequestDto request,

            @RequestParam(value = "photo", required = false)
            MultipartFile photo,

            @RequestParam(defaultValue = "false")
            boolean confirmDuplicate,

            @AuthenticationPrincipal UserDetails userDetails) {

        DuplicateComplaintResponseDto response =
                complaintService.createComplaintWithDuplicateCheck(
                        request.getTitle(),
                        request.getDescription(),
                        request.getLocation(),
                        userDetails.getUsername(),
                        confirmDuplicate
                );

        // Duplicate found: return warning without creating a new complaint
        if (response.isDuplicateFound() && !response.isSubmitted()) {
            return ResponseEntity.ok(response);
        }

        // Save the optional complaint photo
        if (response.isSubmitted()
                && photo != null
                && !photo.isEmpty()) {

            Long complaintId = response.getComplaint().getId();

            Complaint complaint =
                    complaintService.getComplaintById(complaintId);

            String photoPath = complaintPhotoService.savePhoto(photo);

            complaint.setPhotoPath(photoPath);

            Complaint updatedComplaint =
                    complaintService.saveComplaint(complaint);

            response.setComplaint(
                    new ComplaintResponseDto(updatedComplaint)
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 2. Get complaints created by the logged-in citizen
    @GetMapping
    public ResponseEntity<List<ComplaintResponseDto>> getMyComplaints(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ComplaintResponseDto> complaints =
                complaintService
                        .getCitizenComplaints(userDetails.getUsername())
                        .stream()
                        .map(ComplaintResponseDto::new)
                        .toList();

        return ResponseEntity.ok(complaints);
    }

    // 3. Get all public complaints
    @GetMapping("/public")
    public ResponseEntity<List<PublicComplaintResponseDto>>
    getPublicComplaints() {

        List<PublicComplaintResponseDto> complaints =
                complaintService.getPublicComplaints();

        return ResponseEntity.ok(complaints);
    }

    // 4. Filter public complaints by status
    @GetMapping("/public/filter")
    public ResponseEntity<List<PublicComplaintResponseDto>>
    getPublicComplaintsByStatus(
            @RequestParam ComplaintStatus status) {

        List<PublicComplaintResponseDto> complaints =
                complaintService.getPublicComplaintsByStatus(status);

        return ResponseEntity.ok(complaints);
    }

    // 5. Filter public complaints by location
    @GetMapping("/public/location")
    public ResponseEntity<List<PublicComplaintResponseDto>>
    getPublicComplaintsByLocation(
            @RequestParam String location) {

        List<PublicComplaintResponseDto> complaints =
                complaintService.getPublicComplaintsByLocation(location);

        return ResponseEntity.ok(complaints);
    }

    // 6. Get a specific complaint's original photo
    @GetMapping("/{complaintId}/photo")
    public ResponseEntity<Resource> getComplaintPhoto(
            @PathVariable Long complaintId) {

        Complaint complaint =
                complaintService.getComplaintById(complaintId);

        return buildPhotoResponse(complaint);
    }

    // 7. Get an original photo through the public complaint URL
    @GetMapping("/public/{complaintId}/photo")
    public ResponseEntity<Resource> getPublicComplaintPhoto(
            @PathVariable Long complaintId) {

        Complaint complaint =
                complaintService.getComplaintById(complaintId);

        return buildPhotoResponse(complaint);
    }

    // 8. Get worker's completion photo
    // URL: GET /api/citizen/complaints/{complaintId}/completion-photo
    @GetMapping("/{complaintId}/completion-photo")
    public ResponseEntity<Resource> getCompletionPhoto(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verify permission and get the completion photo path
        String photoPath = complaintService.getCompletionPhotoPath(
                complaintId,
                userDetails.getUsername()
        );

        // Photo not uploaded
        if (photoPath == null || photoPath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // Load completion photo
        Resource photo = complaintPhotoService.loadPhoto(photoPath);

        if (photo == null || !photo.exists() || !photo.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = getImageMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"completion-photo\""
                )
                .body(photo);
    }

    // Helper method to return the original complaint image
    private ResponseEntity<Resource> buildPhotoResponse(
            Complaint complaint) {

        String photoPath = complaint.getPhotoPath();

        if (photoPath == null || photoPath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource photo = complaintPhotoService.loadPhoto(photoPath);

        if (photo == null || !photo.exists() || !photo.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = getImageMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"complaint-photo\""
                )
                .body(photo);
    }

    // Detect the image content type
    private MediaType getImageMediaType(String photoPath) {

        try {
            Path path = Paths.get(photoPath);
            String contentType = Files.probeContentType(path);

            if (contentType != null
                    && contentType.startsWith("image/")) {

                return MediaType.parseMediaType(contentType);
            }

        } catch (IOException | IllegalArgumentException exception) {
            // Use the default content type if detection fails
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    // 9. Update a citizen's own complaint
    @PutMapping("/{complaintId}")
    public ResponseEntity<ComplaintResponseDto> updateCitizenComplaint(
            @PathVariable Long complaintId,

            @Valid @RequestBody CreateComplaintRequestDto request,

            @AuthenticationPrincipal UserDetails userDetails) {

        Complaint updatedComplaint =
                complaintService.updateCitizenComplaint(
                        complaintId,
                        userDetails.getUsername(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getLocation()
                );

        return ResponseEntity.ok(
                new ComplaintResponseDto(updatedComplaint)
        );
    }
}