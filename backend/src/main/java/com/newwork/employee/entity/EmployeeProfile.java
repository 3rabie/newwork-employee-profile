package com.newwork.employee.entity;

import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.WorkLocationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Employee Profile entity containing all profile information.
 * Fields are classified into three categories: SYSTEM_MANAGED, NON_SENSITIVE, and SENSITIVE.
 * See PRD Section 3.2 for field classifications.
 */
@Entity
@Table(name = "employee_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Link to User entity
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ============================================
    // SYSTEM-MANAGED FIELDS
    // Always read-only, managed by HR/IT systems
    // ============================================

    @Column(name = "legal_first_name", nullable = false, length = 100)
    private String legalFirstName;

    @Column(name = "legal_last_name", nullable = false, length = 100)
    private String legalLastName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "job_code", length = 50)
    private String jobCode;

    @Column(name = "job_family", length = 100)
    private String jobFamily;

    @Column(name = "job_level", length = 50)
    private String jobLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "fte", precision = 3, scale = 2)
    private BigDecimal fte; // Full-Time Equivalent (e.g., 1.00 = 100%, 0.50 = 50%)

    // ============================================
    // NON-SENSITIVE FIELDS
    // Visible to everyone, editable by employee + manager
    // ============================================

    @Column(name = "preferred_name", length = 100)
    private String preferredName;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "office_location", length = 200)
    private String officeLocation;

    @Column(name = "work_phone", length = 20)
    private String workPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_location_type", length = 20)
    private WorkLocationType workLocationType;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills; // Stored as comma-separated or JSON string

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    // ============================================
    // SENSITIVE FIELDS
    // Visible to employee + manager only, editable by employee only
    // ============================================

    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Column(name = "personal_phone", length = 20)
    private String personalPhone;

    @Column(name = "home_address", columnDefinition = "TEXT")
    private String homeAddress;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "visa_work_permit", length = 200)
    private String visaWorkPermit;

    @Column(name = "absence_balance_days", precision = 5, scale = 2)
    private BigDecimal absenceBalanceDays;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "performance_rating", length = 50)
    private String performanceRating;

    // ============================================
    // AUDIT FIELDS
    // ============================================

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
