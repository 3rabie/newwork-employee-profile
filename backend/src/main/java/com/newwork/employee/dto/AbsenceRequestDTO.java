package com.newwork.employee.dto;

import com.newwork.employee.entity.enums.AbsenceStatus;
import com.newwork.employee.entity.enums.AbsenceType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class AbsenceRequestDTO {
    UUID id;
    UUID userId;
    UUID managerId;
    LocalDate startDate;
    LocalDate endDate;
    AbsenceType type;
    AbsenceStatus status;
    String note;
}
