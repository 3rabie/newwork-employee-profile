package com.newwork.employee.mapper;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Pure mapper for converting EmployeeProfile entities to ProfileDTOs.
 * Field filtering is determined by the caller (service layer) via visible field types.
 */
@Component
public class ProfileMapper {

    /**
     * Convert EmployeeProfile entity to ProfileDTO with field filtering.
     * Only fields whose types are in the visibleFieldTypes set will be included.
     *
     * @param profile           the employee profile entity
     * @param visibleFieldTypes set of field types that should be included in the DTO
     * @return ProfileDTO with fields filtered based on visible types
     */
    public ProfileDTO toDTO(EmployeeProfile profile, Set<FieldType> visibleFieldTypes) {
        if (profile == null) {
            return null;
        }

        ProfileDTO.ProfileDTOBuilder builder = ProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .email(profile.getUser().getEmail())
                .employeeId(profile.getUser().getEmployeeId())
                .createdAt(DateTimeUtil.toOffset(profile.getCreatedAt()))
                .updatedAt(DateTimeUtil.toOffset(profile.getUpdatedAt()));

        // SYSTEM_MANAGED fields
        if (visibleFieldTypes.contains(FieldType.SYSTEM_MANAGED)) {
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

        // NON_SENSITIVE fields
        if (visibleFieldTypes.contains(FieldType.NON_SENSITIVE)) {
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

        // SENSITIVE fields
        if (visibleFieldTypes.contains(FieldType.SENSITIVE)) {
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
}
