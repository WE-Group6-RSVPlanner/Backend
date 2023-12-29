package com.rsvpplaner.service;

import com.rsvpplaner.controller.ErrorResponseException;
import com.rsvpplaner.repository.EventParticipantAvailabilityRepository;
import com.rsvpplaner.repository.EventParticipantRepository;
import com.rsvpplaner.repository.EventRepository;
import com.rsvpplaner.repository.model.EventParticipant;
import com.rsvpplaner.repository.model.EventParticipantAvailability;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rsvplaner.v1.model.Attendee;
import rsvplaner.v1.model.Event;
import rsvplaner.v1.model.EventType;
import rsvplaner.v1.model.NewEvent;
import rsvplaner.v1.model.NewEventPossibleDateTimesInner;
import rsvplaner.v1.model.Organizer;

@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventParticipantAvailabilityRepository availabilityRepository;
    private final EntityManager entityManager;
    private final MinioClient minioClient;

    private final String imageBucketName;

    public EventService(EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            EventParticipantAvailabilityRepository availabilityRepository,
            EntityManager entityManager, MinioClient minioClient,
            @Value("${minio.bucket.eventimages}") String imageBucketName) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.availabilityRepository = availabilityRepository;
        this.entityManager = entityManager;
        this.minioClient = minioClient;
        this.imageBucketName = imageBucketName;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Event createEvent(NewEvent newEvent) {
        com.rsvpplaner.repository.model.Event event = mapToDbEvent(newEvent);

        var eventParticipant = EventParticipant.builder()
                .id(UUID.randomUUID().toString())
                .email(newEvent.getOrganizer().getEmail())
                .name(newEvent.getOrganizer().getName())
                .participantType(EventParticipant.ParticipantType.ORGANISER)
                .event(event)
                .build();

        var availabilities = newEvent.getPossibleDateTimes().stream().map(
                d -> mapAvailability(event, eventParticipant, d)).toList();
        eventParticipant.setAvailabilities(availabilities);
        event.setEventParticipants(List.of(eventParticipant));

        var savedEvent = eventRepository.save(event);
        return mapToApiEvent(savedEvent);
    }

    public void addEventAttendee(String eventId, Attendee attendee) {
        var event = eventRepository.findById(eventId).orElseThrow(() -> new ErrorResponseException(
                HttpStatus.NOT_FOUND, String.format("event with id %s not found", eventId)));

        event.getEventParticipants().stream().filter(p -> p.getEmail().equals(attendee.getEmail()))
                .findAny().ifPresent(p -> {
                    throw new ErrorResponseException(HttpStatus.CONFLICT,
                            String.format("attendee with email %s already exists",
                                    attendee.getEmail()));
                });

        var eventParticipant = EventParticipant.builder()
                .id(UUID.randomUUID().toString())
                .email(attendee.getEmail())
                .name(attendee.getName())
                .participantType(EventParticipant.ParticipantType.ATTENDEE)
                .event(event)
                .build();

        var availabilities = attendee.getAttendeeAvailabilities().stream().map(
                a -> EventParticipantAvailability.builder()
                        .id(UUID.randomUUID().toString())
                        .event(event)
                        .participant(eventParticipant)
                        .startTime(a.getStartDate().toInstant())
                        .endTime(a.getEndDate().toInstant())
                        .build()).toList();

        for (var a : availabilities) {
            if (event.getEventDates().stream().noneMatch(
                    d -> d.getStartTime().equals(a.getStartTime()) && d.getEndTime().equals(
                            a.getEndTime()))) {
                throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                        String.format(
                                "availability with start time: %s and end time: %s does not match"
                                + " any possible event date",
                                a.getStartTime().atOffset(
                                        ZoneOffset.UTC), a.getEndTime().atOffset(ZoneOffset.UTC)));
            }
        }

        eventParticipant.setAvailabilities(availabilities);
        eventParticipantRepository.save(eventParticipant);
    }

    public Event getEvent(String eventId) {
        var event = eventRepository.findById(eventId).orElseThrow(() -> new ErrorResponseException(
                HttpStatus.NOT_FOUND, String.format("Event with id %s not found", eventId)));

        return mapToApiEvent(event);
    }

    public List<Event> findEvents(int pageSize, int pageOffset, EventType eventType,
            String organizer,
            Instant startDate,
            Instant endDate) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(com.rsvpplaner.repository.model.Event.class);
        var root = query.from(com.rsvpplaner.repository.model.Event.class);

        var predicates = new ArrayList<>();
        if (organizer != null) {
            predicates.add(cb.equal(root.get("organizerEmail"), organizer));
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
        }

        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDate));
        }

        predicates.add(cb.equal(root.get("eventType"), eventType));

        var events = entityManager
                .createQuery(
                        query.select(root).where(predicates.toArray(new Predicate[0])))
                .setFirstResult(pageOffset).setMaxResults(pageSize)
                .getResultList();

        if (events.isEmpty()) {
            throw new ErrorResponseException(HttpStatus.NOT_FOUND, "no events found");
        }

        return events.stream().map(this::mapToApiEvent).toList();
    }


    private com.rsvpplaner.repository.model.Event mapToDbEvent(NewEvent newEvent) {
        var event = new com.rsvpplaner.repository.model.Event();
        event.setId(UUID.randomUUID().toString());
        event.setEventType(newEvent.getEventType());
        event.setTitle(newEvent.getTitle());
        event.setDescription(newEvent.getDescription());
        event.setLocation(newEvent.getLocation());
        event.setOrganizerEmail(newEvent.getOrganizer().getEmail());
        event.setOrganizerName(newEvent.getOrganizer().getName());
        event.setEventDates(newEvent.getPossibleDateTimes().stream().map(
                d -> com.rsvpplaner.repository.model.EventDate.builder()
                        .id(UUID.randomUUID().toString())
                        .event(event)
                        .startTime(d.getStartDate().toInstant())
                        .endTime(d.getEndDate().toInstant())
                        .build()).toList());
        return event;
    }

    private EventParticipantAvailability mapAvailability(
            com.rsvpplaner.repository.model.Event event,
            EventParticipant eventParticipant, NewEventPossibleDateTimesInner availability) {
        return EventParticipantAvailability.builder()
                .id(UUID.randomUUID().toString())
                .event(event)
                .participant(eventParticipant)
                .startTime(availability.getStartDate().toInstant())
                .endTime(availability.getEndDate().toInstant())
                .build();
    }

    private Event mapToApiEvent(com.rsvpplaner.repository.model.Event dbEvent) {
        Event event = new Event();
        event.setEventId(dbEvent.getId());
        event.setTitle(dbEvent.getTitle());
        event.setDescription(dbEvent.getDescription());
        event.setLocation(dbEvent.getLocation());
        event.setEventType(dbEvent.getEventType());
        event.setOrganizer(new Organizer().name(dbEvent.getOrganizerName())
                .email(dbEvent.getOrganizerEmail()));
        event.setAttendees(dbEvent.getEventParticipants().stream().map(
                p -> new Attendee().name(p.getName()).email(p.getEmail())).toList());
        event.setAttendeesCount(dbEvent.getEventParticipants().size());
        return event;
    }

    public void uploadImage(String eventId, Resource body) {
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(imageBucketName).object(
                    eventId).contentType(
                    "image/png").stream(body.getInputStream(), body.contentLength(), 1).build());

        } catch (IOException e) {
            throw new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to upload image");
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (io.minio.errors.ErrorResponseException e) {
            throw new RuntimeException(e);
        }
    }
}
