package com.rsvpplaner.controller;

import com.rsvpplaner.service.EventService;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import rsvplaner.v1.api.EventApi;
import rsvplaner.v1.model.Attendee;
import rsvplaner.v1.model.Event;
import rsvplaner.v1.model.NewEvent;

@Controller
public class EventController implements EventApi {
    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public ResponseEntity<Event> createEvent(NewEvent newEvent) {
        if (newEvent == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body must be set");
        }

        if (StringUtils.isBlank(newEvent.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must be set");
        }

        if (newEvent.getOrganizer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizer must be set");
        }

        if (newEvent.getOrganizer().getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "organizer email must be set");
        }

        if (newEvent.getOrganizer().getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizer name must be set");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(newEvent));
    }

    @Override
    public ResponseEntity<Event> getEvent(String eventId) {
        return null;
    }

    @Override
    public ResponseEntity<List<Event>> getEvents(String eventType, String organizer,
            OffsetDateTime startDate, OffsetDateTime endDate) {
        return null;
    }

    @Override
    public ResponseEntity<Void> uploadEventImage(String eventId, Resource body) {
        return null;
    }

    @Override
    public ResponseEntity<Void> addEventAttendee(String eventId, Attendee attendee) {
        eventService.addEventAttendee(eventId, attendee);
        return ResponseEntity.noContent().build();
    }
}
