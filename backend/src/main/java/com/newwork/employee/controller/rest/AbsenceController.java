package com.newwork.employee.controller.rest;

import com.newwork.employee.dto.EmployeeAbsenceDTO;
import com.newwork.employee.dto.request.CreateAbsenceRequest;
import com.newwork.employee.dto.request.UpdateAbsenceStatusRequest;
import com.newwork.employee.security.AuthenticatedUser;
import com.newwork.employee.service.AbsenceService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/absence")
@RequiredArgsConstructor
@Tag(name = "Absence", description = "Submit and manage absence requests")
@SecurityRequirement(name = "bearerAuth")
public class AbsenceController {

    private final AbsenceService absenceService;

    @PostMapping
    @Operation(
            summary = "Submit a new absence request",
            description = "Create an absence request for the authenticated user. Supports vacation, sick, and other types."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = EmployeeAbsenceDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EmployeeAbsenceDTO> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateAbsenceRequest request
    ) {
        EmployeeAbsenceDTO created = absenceService.submit(user.getUserId(), request);
        return ResponseEntity.status(201).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update absence status (manager only)",
            description = "Managers can approve or reject a pending absence using action APPROVE or REJECT. Optional note is accepted for rejections."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request updated", content = @Content(schema = @Schema(implementation = EmployeeAbsenceDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid action or body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not a manager or not allowed to update this request"),
            @ApiResponse(responseCode = "404", description = "Request not found")
    })
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<EmployeeAbsenceDTO> updateStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAbsenceStatusRequest request
    ) {
        return ResponseEntity.ok(absenceService.updateStatus(user.getUserId(), id, request));
    }
}
