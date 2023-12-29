package com.rsvpplaner.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rsvplaner.v1.model.AttendeeAvailability;

@Table(name = "event_participant_availability")
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventParticipantAvailability {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "event_participant_id", referencedColumnName = "id", nullable = false)
    private EventParticipant participant;

    @OneToOne
    @JoinColumn(name = "event_id", referencedColumnName = "id", nullable = false)
    private Event event;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AttendeeAvailability.StatusEnum status;
}
