package com.rsvpplaner.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "event_dates")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDate {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "id", nullable = false)
    private Event event;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventDate eventDate)) return false;
        return Objects.equals(getId(), eventDate.getId()) && Objects.equals(
                getEvent(), eventDate.getEvent()) && Objects.equals(getStartTime(),
                eventDate.getStartTime()) && Objects.equals(getEndTime(),
                eventDate.getEndTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getEvent(), getStartTime(), getEndTime());
    }
}
