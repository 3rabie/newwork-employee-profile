package com.newwork.employee.service;

import com.newwork.employee.dto.CoworkerDTO;

import java.util.List;
import java.util.UUID;

/**
 * Directory service that exposes coworker listings scoped to the authenticated user.
 */
public interface DirectoryService {

    /**
     * Fetch coworkers/direct reports visible to the viewer.
     *
     * @param viewerId   authenticated user ID
     * @param searchTerm optional search term (name, email, employeeId, department)
     * @param department optional department filter
     * @param directReportsOnly limit results to direct reports of the viewer (manager-only)
     * @return ordered list of coworker DTOs
     */
    List<CoworkerDTO> getDirectory(UUID viewerId, String searchTerm, String department, Boolean directReportsOnly);
}
