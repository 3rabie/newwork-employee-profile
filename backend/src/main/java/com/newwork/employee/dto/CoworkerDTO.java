package com.newwork.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight DTO for displaying coworker directory entries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoworkerDTO {

    private UUID userId;
    private String employeeId;
    private String preferredName;
    private String legalFirstName;
    private String legalLastName;
    private String jobTitle;
    private String department;
    private String workLocationType;
    private String profilePhotoUrl;
    private String relationship;
    private boolean directReport;
    private Integer pendingAbsenceCount;
}
