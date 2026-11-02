package com.cleancity.cleancity.controller;

import com.cleancity.cleancity.dto.ComplaintResponseDto;
import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.service.ComplaintService;
import com.cleancity.cleancity.storage.ComplaintPhotoService;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    private final ComplaintService complaintService;
    private final ComplaintPhotoService complaintPhotoService;

    public WorkerController(
            ComplaintService complaintService,
            ComplaintPhotoService complaintPhotoService) {

        this.complaintService = complaintService;
        this.complaintPhotoService = complaintPhotoService;
    }

    // 1. Get available complaints
    @GetMapping("/complaints/available")
    public List<ComplaintResponseDto> getAvailableComplaints(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        List<Complaint> complaints =
                complaintService.getAvailableComplaints(
                        userDetails.getUsername()
                );

        return complaints.stream()
                .map(ComplaintResponseDto::new)
                .toList();
    }

    // 2. Accept a complaint
    @PutMapping("/complaints/{complaintId}/accept")
    public ComplaintResponseDto acceptComplaint(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        Complaint complaint =
                complaintService.acceptComplaint(
                        complaintId,
                        userDetails.getUsername()
                );

        return new ComplaintResponseDto(complaint);
    }

    // 3. View original complaint photo
    // Only the worker assigned to this complaint can view it.
    @GetMapping("/complaints/{complaintId}/original-photo")
    public ResponseEntity<Resource> getOriginalComplaintPhoto(
            @PathVariable Long complaintId,
            Authentication authentication) {

        Complaint complaint =
                complaintService.getComplaintById(complaintId);

        if (complaint.getAssignedWorker() == null
                || !complaint.getAssignedWorker()
                .getEmail()
                .equals(authentication.getName())) {

            return ResponseEntity.status(403).build();
        }

        String photoPath = complaint.getPhotoPath();

        if (photoPath == null || photoPath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource photo = complaintPhotoService.loadPhoto(photoPath);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(photo)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(photo);
    }

    // 4. Get complaints assigned to the logged-in worker
    @GetMapping("/complaints")
    public List<ComplaintResponseDto> getMyComplaints(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        List<Complaint> complaints =
                complaintService.getWorkerComplaints(
                        userDetails.getUsername()
                );

        return complaints.stream()
                .map(ComplaintResponseDto::new)
                .toList();
    }

    // 5. Update complaint status
    @PutMapping("/complaints/{complaintId}/status")
    public ComplaintResponseDto updateComplaintStatus(
            @PathVariable Long complaintId,
            @RequestParam ComplaintStatus status,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        Complaint complaint =
                complaintService.updateComplaintStatus(
                        complaintId,
                        userDetails.getUsername(),
                        status
                );

        return new ComplaintResponseDto(complaint);
    }

    // 6. Complete complaint and upload after-cleaning photo
    @PutMapping(
            value = "/complaints/{complaintId}/complete",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComplaintResponseDto completeComplaint(
            @PathVariable Long complaintId,
            @RequestParam("photo") MultipartFile photo,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        Complaint complaint =
                complaintService.completeComplaint(
                        complaintId,
                        userDetails.getUsername(),
                        photo
                );

        return new ComplaintResponseDto(complaint);
    }

    // 7. View completion photo
    @GetMapping("/complaints/{complaintId}/completion-photo")
    public ResponseEntity<Resource> getCompletionPhoto(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String photoPath =
                complaintService.getCompletionPhotoPath(
                        complaintId,
                        userDetails.getUsername()
                );

        if (photoPath == null || photoPath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource photo = complaintPhotoService.loadPhoto(photoPath);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(photo)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(photo);
    }
}