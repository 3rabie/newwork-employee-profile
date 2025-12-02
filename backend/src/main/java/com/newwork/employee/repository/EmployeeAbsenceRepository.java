package com.newwork.employee.repository;

import com.newwork.employee.entity.EmployeeAbsence;
import com.newwork.employee.entity.enums.AbsenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAbsenceRepository extends JpaRepository<EmployeeAbsence, UUID> {

    @Query("""
            select ar from EmployeeAbsence ar
            join fetch ar.user u
            left join fetch ar.manager m
            where ar.id = :id
            """)
    Optional<EmployeeAbsence> findByIdWithUserAndManager(@Param("id") UUID id);

    @Query("""
            select ar from EmployeeAbsence ar
            join fetch ar.user u
            where u.id = :userId
            order by ar.startDate desc
            """)
    List<EmployeeAbsence> findAllByUserId(@Param("userId") UUID userId);

    @Query("""
            select ar from EmployeeAbsence ar
            join fetch ar.user u
            where ar.manager.id = :managerId
              and ar.status = :status
            order by ar.startDate asc
            """)
    List<EmployeeAbsence> findByManagerIdAndStatus(
            @Param("managerId") UUID managerId,
            @Param("status") AbsenceStatus status);

    @Query("""
            select count(ar) from EmployeeAbsence ar
            where ar.manager.id = :managerId
              and ar.user.id = :userId
              and ar.status = :status
            """)
    long countByManagerAndUserAndStatus(
            @Param("managerId") UUID managerId,
            @Param("userId") UUID userId,
            @Param("status") AbsenceStatus status);

    @Query("""
            select ar from EmployeeAbsence ar
            join fetch ar.user u
            left join fetch ar.manager m
            where ar.status = :status
              and ar.endDate < :beforeDate
            """)
    List<EmployeeAbsence> findByStatusAndEndDateBefore(
            @Param("status") AbsenceStatus status,
            @Param("beforeDate") LocalDate beforeDate);
}
