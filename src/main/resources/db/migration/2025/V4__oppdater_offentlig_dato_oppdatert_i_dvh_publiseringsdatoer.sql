alter table publiseringsdatoer

alter column rapport_periode type varchar,
alter column rapport_periode set not null,

alter column offentlig_dato type timestamp without time zone,
alter column offentlig_dato set not null,

alter column oppdatert_i_dvh type timestamp without time zone,
alter column oppdatert_i_dvh set not null;
