alter table journalforingstatus add column skal_sende_brev boolean not null default false;
alter table journalforingstatus add column bestillingsid uuid;