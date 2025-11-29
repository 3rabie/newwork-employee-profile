package com.newwork.employee.service.impl;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.dto.ProfileMetadataDTO;
import com.newwork.employee.dto.ProfileUpdateDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.entity.enums.Relationship;
import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.exception.ResourceNotFoundException;
import com.newwork.employee.mapper.ProfileMapper;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.PermissionService;
import com.newwork.employee.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing employee profiles.
 * Handles profile retrieval and updates with permission checking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final EmployeeProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ProfileMapper profileMapper;

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getProfile(UUID viewerId, UUID profileUserId) {
        log.debug("Getting profile for user {} viewed by {}", profileUserId, viewerId);

        EmployeeProfile profile = profileRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + profileUserId));

        Relationship relationship = permissionService.determineRelationship(viewerId, profileUserId);
        log.debug("Relationship determined: {}", relationship);

        ProfileDTO dto = profileMapper.toDTO(profile, relationship);
        dto.setMetadata(buildMetadata(relationship));
        return dto;
    }

    @Override
    @Transactional
    public ProfileDTO updateProfile(UUID viewerId, UUID profileUserId, ProfileUpdateDTO updateDTO) {
        log.debug("Updating profile for user {} by viewer {}", profileUserId, viewerId);

        EmployeeProfile profile = profileRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + profileUserId));

        Relationship relationship = permissionService.determineRelationship(viewerId, profileUserId);
        log.debug("Relationship determined: {}", relationship);

        boolean updatingNonSensitive = hasNonSensitiveUpdates(updateDTO);
        boolean updatingSensitive = hasSensitiveUpdates(updateDTO);

        if (updatingNonSensitive && !permissionService.canEdit(relationship, FieldType.NON_SENSITIVE)) {
            throw new ForbiddenException("You don't have permission to edit non-sensitive fields of this profile");
        }
        if (updatingSensitive && !permissionService.canEdit(relationship, FieldType.SENSITIVE)) {
            throw new ForbiddenException("You don't have permission to edit sensitive fields of this profile");
        }

        if (updatingNonSensitive && permissionService.canEdit(relationship, FieldType.NON_SENSITIVE)) {
            applyNonSensitiveUpdates(profile, updateDTO);
        }

        if (updatingSensitive && permissionService.canEdit(relationship, FieldType.SENSITIVE)) {
            applySensitiveUpdates(profile, updateDTO);
        }

        EmployeeProfile saved = profileRepository.save(profile);
        log.info("Profile updated for user {} by viewer {}", profileUserId, viewerId);

        ProfileDTO dto = profileMapper.toDTO(saved, relationship);
        dto.setMetadata(buildMetadata(relationship));
        return dto;
    }

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

    private ProfileMetadataDTO buildMetadata(Relationship relationship) {
        String relationshipLabel = relationship == Relationship.COWORKER ? "OTHER" : relationship.name();

        List<String> visibleFields = Arrays.stream(FieldType.values())
                .filter(fieldType -> permissionService.canView(relationship, fieldType))
                .map(FieldType::name)
                .toList();

        List<String> editableFields = Arrays.stream(FieldType.values())
                .filter(fieldType -> permissionService.canEdit(relationship, fieldType))
                .map(FieldType::name)
                .toList();

        return ProfileMetadataDTO.builder()
                .relationship(relationshipLabel)
                .visibleFields(visibleFields)
                .editableFields(editableFields)
                .build();
    }
}
