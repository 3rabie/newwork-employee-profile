package com.newwork.employee.dto.request;

import com.newwork.employee.entity.enums.AbsenceType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAbsenceRequest(
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate endDate,
        @NotNull AbsenceType type,
        @Size(max = 500) String note
) {}
