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
public class PublicComplaintResponseDto {

    private Long id;

    private String title;

    private String description;

    private String location;

    private ComplaintStatus status;

    private LocalDateTime createdAt;

    private String photoUrl;

    public PublicComplaintResponseDto(Complaint complaint) {

        this.id = complaint.getId();

        this.title = complaint.getTitle();

        this.description = complaint.getDescription();

        this.location = complaint.getLocation();

        this.status = complaint.getStatus();

        this.createdAt = complaint.getCreatedAt();

        this.photoUrl = complaint.getPhotoPath() != null
                ? "/api/citizen/complaints/public/"
                + complaint.getId() + "/photo"
                : null;
    }
}