package com.newwork.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Metadata describing field visibility and editability for a profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileMetadataDTO {

    private String relationship;
    private List<String> visibleFields;
    private List<String> editableFields;
}
