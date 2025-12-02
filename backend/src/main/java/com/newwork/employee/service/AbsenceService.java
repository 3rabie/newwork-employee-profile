package com.newwork.employee.service;

import com.newwork.employee.dto.EmployeeAbsenceDTO;
import com.newwork.employee.dto.request.CreateAbsenceRequest;
import com.newwork.employee.dto.request.UpdateAbsenceStatusRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AbsenceService {

    EmployeeAbsenceDTO submit(UUID requesterId, CreateAbsenceRequest request);

    List<EmployeeAbsenceDTO> getMyRequests(UUID requesterId);

    List<EmployeeAbsenceDTO> getPendingForManager(UUID managerId);

    EmployeeAbsenceDTO updateStatus(UUID managerId, UUID requestId, UpdateAbsenceStatusRequest update);

    int completeExpiredApproved(LocalDate asOfDate);
}
