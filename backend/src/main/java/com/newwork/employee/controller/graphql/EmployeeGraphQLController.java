package com.newwork.employee.controller.graphql;

import com.newwork.employee.dto.EmployeeAbsenceDTO;
import com.newwork.employee.dto.CoworkerDTO;
import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.security.AuthenticatedUser;
import com.newwork.employee.security.AuthenticatedUserAccessor;
import com.newwork.employee.service.AbsenceService;
import com.newwork.employee.service.DirectoryService;
import com.newwork.employee.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL controller aggregating employee-related queries (profile, directory, absences).
 * Also handles User.profile field resolution with permission-based filtering.
 */
@Controller
@RequiredArgsConstructor
public class EmployeeGraphQLController {

    private final ProfileService profileService;
    private final DirectoryService directoryService;
    private final AbsenceService absenceService;

    @QueryMapping
    public ProfileDTO profile(
            @Argument UUID userId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return profileService.getProfile(authenticatedUser.getUserId(), userId);
    }

    @QueryMapping
    public List<CoworkerDTO> coworkerDirectory(
            @Argument String search,
            @Argument String department,
            @Argument Boolean directReportsOnly,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return directoryService.getDirectory(authenticatedUser.getUserId(), search, department, directReportsOnly);
    }

    @QueryMapping
    public List<EmployeeAbsenceDTO> myAbsenceRequests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return absenceService.getMyRequests(authenticatedUser.getUserId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<EmployeeAbsenceDTO> pendingAbsenceRequests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return absenceService.getPendingForManager(authenticatedUser.getUserId());
    }

    @SchemaMapping(typeName = "User", field = "profile")
    public CompletableFuture<ProfileDTO> userProfile(User user, org.dataloader.DataLoader<UUID, EmployeeProfile> loader) {
        UUID viewerId = AuthenticatedUserAccessor.currentUserId();
        return loader.load(user.getId())
            .thenApply(profile -> profileService.toProfileDtoForViewer(profile, viewerId, user.getId()));
    }

}
