package com.newwork.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record UpdateAbsenceStatusRequest(
        @NotBlank(message = "action is required")
        @Pattern(regexp = "APPROVE|REJECT", flags = Pattern.Flag.CASE_INSENSITIVE, message = "action must be APPROVE or REJECT")
        String action,
        String note
) {
}
