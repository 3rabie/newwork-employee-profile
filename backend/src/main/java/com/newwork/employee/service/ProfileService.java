package com.newwork.employee.service;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.dto.ProfileUpdateDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.entity.enums.Relationship;
import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.exception.ResourceNotFoundException;
import com.newwork.employee.mapper.ProfileMapper;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing employee profiles.
 * Handles profile retrieval and updates with permission checking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final EmployeeProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ProfileMapper profileMapper;

    /**
     * Get employee profile by user ID with permission filtering.
     * Fields are filtered based on the viewer's relationship to the profile owner.
     *
     * @param viewerId the ID of the user viewing the profile
     * @param profileUserId the ID of the profile owner
     * @return ProfileDTO with fields filtered by permissions
     * @throws ResourceNotFoundException if profile not found
     */
    @Transactional(readOnly = true)
    public ProfileDTO getProfile(UUID viewerId, UUID profileUserId) {
        log.debug("Getting profile for user {} viewed by {}", profileUserId, viewerId);

        // Find the profile
        EmployeeProfile profile = profileRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + profileUserId));

        // Determine relationship
        Relationship relationship = permissionService.determineRelationship(viewerId, profileUserId);
        log.debug("Relationship determined: {}", relationship);

        // Convert to DTO with permission filtering
        return profileMapper.toDTO(profile, relationship);
    }

    /**
     * Update employee profile with permission checking.
     * Only allows updates to fields the viewer has permission to edit.
     *
     * @param viewerId the ID of the user updating the profile
     * @param profileUserId the ID of the profile owner
     * @param updateDTO the update data
     * @return Updated ProfileDTO with fields filtered by permissions
     * @throws ResourceNotFoundException if profile not found
     * @throws ForbiddenException if viewer lacks permission to update
     */
    @Transactional
    public ProfileDTO updateProfile(UUID viewerId, UUID profileUserId, ProfileUpdateDTO updateDTO) {
        log.debug("Updating profile for user {} by viewer {}", profileUserId, viewerId);

        // Find the profile
        EmployeeProfile profile = profileRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + profileUserId));

        // Determine relationship
        Relationship relationship = permissionService.determineRelationship(viewerId, profileUserId);
        log.debug("Relationship determined: {}", relationship);

        // Check if any NON_SENSITIVE fields are being updated
        boolean updatingNonSensitive = hasNonSensitiveUpdates(updateDTO);
        // Check if any SENSITIVE fields are being updated
        boolean updatingSensitive = hasSensitiveUpdates(updateDTO);

        // Validate permissions
        if (updatingNonSensitive && !permissionService.canEdit(relationship, FieldType.NON_SENSITIVE)) {
            throw new ForbiddenException("You don't have permission to edit non-sensitive fields of this profile");
        }
        if (updatingSensitive && !permissionService.canEdit(relationship, FieldType.SENSITIVE)) {
            throw new ForbiddenException("You don't have permission to edit sensitive fields of this profile");
        }

        // Apply NON_SENSITIVE updates if allowed
        if (updatingNonSensitive && permissionService.canEdit(relationship, FieldType.NON_SENSITIVE)) {
            applyNonSensitiveUpdates(profile, updateDTO);
        }

        // Apply SENSITIVE updates if allowed
        if (updatingSensitive && permissionService.canEdit(relationship, FieldType.SENSITIVE)) {
            applySensitiveUpdates(profile, updateDTO);
        }

        // Save and return
        EmployeeProfile saved = profileRepository.save(profile);
        log.info("Profile updated for user {} by viewer {}", profileUserId, viewerId);

        // Return with permission filtering
        return profileMapper.toDTO(saved, relationship);
    }

    /**
     * Check if the update DTO contains any NON_SENSITIVE field updates.
     */
    private boolean hasNonSensitiveUpdates(ProfileUpdateDTO dto) {
        return dto.getPreferredName() != null ||
                dto.getJobTitle() != null ||
                dto.getOfficeLocation() != null ||
                dto.getWorkPhone() != null ||
                dto.getWorkLocationType() != null ||
                dto.getBio() != null ||
                dto.getSkills() != null ||
                dto.getProfilePhotoUrl() != null;
    }

    /**
     * Check if the update DTO contains any SENSITIVE field updates.
     */
    private boolean hasSensitiveUpdates(ProfileUpdateDTO dto) {
        return dto.getPersonalEmail() != null ||
                dto.getPersonalPhone() != null ||
                dto.getHomeAddress() != null ||
                dto.getEmergencyContactName() != null ||
                dto.getEmergencyContactPhone() != null ||
                dto.getEmergencyContactRelationship() != null ||
                dto.getDateOfBirth() != null ||
                dto.getVisaWorkPermit() != null;
    }

    /**
     * Apply NON_SENSITIVE field updates to the profile entity.
     */
    private void applyNonSensitiveUpdates(EmployeeProfile profile, ProfileUpdateDTO dto) {
        if (dto.getPreferredName() != null) {
            profile.setPreferredName(dto.getPreferredName());
        }
        if (dto.getJobTitle() != null) {
            profile.setJobTitle(dto.getJobTitle());
        }
        if (dto.getOfficeLocation() != null) {
            profile.setOfficeLocation(dto.getOfficeLocation());
        }
        if (dto.getWorkPhone() != null) {
            profile.setWorkPhone(dto.getWorkPhone());
        }
        if (dto.getWorkLocationType() != null) {
            profile.setWorkLocationType(dto.getWorkLocationType());
        }
        if (dto.getBio() != null) {
            profile.setBio(dto.getBio());
        }
        if (dto.getSkills() != null) {
            profile.setSkills(dto.getSkills());
        }
        if (dto.getProfilePhotoUrl() != null) {
            profile.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        }
    }

    /**
     * Apply SENSITIVE field updates to the profile entity.
     */
    private void applySensitiveUpdates(EmployeeProfile profile, ProfileUpdateDTO dto) {
        if (dto.getPersonalEmail() != null) {
            profile.setPersonalEmail(dto.getPersonalEmail());
        }
        if (dto.getPersonalPhone() != null) {
            profile.setPersonalPhone(dto.getPersonalPhone());
        }
        if (dto.getHomeAddress() != null) {
            profile.setHomeAddress(dto.getHomeAddress());
        }
        if (dto.getEmergencyContactName() != null) {
            profile.setEmergencyContactName(dto.getEmergencyContactName());
        }
        if (dto.getEmergencyContactPhone() != null) {
            profile.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        }
        if (dto.getEmergencyContactRelationship() != null) {
            profile.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        }
        if (dto.getDateOfBirth() != null) {
            profile.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getVisaWorkPermit() != null) {
            profile.setVisaWorkPermit(dto.getVisaWorkPermit());
        }
    }
}
