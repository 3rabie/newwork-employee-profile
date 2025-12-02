package com.newwork.employee.service;

import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.entity.enums.Relationship;

import java.util.Set;
import java.util.UUID;

/**
 * Service for evaluating permissions based on user relationships and field types.
 * Implements the permission matrix defined in PRD Section 3.3.
 */
public interface PermissionService {

    /**
     * Determines the relationship between the viewer and the profile owner.
     *
     * @param viewerId the ID of the user viewing the profile
     * @param profileOwnerId the ID of the profile being viewed
     * @return the relationship type (SELF, MANAGER, or COWORKER)
     */
    Relationship determineRelationship(UUID viewerId, UUID profileOwnerId);

    /**
     * Determines the relationship between the viewer and the profile owner.
     *
     * @param viewer the user viewing the profile
     * @param profileOwner the user whose profile is being viewed
     * @return the relationship type (SELF, MANAGER, or COWORKER)
     */
    Relationship determineRelationship(User viewer, User profileOwner);

    /**
     * Checks if the viewer can view a field of the given type.
     *
     * @param relationship the relationship between viewer and profile owner
     * @param fieldType the classification of the field being accessed
     * @return true if the viewer can view the field, false otherwise
     */
    boolean canView(Relationship relationship, FieldType fieldType);

    /**
     * Checks if the viewer can edit a field of the given type.
     *
     * @param relationship the relationship between viewer and profile owner
     * @param fieldType the classification of the field being modified
     * @return true if the viewer can edit the field, false otherwise
     */
    boolean canEdit(Relationship relationship, FieldType fieldType);

    /**
     * Checks if the viewer can view a specific field type on a profile.
     *
     * @param viewerId the ID of the user viewing the profile
     * @param profileOwnerId the ID of the profile being viewed
     * @param fieldType the classification of the field being accessed
     * @return true if the viewer can view the field, false otherwise
     */
    boolean canView(UUID viewerId, UUID profileOwnerId, FieldType fieldType);

    /**
     * Checks if the viewer can edit a specific field type on a profile.
     *
     * @param viewerId the ID of the user viewing the profile
     * @param profileOwnerId the ID of the profile being modified
     * @param fieldType the classification of the field being modified
     * @return true if the viewer can edit the field, false otherwise
     */
    boolean canEdit(UUID viewerId, UUID profileOwnerId, FieldType fieldType);

    /**
     * Returns the set of field types that are visible for the given relationship.
     * This method encapsulates the permission logic for field visibility.
     *
     * @param relationship the relationship between viewer and profile owner
     * @return set of field types that the viewer can see
     */
    Set<FieldType> getVisibleFieldTypes(Relationship relationship);
}
