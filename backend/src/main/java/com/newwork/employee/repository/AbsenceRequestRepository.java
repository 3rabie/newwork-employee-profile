package com.newwork.employee.repository;

import com.newwork.employee.entity.AbsenceRequest;
import com.newwork.employee.entity.enums.AbsenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, UUID> {

    @Query("""
            select ar from AbsenceRequest ar
            join fetch ar.user u
            left join fetch ar.manager m
            where ar.id = :id
            """)
    Optional<AbsenceRequest> findByIdWithUserAndManager(@Param("id") UUID id);

    @Query("""
            select ar from AbsenceRequest ar
            join fetch ar.user u
            where u.id = :userId
            order by ar.startDate desc
            """)
    List<AbsenceRequest> findAllByUserId(@Param("userId") UUID userId);

    @Query("""
            select ar from AbsenceRequest ar
            join fetch ar.user u
            where ar.manager.id = :managerId
              and ar.status = :status
            order by ar.startDate asc
            """)
    List<AbsenceRequest> findByManagerIdAndStatus(
            @Param("managerId") UUID managerId,
            @Param("status") AbsenceStatus status);
}
