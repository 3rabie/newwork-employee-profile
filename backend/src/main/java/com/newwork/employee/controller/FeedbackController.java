package com.newwork.employee.controller;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.dto.request.CreateFeedbackRequest;
import com.newwork.employee.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for feedback operations.
 * Provides endpoints for creating and viewing peer feedback with privacy controls.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Feedback Management", description = "APIs for giving and viewing peer feedback")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Create new feedback.
     * POST /api/feedback
     *
     * @param userDetails The authenticated user details (author)
     * @param request The feedback creation request
     * @return Created feedback DTO with 201 status
     */
    @PostMapping("/feedback")
    @Operation(
            summary = "Create new feedback",
            description = "Create feedback from the authenticated user to another employee. " +
                    "Cannot give feedback to yourself."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Feedback created successfully",
                    content = @Content(schema = @Schema(implementation = FeedbackDTO.class))
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
                    description = "Forbidden - Cannot give feedback to yourself"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Recipient not found"
            )
    })
    public ResponseEntity<FeedbackDTO> createFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateFeedbackRequest request) {
        UUID authorId = UUID.fromString(userDetails.getUsername());
        FeedbackDTO feedback = feedbackService.createFeedback(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    /**
     * Get feedback for a specific user profile.
     * GET /api/profiles/{id}/feedback
     *
     * Returns feedback visible to the current user according to visibility rules:
     * - Authors can see feedback they wrote
     * - Recipients can see feedback written about them
     * - Managers can see feedback about their direct reports
     *
     * @param userDetails The authenticated user details (viewer)
     * @param id The UUID of the user whose feedback to retrieve
     * @return List of visible feedback
     */
    @GetMapping("/profiles/{id}/feedback")
    @Operation(
            summary = "Get feedback for a user profile",
            description = "Retrieve feedback visible to the authenticated user about a specific employee. " +
                    "Visibility rules: authors see their feedback, recipients see feedback about them, " +
                    "managers see feedback about their direct reports."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Feedback retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FeedbackDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    public ResponseEntity<List<FeedbackDTO>> getFeedbackForProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID viewerId = UUID.fromString(userDetails.getUsername());
        List<FeedbackDTO> feedback = feedbackService.getFeedbackForUser(viewerId, id);
        return ResponseEntity.ok(feedback);
    }

    /**
     * Get all feedback written by the current user.
     * GET /api/feedback/authored
     *
     * @param userDetails The authenticated user details
     * @return List of feedback written by the user
     */
    @GetMapping("/feedback/authored")
    @Operation(
            summary = "Get feedback authored by current user",
            description = "Retrieve all feedback written by the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Feedback retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FeedbackDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            )
    })
    public ResponseEntity<List<FeedbackDTO>> getAuthoredFeedback(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID authorId = UUID.fromString(userDetails.getUsername());
        List<FeedbackDTO> feedback = feedbackService.getFeedbackByAuthor(authorId);
        return ResponseEntity.ok(feedback);
    }

    /**
     * Get all feedback received by the current user.
     * GET /api/feedback/received
     *
     * @param userDetails The authenticated user details
     * @return List of feedback received by the user
     */
    @GetMapping("/feedback/received")
    @Operation(
            summary = "Get feedback received by current user",
            description = "Retrieve all feedback received by the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Feedback retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FeedbackDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing authentication token"
            )
    })
    public ResponseEntity<List<FeedbackDTO>> getReceivedFeedback(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID recipientId = UUID.fromString(userDetails.getUsername());
        List<FeedbackDTO> feedback = feedbackService.getFeedbackByRecipient(recipientId);
        return ResponseEntity.ok(feedback);
    }
}
