create table event
(
    id              varchar not null primary key,
    event_type      varchar not null,
    description     varchar,
    location        varchar,
    organizer_email varchar,
    organizer_name  varchar,
    title           varchar
);

create index event_type_idx ON event (event_type);

create table event_times
(
    id         varchar     not null primary key,
    event_id   varchar     not null,
    start_time timestamptz not null,
    end_time   timestamptz not null,

    constraint event_times_event_id foreign key (event_id) references event (id)
);

create index event_times_event_id_idx ON event_times (event_id);

create table event_participant
(
    id               varchar not null primary key,
    email            varchar not null,
    event_id         varchar not null,
    name             varchar,
    participant_type varchar,

    unique (email, event_id),
    constraint participant_event_id foreign key (event_id) references event (id)
);

create index event_participant_type_event on event_participant (event_id, participant_type);

create table event_participant_availability
(
    id                   varchar not null primary key,
    event_participant_id varchar not null,
    event_id             varchar not null,
    end_time   timestamptz not null,
    start_time timestamptz not null,
    status     varchar     not null,

    constraint participant_availability_id foreign key (event_participant_id) references event_participant (id),
    constraint participant_availability_event foreign key (event_id) references event (id)
);
