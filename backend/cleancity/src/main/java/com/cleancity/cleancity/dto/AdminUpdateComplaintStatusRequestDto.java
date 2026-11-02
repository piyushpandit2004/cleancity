
        package com.cleancity.cleancity.dto;

import com.cleancity.cleancity.entity.ComplaintStatus;

public class AdminUpdateComplaintStatusRequestDto {

    private ComplaintStatus status;
    private String rejectionReason;

    public AdminUpdateComplaintStatusRequestDto() {
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
