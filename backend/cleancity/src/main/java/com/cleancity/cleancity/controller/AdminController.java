
        package com.cleancity.cleancity.controller;

import com.cleancity.cleancity.dto.AdminUpdateComplaintRequestDto;
import com.cleancity.cleancity.dto.AdminUpdateComplaintStatusRequestDto;
import com.cleancity.cleancity.dto.ComplaintResponseDto;
import com.cleancity.cleancity.dto.ComplaintStatisticsDto;
import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.service.ComplaintService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ComplaintService complaintService;

    public AdminController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Get all complaints
    @GetMapping("/complaints")
    public List<ComplaintResponseDto> getAllComplaints() {
        return complaintService.getAllComplaints()
                .stream()
                .map(ComplaintResponseDto::new)
                .toList();
    }

    // Assign complaint to worker
    @PutMapping("/complaints/{complaintId}/assign")
    public ComplaintResponseDto assignWorker(
            @PathVariable Long complaintId,
            @RequestParam String workerEmail) {

        Complaint complaint = complaintService.assignWorker(
                complaintId,
                workerEmail
        );

        return new ComplaintResponseDto(complaint);
    }

    // Filter complaints by status
    @GetMapping("/complaints/filter")
    public List<ComplaintResponseDto> getComplaintsByStatus(
            @RequestParam ComplaintStatus status) {

        return complaintService.getComplaintsByStatus(status)
                .stream()
                .map(ComplaintResponseDto::new)
                .toList();
    }

    // Reject complaint with reason
    @PutMapping("/complaints/{complaintId}/reject")
    public ComplaintResponseDto rejectComplaint(
            @PathVariable Long complaintId,
            @RequestParam String rejectionReason) {

        Complaint complaint = complaintService.rejectComplaint(
                complaintId,
                rejectionReason
        );

        return new ComplaintResponseDto(complaint);
    }

    // Edit complaint details
    @PutMapping("/complaints/{complaintId}/edit")
    public ComplaintResponseDto updateComplaint(
            @PathVariable Long complaintId,
            @RequestBody AdminUpdateComplaintRequestDto request) {

        Complaint complaint = complaintService.updateAdminComplaint(
                complaintId,
                request.getTitle(),
                request.getDescription(),
                request.getLocation()
        );

        return new ComplaintResponseDto(complaint);
    }

    // Change complaint status
    @PutMapping("/complaints/{complaintId}/status")
    public ComplaintResponseDto updateComplaintStatus(
            @PathVariable Long complaintId,
            @RequestBody AdminUpdateComplaintStatusRequestDto request) {

        Complaint complaint =
                complaintService.updateAdminComplaintStatus(
                        complaintId,
                        request.getStatus(),
                        request.getRejectionReason()
                );

        return new ComplaintResponseDto(complaint);
    }

    // Get complaint statistics
    @GetMapping("/complaints/statistics")
    public ResponseEntity<ComplaintStatisticsDto> getComplaintStatistics() {

        ComplaintStatisticsDto statistics =
                complaintService.getComplaintStatistics();

        return ResponseEntity.ok(statistics);
    }
}
