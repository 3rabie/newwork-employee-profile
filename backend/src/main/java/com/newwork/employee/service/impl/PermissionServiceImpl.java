package com.newwork.employee.service.impl;

import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.entity.enums.Relationship;
import com.newwork.employee.exception.UserNotFoundException;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of permission evaluation service.
 * Based on PRD Section 3.3 Permission Matrix.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Relationship determineRelationship(UUID viewerId, UUID profileOwnerId) {
        log.debug("Determining relationship between viewer {} and profile owner {}", viewerId, profileOwnerId);

        // Check if viewing own profile
        if (viewerId.equals(profileOwnerId)) {
            log.debug("Relationship: SELF");
            return Relationship.SELF;
        }

        // Load users to check manager relationship
        User profileOwner = userRepository.findById(profileOwnerId)
                .orElseThrow(() -> new UserNotFoundException("Profile owner not found with id: " + profileOwnerId));

        // Check if viewer is the profile owner's manager
        if (profileOwner.getManager() != null && profileOwner.getManager().getId().equals(viewerId)) {
            log.debug("Relationship: MANAGER");
            return Relationship.MANAGER;
        }

        // Default to coworker
        log.debug("Relationship: COWORKER");
        return Relationship.COWORKER;
    }

    @Override
    public Relationship determineRelationship(User viewer, User profileOwner) {
        log.debug("Determining relationship between viewer {} and profile owner {}",
                viewer.getEmail(), profileOwner.getEmail());

        // Check if viewing own profile
        if (viewer.getId().equals(profileOwner.getId())) {
            log.debug("Relationship: SELF");
            return Relationship.SELF;
        }

        // Check if viewer is the profile owner's manager
        if (profileOwner.getManager() != null && profileOwner.getManager().getId().equals(viewer.getId())) {
            log.debug("Relationship: MANAGER");
            return Relationship.MANAGER;
        }

        // Default to coworker
        log.debug("Relationship: COWORKER");
        return Relationship.COWORKER;
    }

    @Override
    public boolean canView(Relationship relationship, FieldType fieldType) {
        boolean result = switch (fieldType) {
            case SYSTEM_MANAGED -> {
                // Everyone can view system-managed fields
                yield true;
            }
            case NON_SENSITIVE -> {
                // Everyone can view non-sensitive fields
                yield true;
            }
            case SENSITIVE -> {
                // Only SELF and MANAGER can view sensitive fields
                yield relationship == Relationship.SELF || relationship == Relationship.MANAGER;
            }
        };

        log.debug("canView({}, {}) = {}", relationship, fieldType, result);
        return result;
    }

    @Override
    public boolean canEdit(Relationship relationship, FieldType fieldType) {
        boolean result = switch (fieldType) {
            case SYSTEM_MANAGED -> {
                // System-managed fields are never editable through the application
                yield false;
            }
            case NON_SENSITIVE -> {
                // SELF and MANAGER can edit non-sensitive fields
                yield relationship == Relationship.SELF || relationship == Relationship.MANAGER;
            }
            case SENSITIVE -> {
                // Only SELF can edit sensitive fields
                yield relationship == Relationship.SELF;
            }
        };

        log.debug("canEdit({}, {}) = {}", relationship, fieldType, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canView(UUID viewerId, UUID profileOwnerId, FieldType fieldType) {
        Relationship relationship = determineRelationship(viewerId, profileOwnerId);
        return canView(relationship, fieldType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEdit(UUID viewerId, UUID profileOwnerId, FieldType fieldType) {
        Relationship relationship = determineRelationship(viewerId, profileOwnerId);
        return canEdit(relationship, fieldType);
    }

    @Override
    public Set<FieldType> getVisibleFieldTypes(Relationship relationship) {
        Set<FieldType> visibleTypes = new HashSet<>();
        for (FieldType fieldType : FieldType.values()) {
            if (canView(relationship, fieldType)) {
                visibleTypes.add(fieldType);
            }
        }
        log.debug("Visible field types for {}: {}", relationship, visibleTypes);
        return visibleTypes;
    }
}
