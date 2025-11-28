package com.newwork.employee.repository;

import com.newwork.employee.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Feedback entity with visibility queries.
 *
 * Visibility rules:
 * - Authors can see feedback they wrote
 * - Recipients can see feedback written about them
 * - Managers can see feedback about their direct reports
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    /**
     * Find all feedback written by a specific author.
     *
     * @param authorId The UUID of the author
     * @return List of feedback written by the author
     */
    List<Feedback> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    /**
     * Find all feedback received by a specific recipient.
     *
     * @param recipientId The UUID of the recipient
     * @return List of feedback received by the recipient
     */
    List<Feedback> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    /**
     * Find all feedback visible to a viewer about a specific user.
     * Includes feedback where the viewer is:
     * - The author (feedback they wrote)
     * - The recipient (feedback written about them)
     * - The manager of the recipient (feedback about their direct reports)
     *
     * @param viewerId The UUID of the viewer
     * @param userId The UUID of the user whose feedback to retrieve
     * @return List of feedback visible to the viewer about the user
     */
    @Query("""
        SELECT f FROM Feedback f
        JOIN f.recipient r
        LEFT JOIN r.manager m
        WHERE r.id = :userId
        AND (
            f.author.id = :viewerId
            OR r.id = :viewerId
            OR m.id = :viewerId
        )
        ORDER BY f.createdAt DESC
        """)
    List<Feedback> findVisibleFeedbackForUser(
        @Param("viewerId") UUID viewerId,
        @Param("userId") UUID userId
    );

    /**
     * Find all feedback written by a specific author about a specific recipient.
     *
     * @param authorId The UUID of the author
     * @param recipientId The UUID of the recipient
     * @return List of feedback from author to recipient
     */
    List<Feedback> findByAuthorIdAndRecipientIdOrderByCreatedAtDesc(UUID authorId, UUID recipientId);

    /**
     * Count feedback received by a specific user.
     *
     * @param recipientId The UUID of the recipient
     * @return Count of feedback items
     */
    long countByRecipientId(UUID recipientId);

    /**
     * Count feedback written by a specific user.
     *
     * @param authorId The UUID of the author
     * @return Count of feedback items
     */
    long countByAuthorId(UUID authorId);
}
