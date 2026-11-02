package com.cleancity.cleancity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuplicateComplaintResponseDto {

    private boolean duplicateFound;
    private String message;
    private Long existingComplaintId;
    private boolean submitted;
    private ComplaintResponseDto complaint;

    public DuplicateComplaintResponseDto() {
    }

    public DuplicateComplaintResponseDto(
            boolean duplicateFound,
            String message,
            Long existingComplaintId,
            boolean submitted,
            ComplaintResponseDto complaint) {

        this.duplicateFound = duplicateFound;
        this.message = message;
        this.existingComplaintId = existingComplaintId;
        this.submitted = submitted;
        this.complaint = complaint;
    }
}