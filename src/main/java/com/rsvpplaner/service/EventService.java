package com.rsvpplaner.service;

import com.rsvpplaner.repository.EventParticipantAvailabilityRepository;
import com.rsvpplaner.repository.EventParticipantRepository;
import com.rsvpplaner.repository.EventRepository;
import com.rsvpplaner.repository.model.EventParticipant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rsvplaner.v1.model.Attendee;
import rsvplaner.v1.model.Event;
import rsvplaner.v1.model.NewEvent;
import rsvplaner.v1.model.Organizer;

@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventParticipantAvailabilityRepository availabilityRepository;

    public EventService(EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            EventParticipantAvailabilityRepository availabilityRepository) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.availabilityRepository = availabilityRepository;
    }

    public Event createEvent(NewEvent newEvent) {
        var savedEvent = eventRepository.save(mapToDbEvent(newEvent));
        return mapToApiEvent(savedEvent);
    }

    public void addEventAttendee(String eventId, Attendee attendee) {
        var event = eventRepository.findById(eventId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, String.format("Event with id %s not found", eventId)));

        var eventParticipant = EventParticipant.builder()
                .email(attendee.getEmail())
                .name(attendee.getName())
                .participantType(EventParticipant.ParticipantType.ATTENDEE)
                .event(event)
                .build();
        event.getEventParticipants().add(eventParticipant);
        eventRepository.save(event);
    }

    private com.rsvpplaner.repository.model.Event mapToDbEvent(NewEvent event) {
        return new com.rsvpplaner.repository.model.Event(UUID.randomUUID().toString(),
                event.getTitle(),
                event.getDescription(), event.getLocation(),
                event.getOrganizer().getEmail(), event.getOrganizer().getName(),
                new ArrayList<>());
    }

    private Event mapToApiEvent(com.rsvpplaner.repository.model.Event event) {
        return new Event()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .organizer(new Organizer()
                        .name(event.getOrganizerName()).email(event.getOrganizerEmail()))
                .attendees(List.of());
    }

    private Attendee mapToApiAttendee(com.rsvpplaner.repository.model.EventParticipant attendee) {
        return new Attendee()
                .email(attendee.getEmail())
                .name(attendee.getName())
                .attendeeAvailabilities(List.of());
    }

    private Organizer mapToApiOrganizer(
            com.rsvpplaner.repository.model.EventParticipant organizer) {
        return new Organizer()
                .email(organizer.getEmail())
                .name(organizer.getName());
    }


}
