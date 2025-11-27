package com.newwork.employee.entity.enums;

/**
 * Represents the relationship between the viewer and the profile owner.
 * Used to determine field visibility and edit permissions.
 */
public enum Relationship {
    /**
     * The viewer is viewing their own profile.
     * Full access to all fields (view + edit personal fields).
     */
    SELF,

    /**
     * The viewer is the direct manager of the profile owner.
     * Can view sensitive fields and edit non-sensitive fields of direct reports.
     */
    MANAGER,

    /**
     * The viewer is a coworker (neither self nor manager).
     * Can only view system-managed and non-sensitive fields.
     */
    COWORKER
}
