package com.rsvpplaner.repository;

import com.rsvpplaner.repository.model.EventParticipant;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, String> {

    @Query(
            value =
                    """
                            SELECT ep.*
                            FROM event_participant ep
                            WHERE ep.event_id = ?1 AND ep.email = ?2
                            """,
            nativeQuery = true)
    Optional<EventParticipant> findByEventIdAndEmail(String eventId, String email);

    @Modifying
    @Transactional
    @Query(
            value =
                    """
                            UPDATE event_participant ep
                            SET notify = ?3
                            WHERE ep.event_id = ?1 AND ep.email = ?2
                            """,
            nativeQuery = true)
    void updateNotify(String eventId, String email, boolean notify);

    @Query(
            value =
                    """
                            SELECT EXISTS(
                                SELECT 1
                                FROM event_participant ep
                                WHERE ep.event_id = ?1 AND ep.email = ?2
                            )
                            """,
            nativeQuery = true)
    boolean existsByEventIdAndEmail(String eventId, String email);
}
