package com.rsvpplaner.repository;

import com.rsvpplaner.repository.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rsvplaner.v1.model.EventType;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    @Query("""
            SELECT e
            FROM Event e
                LEFT JOIN e.eventParticipants ep
            WHERE ep.email = ?1
            AND e.eventType = ?2
            """)
    Page<Event> findAllByExampleAndParticipantEmail(String email, EventType evenType,
            Pageable pageable);
}
