create table endringshendelse
(
    hendelse_id uuid primary key,
    deltaker_id uuid                                               not null,
    hendelse    jsonb                                              not null,
    created_at  timestamp with time zone default current_timestamp not null
);

create index endringshendelse_deltaker_id_idx on endringshendelse(deltaker_id);