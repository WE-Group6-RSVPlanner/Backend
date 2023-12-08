package com.rsvpplaner.repository;

import com.rsvpplaner.repository.model.EventParticipantAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventParticipantAvailabilityRepository extends
        JpaRepository<EventParticipantAvailability, String> {
}
