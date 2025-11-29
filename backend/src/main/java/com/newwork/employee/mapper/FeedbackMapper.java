package com.newwork.employee.mapper;

import com.newwork.employee.dto.FeedbackDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Feedback entity and FeedbackDTO.
 */
@Component
@RequiredArgsConstructor
public class FeedbackMapper {

    private final EmployeeProfileRepository profileRepository;

    /**
     * Convert Feedback entity to FeedbackDTO.
     *
     * @param feedback The feedback entity to convert
     * @return FeedbackDTO with all feedback information
     */
    public FeedbackDTO toDTO(Feedback feedback) {
        if (feedback == null) {
            return null;
        }

        FeedbackDTO dto = new FeedbackDTO();
        dto.setId(feedback.getId());
        dto.setAuthorId(feedback.getAuthor().getId());
        dto.setAuthorName(getDisplayName(feedback.getAuthor()));
        dto.setRecipientId(feedback.getRecipient().getId());
        dto.setRecipientName(getDisplayName(feedback.getRecipient()));
        dto.setText(feedback.getText());
        dto.setAiPolished(feedback.getAiPolished());
        dto.setCreatedAt(DateTimeUtil.toOffset(feedback.getCreatedAt()));

        return dto;
    }

    /**
     * Get display name for a user (preferred name or legal name).
     *
     * @param user The user entity
     * @return Display name
     */
    private String getDisplayName(User user) {
        EmployeeProfile profile = profileRepository.findByUserId(user.getId())
                .orElse(null);

        if (profile == null) {
            return "Unknown User";
        }

        if (profile.getPreferredName() != null && !profile.getPreferredName().isBlank()) {
            return profile.getPreferredName();
        }
        return profile.getLegalFirstName() + " " + profile.getLegalLastName();
    }

}
