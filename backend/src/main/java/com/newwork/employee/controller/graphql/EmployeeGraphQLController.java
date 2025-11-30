package com.newwork.employee.controller.graphql;

import com.newwork.employee.dto.CoworkerDTO;
import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.security.AuthenticatedUser;
import com.newwork.employee.service.DirectoryService;
import com.newwork.employee.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL controller aggregating employee-related queries (profile + directory).
 */
@Controller
@RequiredArgsConstructor
public class EmployeeGraphQLController {

    private final ProfileService profileService;
    private final DirectoryService directoryService;

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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return directoryService.getDirectory(authenticatedUser.getUserId(), search, department);
    }
}
