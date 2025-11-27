package com.newwork.employee.mapper;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.entity.enums.Relationship;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting EmployeeProfile entities to ProfileDTOs.
 * Applies permission-based field filtering based on viewer relationship.
 */
@Component
public class ProfileMapper {

    /**
     * Convert EmployeeProfile entity to ProfileDTO with full data (no filtering).
     * Use this when the viewer is the profile owner (SELF relationship).
     *
     * @param profile the employee profile entity
     * @return ProfileDTO with all fields populated
     */
    public ProfileDTO toDTO(EmployeeProfile profile) {
        return toDTO(profile, Relationship.SELF);
    }

    /**
     * Convert EmployeeProfile entity to ProfileDTO with permission filtering.
     * Fields are included/excluded based on the viewer's relationship to the profile owner.
     *
     * @param profile      the employee profile entity
     * @param relationship the viewer's relationship to the profile owner
     * @return ProfileDTO with fields filtered by permissions
     */
    public ProfileDTO toDTO(EmployeeProfile profile, Relationship relationship) {
        if (profile == null) {
            return null;
        }

        ProfileDTO.ProfileDTOBuilder builder = ProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt());

        // SYSTEM_MANAGED fields - visible to everyone (SELF, MANAGER, COWORKER)
        if (canView(relationship, FieldType.SYSTEM_MANAGED)) {
            builder
                    .legalFirstName(profile.getLegalFirstName())
                    .legalLastName(profile.getLegalLastName())
                    .department(profile.getDepartment())
                    .jobCode(profile.getJobCode())
                    .jobFamily(profile.getJobFamily())
                    .jobLevel(profile.getJobLevel())
                    .employmentStatus(profile.getEmploymentStatus())
                    .hireDate(profile.getHireDate())
                    .terminationDate(profile.getTerminationDate())
                    .fte(profile.getFte());
        }

        // NON_SENSITIVE fields - visible to everyone (SELF, MANAGER, COWORKER)
        if (canView(relationship, FieldType.NON_SENSITIVE)) {
            builder
                    .preferredName(profile.getPreferredName())
                    .jobTitle(profile.getJobTitle())
                    .officeLocation(profile.getOfficeLocation())
                    .workPhone(profile.getWorkPhone())
                    .workLocationType(profile.getWorkLocationType())
                    .bio(profile.getBio())
                    .skills(profile.getSkills())
                    .profilePhotoUrl(profile.getProfilePhotoUrl());
        }

        // SENSITIVE fields - visible to SELF and MANAGER only
        if (canView(relationship, FieldType.SENSITIVE)) {
            builder
                    .personalEmail(profile.getPersonalEmail())
                    .personalPhone(profile.getPersonalPhone())
                    .homeAddress(profile.getHomeAddress())
                    .emergencyContactName(profile.getEmergencyContactName())
                    .emergencyContactPhone(profile.getEmergencyContactPhone())
                    .emergencyContactRelationship(profile.getEmergencyContactRelationship())
                    .dateOfBirth(profile.getDateOfBirth())
                    .visaWorkPermit(profile.getVisaWorkPermit())
                    .absenceBalanceDays(profile.getAbsenceBalanceDays())
                    .salary(profile.getSalary())
                    .performanceRating(profile.getPerformanceRating());
        }

        return builder.build();
    }

    /**
     * Determine if a relationship allows viewing a specific field type.
     * Implements the permission matrix from PRD Section 3.3.
     *
     * @param relationship the viewer's relationship
     * @param fieldType    the field type
     * @return true if viewing is allowed
     */
    private boolean canView(Relationship relationship, FieldType fieldType) {
        return switch (fieldType) {
            case SYSTEM_MANAGED, NON_SENSITIVE ->
                // Everyone can view SYSTEM_MANAGED and NON_SENSITIVE fields
                    relationship == Relationship.SELF ||
                            relationship == Relationship.MANAGER ||
                            relationship == Relationship.COWORKER;
            case SENSITIVE ->
                // Only SELF and MANAGER can view SENSITIVE fields
                    relationship == Relationship.SELF ||
                            relationship == Relationship.MANAGER;
        };
    }
}
