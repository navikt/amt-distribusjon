alter table varsel add column sendt timestamp with time zone,
   add column status varchar not null default 'UTFORT',
   add column revarsel_for_varsel UUID,
   add column skal_revarsles boolean not null default false;

update varsel
set status = 'AKTIV'
where aktiv_til at time zone 'UTC'> current_timestamp at time zone 'UTC';

alter table varsel alter column status drop default;

alter table varsel drop column er_sendt;