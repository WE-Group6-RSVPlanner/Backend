package com.rsvpplaner.service;

import static java.time.ZoneOffset.UTC;

import com.google.common.io.Files;
import com.rsvpplaner.controller.ErrorResponseException;
import com.rsvpplaner.repository.EventParticipantAvailabilityRepository;
import com.rsvpplaner.repository.EventParticipantRepository;
import com.rsvpplaner.repository.EventRepository;
import com.rsvpplaner.repository.model.EventParticipant;
import com.rsvpplaner.repository.model.EventParticipantAvailability;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import rsvplaner.v1.model.Attendee;
import rsvplaner.v1.model.AttendeeAvailability;
import rsvplaner.v1.model.Event;
import rsvplaner.v1.model.EventDateTimesInner;
import rsvplaner.v1.model.EventType;
import rsvplaner.v1.model.NewEvent;
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
                a -> {
                    if (a.getStartDate() == null || a.getEndDate() == null) {
                        throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                                "attendee availability start and end date must be set");
                    }

                    if (StringUtils.isBlank(a.getStatus().toString())) {
                        throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                                "attendee availability status must be set");
                    }

                    return EventParticipantAvailability.builder()
                            .id(UUID.randomUUID().toString())
                            .event(event)
                            .participant(eventParticipant)
                            .startTime(a.getStartDate().toInstant())
                            .endTime(a.getEndDate().toInstant())
                            .status(a.getStatus())
                            .build();
                }).toList();

        for (var a : availabilities) {
            if (event.getEventDates().stream().noneMatch(
                    d -> d.getStartTime().equals(a.getStartTime()) && d.getEndTime().equals(
                            a.getEndTime()))) {
                throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                        String.format(
                                "availability with start time: %s and end time: %s does not match"
                                + " any possible event date",
                                a.getStartTime().atOffset(
                                        UTC), a.getEndTime().atOffset(UTC)));
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

    public List<Event> findEvents(int pageNumber, int pageSize, EventType eventType,
            String organizerEmail,
            Instant startTime,
            Instant endTime) {
        com.rsvpplaner.repository.model.Event event = new com.rsvpplaner.repository.model.Event();
        event.setEventType(eventType);

        if (organizerEmail != null) {
            event.setOrganizerEmail(organizerEmail);
        }

        if (startTime != null && endTime != null) {
            event.setEventDates(List.of(com.rsvpplaner.repository.model.EventDate.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .build()));
        }

        Example<com.rsvpplaner.repository.model.Event> eventExample = Example.of(event);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(
                Sort.Direction.DESC, "eventDates"));

        var events = eventRepository.findAll(eventExample, pageable);

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
            EventParticipant eventParticipant, EventDateTimesInner availability) {
        return EventParticipantAvailability.builder()
                .id(UUID.randomUUID().toString())
                .event(event)
                .participant(eventParticipant)
                .startTime(availability.getStartDate().toInstant())
                .endTime(availability.getEndDate().toInstant())
                .status(AttendeeAvailability.StatusEnum.ACCEPTED)
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
        event.dateTimes(dbEvent.getEventDates().stream().map(
                d -> new EventDateTimesInner().startDate(d.getStartTime().atOffset(
                        UTC)).endDate(d.getEndTime().atOffset(UTC))).toList());
        event.setAttendees(dbEvent.getEventParticipants().stream().map(
                p -> new Attendee().name(p.getName()).email(p.getEmail()).attendeeAvailabilities(
                        p.getAvailabilities().stream().map(
                                a -> new AttendeeAvailability().startDate(
                                        a.getStartTime().atOffset(
                                                UTC)).endDate(
                                        a.getEndTime().atOffset(UTC)).status(
                                        a.getStatus())).toList())).toList());
        event.setAttendeesCount(dbEvent.getEventParticipants().size());
        return event;
    }

    public void uploadImage(String eventId, Resource body) {
        try {
            if (body == null) {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(imageBucketName).object(
                        eventId).build());
                return;
            }

            minioClient.putObject(PutObjectArgs.builder().bucket(imageBucketName).object(
                    eventId).contentType(
                    "image/png").stream(body.getInputStream(), body.contentLength(),
                    1000000000).build());

        } catch (IOException e) {
            throw new RuntimeException(e);
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
