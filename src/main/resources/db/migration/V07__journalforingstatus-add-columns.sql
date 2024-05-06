alter table journalforingstatus add column skal_journalfores boolean not null default false;
alter table journalforingstatus add column modified_at timestamp with time zone default current_timestamp not null;

alter table journalforingstatus alter column skal_journalfores drop default;
alter table journalforingstatus alter column journalpost_id drop not null;