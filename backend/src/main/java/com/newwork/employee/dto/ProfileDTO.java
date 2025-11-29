package com.newwork.employee.dto;

import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.WorkLocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for employee profile data.
 * Fields are dynamically populated based on viewer's permissions.
 * Null fields indicate the viewer doesn't have permission to view them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {

    // Identity
    private UUID id;
    private UUID userId;
    private String email;
    private String employeeId;

    // ============================================
    // SYSTEM_MANAGED FIELDS
    // Visible to: SELF, MANAGER, COWORKER
    // Editable by: HR/IT only (out of scope)
    // ============================================
    private String legalFirstName;
    private String legalLastName;
    private String department;
    private String jobCode;
    private String jobFamily;
    private String jobLevel;
    private EmploymentStatus employmentStatus;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private BigDecimal fte;

    // ============================================
    // NON_SENSITIVE FIELDS
    // Visible to: SELF, MANAGER, COWORKER
    // Editable by: SELF, MANAGER (direct reports only)
    // ============================================
    private String preferredName;
    private String jobTitle;
    private String officeLocation;
    private String workPhone;
    private WorkLocationType workLocationType;
    private String bio;
    private String skills;
    private String profilePhotoUrl;

    // ============================================
    // SENSITIVE FIELDS
    // Visible to: SELF, MANAGER (direct reports only)
    // Editable by: SELF only
    // ============================================
    private String personalEmail;
    private String personalPhone;
    private String homeAddress;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private LocalDate dateOfBirth;
    private String visaWorkPermit;
    private BigDecimal absenceBalanceDays;
    private BigDecimal salary;
    private String performanceRating;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ProfileMetadataDTO metadata;
}
