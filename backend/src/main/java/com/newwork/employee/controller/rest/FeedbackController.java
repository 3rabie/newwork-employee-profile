package com.newwork.employee.controller.rest;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.dto.request.CreateFeedbackRequest;
import com.newwork.employee.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.UUID;

/**
 * REST controller for feedback operations.
 * Provides POST endpoint for creating feedback.
 * Query operations have been moved to GraphQL for better performance (see FeedbackGraphQLController).
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback Management", description = "REST API for creating peer feedback. Use GraphQL for queries.")
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
    @PostMapping
    @Operation(
            summary = "Create new feedback",
            description = "Create feedback from the authenticated user to another employee. " +
                    "Cannot give feedback to yourself. " +
                    "Note: Use GraphQL queries to retrieve feedback (see /graphql endpoint)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Feedback created successfully",
                    content = @Content(schema = @Schema(implementation = FeedbackDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input - Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot give feedback to yourself"),
            @ApiResponse(responseCode = "404", description = "Recipient not found")
    })
    public ResponseEntity<FeedbackDTO> createFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateFeedbackRequest request) {
        UUID authorId = UUID.fromString(userDetails.getUsername());
        FeedbackDTO feedback = feedbackService.createFeedback(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }
}
