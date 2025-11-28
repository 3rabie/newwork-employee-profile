package com.newwork.employee.repository;

import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EmployeeProfile entity.
 * Provides methods for querying employee profiles.
 */
@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, UUID> {

    /**
     * Find employee profile by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the profile if found
     */
    Optional<EmployeeProfile> findByUserId(UUID userId);

    /**
     * Find all employee profiles by user IDs (for batch loading).
     *
     * @param userIds list of user IDs
     * @return list of profiles matching the user IDs
     */
    @Query("SELECT p FROM EmployeeProfile p WHERE p.user.id IN :userIds")
    List<EmployeeProfile> findAllByUserIdIn(@Param("userIds") List<UUID> userIds);

    /**
     * Find all profiles with a specific employment status.
     *
     * @param status the employment status
     * @return list of profiles with the given status
     */
    List<EmployeeProfile> findByEmploymentStatus(EmploymentStatus status);

    /**
     * Find all active employee profiles.
     *
     * @return list of active profiles
     */
    @Query("SELECT p FROM EmployeeProfile p WHERE p.employmentStatus = 'ACTIVE'")
    List<EmployeeProfile> findAllActiveProfiles();

    /**
     * Find profiles by department.
     *
     * @param department the department name
     * @return list of profiles in the department
     */
    List<EmployeeProfile> findByDepartment(String department);

    /**
     * Check if a profile exists for a given user ID.
     *
     * @param userId the user ID
     * @return true if profile exists, false otherwise
     */
    boolean existsByUserId(UUID userId);

    /**
     * Find profiles managed by a specific manager (direct reports).
     *
     * @param managerId the manager's user ID
     * @return list of profiles for direct reports
     */
    @Query("SELECT p FROM EmployeeProfile p WHERE p.user.manager.id = :managerId")
    List<EmployeeProfile> findByManagerId(@Param("managerId") UUID managerId);

    /**
     * Delete profile by user ID.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);
}
