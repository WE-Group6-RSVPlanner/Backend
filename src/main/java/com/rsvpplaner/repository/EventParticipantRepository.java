package com.rsvpplaner.repository;

import com.rsvpplaner.repository.model.Event;
import com.rsvpplaner.repository.model.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, String> {

    @Query
    Long countByEvent(Event event);
}
