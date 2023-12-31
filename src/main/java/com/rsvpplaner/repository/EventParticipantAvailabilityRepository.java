package com.rsvpplaner.repository;

import com.rsvpplaner.repository.model.Event;
import com.rsvpplaner.repository.model.EventParticipant;
import com.rsvpplaner.repository.model.EventParticipantAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rsvplaner.v1.model.AttendeeAvailability;

@Repository
public interface EventParticipantAvailabilityRepository extends
        JpaRepository<EventParticipantAvailability, String> {

    @Query
    @Modifying
    void deleteByEventAndParticipant(Event event, EventParticipant participant);

    @Query(value = """
            SELECT DISTINCT count(epa.id)
            FROM EventParticipantAvailability epa
            WHERE epa.event = :event AND epa.status = :status
            """)
    Long countDistinctAvailableParticipants(Event event, AttendeeAvailability.StatusEnum status);

}
