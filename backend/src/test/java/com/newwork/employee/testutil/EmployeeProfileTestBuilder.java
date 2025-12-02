package com.newwork.employee.testutil;

import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.WorkLocationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Test builder for creating EmployeeProfile test data with sensible defaults.
 * Provides a fluent API for customizing test profiles.
 */
public class EmployeeProfileTestBuilder {

    private UUID id = UUID.randomUUID();
    private User user = UserTestBuilder.aUser().build();
    private String legalFirstName = "John";
    private String legalLastName = "Doe";
    private String preferredName = "Johnny";
    private String department = "Engineering";
    private String jobCode = "ENG-001";
    private String jobFamily = "Engineering";
    private String jobLevel = "L3";
    private String jobTitle = "Software Engineer";
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;
    private LocalDate hireDate = LocalDate.of(2020, 1, 1);
    private LocalDate terminationDate;
    private BigDecimal fte = new BigDecimal("1.00");
    private String officeLocation = "San Francisco";
    private String workPhone = "+1-555-0100";
    private WorkLocationType workLocationType = WorkLocationType.HYBRID;
    private String bio = "Experienced software engineer";
    private String skills = "Java,Spring,PostgreSQL";
    private String profilePhotoUrl;
    private String personalEmail = "john.doe@personal.com";
    private String personalPhone = "+1-555-0101";
    private String homeAddress = "123 Main St, SF, CA";
    private String emergencyContactName = "Jane Doe";
    private String emergencyContactPhone = "+1-555-0102";
    private String emergencyContactRelationship = "Spouse";
    private LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
    private String visaWorkPermit;
    private BigDecimal absenceBalanceDays = new BigDecimal("20");
    private BigDecimal salary = new BigDecimal("90000.00");
    private String performanceRating = "Exceeds Expectations";

    public static EmployeeProfileTestBuilder aProfile() {
        return new EmployeeProfileTestBuilder();
    }

    public static EmployeeProfileTestBuilder aProfileFor(User user) {
        return new EmployeeProfileTestBuilder().withUser(user);
    }

    public EmployeeProfileTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public EmployeeProfileTestBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public EmployeeProfileTestBuilder withLegalFirstName(String legalFirstName) {
        this.legalFirstName = legalFirstName;
        return this;
    }

    public EmployeeProfileTestBuilder withLegalLastName(String legalLastName) {
        this.legalLastName = legalLastName;
        return this;
    }

    public EmployeeProfileTestBuilder withPreferredName(String preferredName) {
        this.preferredName = preferredName;
        return this;
    }

    public EmployeeProfileTestBuilder withDepartment(String department) {
        this.department = department;
        return this;
    }

    public EmployeeProfileTestBuilder withJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
        return this;
    }

    public EmployeeProfileTestBuilder withEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
        return this;
    }

    public EmployeeProfileTestBuilder withHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
        return this;
    }

    public EmployeeProfileTestBuilder withSalary(BigDecimal salary) {
        this.salary = salary;
        return this;
    }

    public EmployeeProfileTestBuilder withBio(String bio) {
        this.bio = bio;
        return this;
    }

    public EmployeeProfileTestBuilder withPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
        return this;
    }

    public EmployeeProfile build() {
        return EmployeeProfile.builder()
                .id(id)
                .user(user)
                .legalFirstName(legalFirstName)
                .legalLastName(legalLastName)
                .preferredName(preferredName)
                .department(department)
                .jobCode(jobCode)
                .jobFamily(jobFamily)
                .jobLevel(jobLevel)
                .jobTitle(jobTitle)
                .employmentStatus(employmentStatus)
                .hireDate(hireDate)
                .terminationDate(terminationDate)
                .fte(fte)
                .officeLocation(officeLocation)
                .workPhone(workPhone)
                .workLocationType(workLocationType)
                .bio(bio)
                .skills(skills)
                .profilePhotoUrl(profilePhotoUrl)
                .personalEmail(personalEmail)
                .personalPhone(personalPhone)
                .homeAddress(homeAddress)
                .emergencyContactName(emergencyContactName)
                .emergencyContactPhone(emergencyContactPhone)
                .emergencyContactRelationship(emergencyContactRelationship)
                .dateOfBirth(dateOfBirth)
                .visaWorkPermit(visaWorkPermit)
                .absenceBalanceDays(absenceBalanceDays)
                .salary(salary)
                .performanceRating(performanceRating)
                .build();
    }
}
