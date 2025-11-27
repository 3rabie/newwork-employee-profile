package com.newwork.employee.controller;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.dto.ProfileUpdateDTO;
import com.newwork.employee.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for employee profile management.
 * Provides endpoints for viewing and updating employee profiles with permission-based access control.
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "APIs for managing employee profiles")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Get employee profile by user ID.
     * Fields are filtered based on the authenticated user's permissions.
     *
     * @param userDetails authenticated user details (username contains user UUID)
     * @param userId the ID of the user whose profile to retrieve
     * @return ProfileDTO with fields filtered by permissions
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "Get employee profile",
            description = "Retrieve employee profile by user ID. Fields are filtered based on viewer's relationship to the profile owner."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProfileDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profile not found"
            )
    })
    public ResponseEntity<ProfileDTO> getProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId
    ) {
        UUID viewerId = UUID.fromString(userDetails.getUsername());
        ProfileDTO profile = profileService.getProfile(viewerId, userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update employee profile.
     * Only updates fields the authenticated user has permission to edit.
     *
     * @param userDetails authenticated user details (username contains user UUID)
     * @param userId the ID of the user whose profile to update
     * @param updateDTO the profile update data
     * @return Updated ProfileDTO with fields filtered by permissions
     */
    @PatchMapping("/{userId}")
    @Operation(
            summary = "Update employee profile",
            description = "Update employee profile. Only fields the user has permission to edit will be updated. " +
                    "NON_SENSITIVE fields can be edited by SELF and MANAGER (for direct reports). " +
                    "SENSITIVE fields can only be edited by SELF."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ProfileDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input - Validation failed"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User lacks permission to edit this profile"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profile not found"
            )
    })
    public ResponseEntity<ProfileDTO> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId,
            @Valid @RequestBody ProfileUpdateDTO updateDTO
    ) {
        UUID viewerId = UUID.fromString(userDetails.getUsername());
        ProfileDTO updated = profileService.updateProfile(viewerId, userId, updateDTO);
        return ResponseEntity.ok(updated);
    }
}
