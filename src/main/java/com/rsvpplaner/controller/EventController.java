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
import rsvplaner.v1.api.EventApi;
import rsvplaner.v1.model.Attendee;
import rsvplaner.v1.model.AttendeeAvailability;
import rsvplaner.v1.model.Event;
import rsvplaner.v1.model.EventType;
import rsvplaner.v1.model.InvitedPerson;
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
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, "request body must be set");
        }

        if (StringUtils.isBlank(newEvent.getTitle())) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, "title must be set");
        }

        if (newEvent.getOrganizer() == null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, "organizer must be set");
        }

        if (newEvent.getOrganizer().getEmail() == null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "organizer email must be set");
        }

        if (newEvent.getOrganizer().getName() == null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, "organizer name must be set");
        }

        if (newEvent.getPossibleDateTimes() == null || newEvent.getPossibleDateTimes().isEmpty()) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "at least one possible date time must be given");
        }

        if (newEvent.getEventType() == EventType.PUBLIC
            && newEvent.getPossibleDateTimes().size() != 1) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "public events must have exactly one possible date time");
        }

        if (newEvent.getEventType() == EventType.PUBLIC
                && newEvent.getInvitedPeople() != null
                && !newEvent.getInvitedPeople().isEmpty()) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, "public events cannot have invited people");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(newEvent));
    }

    @Override
    public ResponseEntity<Event> getEvent(String eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @Override
    public ResponseEntity<List<Event>> findEvents(Integer pageNumber, Integer pageSize,
            EventType eventType, String title, String organizerEmail, String attendeeEmail,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {

        if (pageNumber == null || pageSize == null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "page offset and page size must be set");
        }

        if (eventType == null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "event type is not set");
        }

        if (startDate != null && endDate == null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "end date must be set if start date is set");
        }

        if (startDate == null && endDate != null) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "start date must be set if end date is set");
        }

        if (startDate != null && startDate.isAfter(endDate)) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                    "start date must be before end date");
        }

        return ResponseEntity.ok(
                eventService.findEvents(pageNumber, pageSize,
                        eventType, title, attendeeEmail, organizerEmail,
                        startDate != null ? startDate.toInstant() : null,
                        endDate != null ? endDate.toInstant() : null));
    }

    @Override
    public ResponseEntity<Void> uploadEventImage(String eventId, Resource body) {
        eventService.uploadImage(eventId, body);
        return null;
    }

    @Override
    public ResponseEntity<Event> addEventAttendee(String eventId, Attendee attendee) {
        return ResponseEntity.ok(eventService.addEventAttendee(eventId, attendee));
    }

    @Override
    public ResponseEntity<Resource> getEventImage(String eventId) {
        return ResponseEntity.ok(eventService.getEventImage(eventId));
    }

    @Override
    public ResponseEntity<Event> updateAttendeeAvailability(String eventId, String attendeeEmail,
            List<AttendeeAvailability> attendeeAvailability) {
        return ResponseEntity.ok(
                eventService.updateAttendeeAvailability(eventId, attendeeEmail,
                        attendeeAvailability));
    }

    @Override
    public ResponseEntity<Event> deleteAttendeeAvailability(String eventId, String attendeeEmail) {
        return ResponseEntity.ok(
                eventService.deleteAttendee(eventId, attendeeEmail));
    }

    @Override
    public ResponseEntity<Attendee> getAttendee(String eventId, String attendeeEmail) {
        return ResponseEntity.ok(eventService.getAttendee(eventId, attendeeEmail));
    }

    @Override
    public ResponseEntity<Void> updateAttendeeNotification(String eventId, String attendeeEmail, Boolean body) {
        eventService.updateAttendeeNotification(eventId, attendeeEmail, body);
        return ResponseEntity.noContent().build();
    }
}
