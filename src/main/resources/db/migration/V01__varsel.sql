create table varsel
(
    id          uuid primary key,
    type        varchar                  not null,
    aktiv_fra   timestamp with time zone not null,
    aktiv_til   timestamp with time zone,
    deltaker_id uuid                     not null,
    personident varchar                  not null,
    tekst       varchar                  not null
)