package com.cleancity.cleancity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintStatisticsDto {

    private long totalComplaints;

    private long pendingComplaints;

    private long assignedComplaints;

    private long inProgressComplaints;

    private long resolvedComplaints;

    private long rejectedComplaints;
}