package com.newwork.employee.dto;

import com.newwork.employee.entity.enums.WorkLocationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating employee profile.
 * Only contains fields that can be updated via the API.
 * SYSTEM_MANAGED fields are excluded (managed by HR/IT systems).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateDTO {

    // ============================================
    // NON_SENSITIVE FIELDS
    // Editable by: SELF, MANAGER (direct reports only)
    // ============================================

    @Size(max = 100, message = "Preferred name must not exceed 100 characters")
    private String preferredName;

    @Size(max = 150, message = "Job title must not exceed 150 characters")
    private String jobTitle;

    @Size(max = 200, message = "Office location must not exceed 200 characters")
    private String officeLocation;

    @Pattern(regexp = "^\\+?[0-9]{1,4}?[-.\\s]?\\(?[0-9]{1,3}?\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}$|^$",
            message = "Invalid work phone format")
    private String workPhone;

    private WorkLocationType workLocationType;

    @Size(max = 5000, message = "Bio must not exceed 5000 characters")
    private String bio;

    @Size(max = 1000, message = "Skills must not exceed 1000 characters")
    private String skills;

    @Size(max = 500, message = "Profile photo URL must not exceed 500 characters")
    private String profilePhotoUrl;

    // ============================================
    // SENSITIVE FIELDS
    // Editable by: SELF only
    // ============================================

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Personal email must not exceed 255 characters")
    private String personalEmail;

    @Pattern(regexp = "^\\+?[0-9]{1,4}?[-.\\s]?\\(?[0-9]{1,3}?\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}$|^$",
            message = "Invalid personal phone format")
    private String personalPhone;

    @Size(max = 500, message = "Home address must not exceed 500 characters")
    private String homeAddress;

    @Size(max = 200, message = "Emergency contact name must not exceed 200 characters")
    private String emergencyContactName;

    @Pattern(regexp = "^\\+?[0-9]{1,4}?[-.\\s]?\\(?[0-9]{1,3}?\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}$|^$",
            message = "Invalid emergency contact phone format")
    private String emergencyContactPhone;

    @Size(max = 50, message = "Emergency contact relationship must not exceed 50 characters")
    private String emergencyContactRelationship;

    private LocalDate dateOfBirth;

    @Size(max = 200, message = "Visa/work permit information must not exceed 200 characters")
    private String visaWorkPermit;
}
