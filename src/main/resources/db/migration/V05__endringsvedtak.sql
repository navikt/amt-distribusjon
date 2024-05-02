create table endringsvedtak
(
    hendelse_id uuid primary key,
    deltaker_id uuid                                               not null,
    hendelse    jsonb                                              not null,
    created_at  timestamp with time zone default current_timestamp not null
);

create index endringsvedtak_deltaker_id_idx on endringsvedtak(deltaker_id);