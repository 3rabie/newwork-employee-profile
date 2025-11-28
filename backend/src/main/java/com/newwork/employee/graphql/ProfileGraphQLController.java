package com.newwork.employee.graphql;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * GraphQL controller for employee profile queries.
 * Provides profile data with permission-based field filtering.
 */
@Controller
@RequiredArgsConstructor
public class ProfileGraphQLController {

    private final ProfileService profileService;

    /**
     * Get employee profile by user ID.
     * Fields are filtered based on the authenticated user's permissions.
     *
     * @param userId the ID of the user whose profile to retrieve
     * @param userDetails the authenticated user
     * @return ProfileDTO with fields filtered by permissions
     */
    @QueryMapping
    public ProfileDTO profile(
            @Argument UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID viewerId = UUID.fromString(userDetails.getUsername());
        return profileService.getProfile(viewerId, userId);
    }
}
