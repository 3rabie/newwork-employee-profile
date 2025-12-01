package com.newwork.employee.service.impl;

import com.newwork.employee.dto.AbsenceRequestDTO;
import com.newwork.employee.dto.request.CreateAbsenceRequest;
import com.newwork.employee.dto.request.UpdateAbsenceStatusRequest;
import com.newwork.employee.entity.AbsenceRequest;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.AbsenceStatus;
import com.newwork.employee.entity.enums.AbsenceType;
import com.newwork.employee.repository.AbsenceRequestRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.AbsenceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AbsenceServiceImpl implements AbsenceService {

    private final AbsenceRequestRepository absenceRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AbsenceRequestDTO submit(UUID requesterId, CreateAbsenceRequest request) {
        validateDates(request.startDate(), request.endDate(), request.type());
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        User manager = requester.getManager();

        AbsenceRequest entity = AbsenceRequest.builder()
                .user(requester)
                .manager(manager)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .type(request.type())
                .status(AbsenceStatus.PENDING)
                .note(request.note())
                .build();

        return toDto(absenceRequestRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceRequestDTO> getMyRequests(UUID requesterId) {
        return absenceRequestRepository.findAllByUserId(requesterId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbsenceRequestDTO> getPendingForManager(UUID managerId) {
        return absenceRequestRepository.findByManagerIdAndStatus(managerId, AbsenceStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AbsenceRequestDTO updateStatus(UUID managerId, UUID requestId, UpdateAbsenceStatusRequest update) {
        AbsenceRequest request = absenceRequestRepository.findByIdWithUserAndManager(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Absence request not found"));
        ensureManager(managerId, request);
        if (request.getStatus() != AbsenceStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be updated");
        }

        AbsenceStatus newStatus = parseAction(update.action());
        request.setStatus(newStatus);
        if (newStatus == AbsenceStatus.REJECTED) {
            request.setNote(update.note());
        }
        return toDto(absenceRequestRepository.save(request));
    }

    private void ensureManager(UUID managerId, AbsenceRequest request) {
        if (request.getManager() == null || !request.getManager().getId().equals(managerId)) {
            throw new AccessDeniedException("Only the manager can act on this request");
        }
    }

    private AbsenceStatus parseAction(String action) {
        if (action == null) {
            throw new IllegalArgumentException("Action is required");
        }
        return switch (action.toUpperCase()) {
            case "APPROVE" -> AbsenceStatus.APPROVED;
            case "REJECT" -> AbsenceStatus.REJECTED;
            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        };
    }

    @Override
    @Transactional
    public int completeExpiredApproved(LocalDate asOfDate) {
        List<AbsenceRequest> toComplete = absenceRequestRepository.findByStatusAndEndDateBefore(
                AbsenceStatus.APPROVED, asOfDate);
        toComplete.forEach(ar -> ar.setStatus(AbsenceStatus.COMPLETED));
        absenceRequestRepository.saveAll(toComplete);
        return toComplete.size();
    }

    private void validateDates(LocalDate start, LocalDate end, AbsenceType type) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        LocalDate today = LocalDate.now();
        if (type != AbsenceType.SICK) {
            if (start.isBefore(today)) {
                throw new IllegalArgumentException("Start date must be today or later for this type");
            }
            if (end.isBefore(today)) {
                throw new IllegalArgumentException("End date must be today or later for this type");
            }
        }
    }

    private AbsenceRequestDTO toDto(AbsenceRequest entity) {
        return AbsenceRequestDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .managerId(entity.getManager() != null ? entity.getManager().getId() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .type(entity.getType())
                .status(entity.getStatus())
                .note(entity.getNote())
                .build();
    }
}
