package com.newwork.employee.entity.enums;

/**
 * Classification of employee profile fields for permission control.
 * Based on PRD Section 3.2 Field Classifications.
 */
public enum FieldType {
    /**
     * HR/IT-controlled data. Always read-only in the application.
     * Examples: Employee ID, Legal name, Work email, Manager ID, Department,
     * Job code, Job level, Employment status, Hire date, Termination date, FTE
     */
    SYSTEM_MANAGED,

    /**
     * Information intended for internal visibility and collaboration.
     * Examples: Preferred name, Job title, Office location, Work phone,
     * Work location type, Bio, Skills, Profile photo
     */
    NON_SENSITIVE,

    /**
     * Private or regulated information with restricted visibility.
     * Examples: Personal email, Personal phone, Home address,
     * Emergency contacts, Date of birth, Visa/work permit
     */
    SENSITIVE
}
