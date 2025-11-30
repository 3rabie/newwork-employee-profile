package com.newwork.employee.service;

import com.newwork.employee.dto.AbsenceRequestDTO;
import com.newwork.employee.dto.request.CreateAbsenceRequest;
import com.newwork.employee.dto.request.UpdateAbsenceStatusRequest;

import java.util.List;
import java.util.UUID;

public interface AbsenceService {

    AbsenceRequestDTO submit(UUID requesterId, CreateAbsenceRequest request);

    List<AbsenceRequestDTO> getMyRequests(UUID requesterId);

    List<AbsenceRequestDTO> getPendingForManager(UUID managerId);

    AbsenceRequestDTO updateStatus(UUID managerId, UUID requestId, UpdateAbsenceStatusRequest update);
}
