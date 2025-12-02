package com.newwork.employee.service.impl;

import com.newwork.employee.dto.CoworkerDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.AbsenceStatus;
import com.newwork.employee.entity.enums.Relationship;
import com.newwork.employee.exception.UserNotFoundException;
import com.newwork.employee.repository.EmployeeAbsenceRepository;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.DirectoryService;
import com.newwork.employee.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectoryServiceImpl implements DirectoryService {

    private final EmployeeProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final EmployeeAbsenceRepository absenceRequestRepository;
    private final PermissionService permissionService;

    @Override
    @Transactional(readOnly = true)
    public List<CoworkerDTO> getDirectory(UUID viewerId, String searchTerm, String department, Boolean directReportsOnly) {
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new UserNotFoundException("Viewer not found with id: " + viewerId));

        String normalizedSearch = normalize(searchTerm);
        String normalizedDepartment = normalize(department);
        boolean onlyDirectReports = Boolean.TRUE.equals(directReportsOnly);

        List<EmployeeProfile> profiles = profileRepository.findAllActiveProfilesWithUserAndManager();
        log.debug("Loaded {} active profiles for directory listing", profiles.size());

        return profiles.stream()
                .filter(profile -> !profile.getUser().getId().equals(viewerId))
                .filter(profile -> matchesSearch(profile, normalizedSearch))
                .filter(profile -> matchesDepartment(profile, normalizedDepartment))
                .map(profile -> mapToDto(viewer, profile, onlyDirectReports))
                .filter(dto -> dto != null)
                .sorted(Comparator.comparing(CoworkerDTO::getPreferredName, DirectoryServiceImpl::compareNullableStrings)
                        .thenComparing(CoworkerDTO::getLegalFirstName, DirectoryServiceImpl::compareNullableStrings))
                .collect(Collectors.toList());
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private static int compareNullableStrings(String first, String second) {
        String safeFirst = first != null ? first.toLowerCase(Locale.ROOT) : "";
        String safeSecond = second != null ? second.toLowerCase(Locale.ROOT) : "";
        return safeFirst.compareTo(safeSecond);
    }

    private boolean matchesSearch(EmployeeProfile profile, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }

        String preferredName = normalize(profile.getPreferredName());
        String legalFullName = normalize(
                (profile.getLegalFirstName() + " " + profile.getLegalLastName()).trim());
        String email = normalize(profile.getUser().getEmail());
        String employeeId = normalize(profile.getUser().getEmployeeId());
        String department = normalize(profile.getDepartment());

        return (preferredName != null && preferredName.contains(search))
                || (legalFullName != null && legalFullName.contains(search))
                || (email != null && email.contains(search))
                || (employeeId != null && employeeId.contains(search))
                || (department != null && department.contains(search));
    }

    private boolean matchesDepartment(EmployeeProfile profile, String departmentFilter) {
        if (!StringUtils.hasText(departmentFilter)) {
            return true;
        }
        String profileDepartment = normalize(profile.getDepartment());
        return departmentFilter.equals(profileDepartment);
    }

    private CoworkerDTO mapToDto(User viewer, EmployeeProfile profile, boolean onlyDirectReports) {
        User profileOwner = profile.getUser();
        Relationship relationship = permissionService.determineRelationship(viewer, profileOwner);
        String relationshipLabel = relationship == Relationship.COWORKER ? "OTHER" : relationship.name();

        if (onlyDirectReports && relationship != Relationship.MANAGER) {
            return null;
        }

        Integer pendingAbsenceCount = null;
        if (relationship == Relationship.MANAGER) {
            long pending = absenceRequestRepository.countByManagerAndUserAndStatus(
                    viewer.getId(), profileOwner.getId(), AbsenceStatus.PENDING);
            pendingAbsenceCount = Math.toIntExact(pending);
        }

        return CoworkerDTO.builder()
                .userId(profileOwner.getId())
                .employeeId(profileOwner.getEmployeeId())
                .preferredName(StringUtils.hasText(profile.getPreferredName())
                        ? profile.getPreferredName()
                        : profile.getLegalFirstName())
                .legalFirstName(profile.getLegalFirstName())
                .legalLastName(profile.getLegalLastName())
                .jobTitle(profile.getJobTitle())
                .department(profile.getDepartment())
                .workLocationType(profile.getWorkLocationType() != null
                        ? profile.getWorkLocationType().name()
                        : null)
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .relationship(relationshipLabel)
                .directReport(relationship == Relationship.MANAGER)
                .pendingAbsenceCount(pendingAbsenceCount)
                .build();
    }
}
