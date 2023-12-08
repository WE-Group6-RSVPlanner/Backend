package com.rsvpplaner.repository.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    private String id;

    private String title;
    private String description;
    private String location;
    private String organizerEmail;
    private String organizerName;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<EventParticipant> eventParticipants;
}
