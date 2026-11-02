package com.cleancity.cleancity.dto;

import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ComplaintResponseDto {

    private Long id;

    private String title;

    private String description;

    private String location;

    private ComplaintStatus status;

    private String citizenEmail;

    private String assignedWorkerEmail;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Citizen ki original complaint photo
    private String photoUrl;

    // Worker ki completion photo
    private String completionPhotoUrl;

    // Complaint resolve hone ka time
    private LocalDateTime resolvedAt;

    public ComplaintResponseDto(Complaint complaint) {

        this.id = complaint.getId();

        this.title = complaint.getTitle();

        this.description = complaint.getDescription();

        this.location = complaint.getLocation();

        this.status = complaint.getStatus();

        this.citizenEmail = complaint.getCitizen() != null
                ? complaint.getCitizen().getEmail()
                : null;

        this.assignedWorkerEmail =
                complaint.getAssignedWorker() != null
                        ? complaint.getAssignedWorker().getEmail()
                        : null;

        this.rejectionReason = complaint.getRejectionReason();

        this.createdAt = complaint.getCreatedAt();

        this.updatedAt = complaint.getUpdatedAt();

        this.photoUrl = complaint.getPhotoPath() != null
                ? "/api/citizen/complaints/" + complaint.getId() + "/photo"
                : null;

        this.completionPhotoUrl =
                complaint.getCompletionPhotoPath() != null
                        ? "/api/worker/complaints/" + complaint.getId()
                        + "/completion-photo"
                        : null;

        this.resolvedAt = complaint.getResolvedAt();
    }
}