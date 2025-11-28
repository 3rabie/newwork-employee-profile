package com.newwork.employee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feedback entity representing peer feedback between employees.
 *
 * Feedback can be given from one employee (author) to another (recipient).
 * The aiPolished flag indicates whether the feedback text was enhanced by AI.
 *
 * Visibility rules:
 * - Authors can see feedback they wrote
 * - Recipients can see feedback written about them
 * - Managers can see feedback about their direct reports
 */
@Entity
@Table(name = "feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * The employee who wrote the feedback
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * The employee who is receiving the feedback
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * The feedback text content
     */
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Flag indicating if the feedback was polished by AI
     */
    @Column(name = "ai_polished", nullable = false)
    private Boolean aiPolished = false;

    /**
     * Timestamp when the feedback was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (aiPolished == null) {
            aiPolished = false;
        }
    }
}
