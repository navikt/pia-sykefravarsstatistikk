create table virksomhet_metadata
(
    id                      serial primary key,
    orgnr                   varchar(20)     not null,
    arstall                 smallint        not null,
    kvartal                 smallint        not null,
    sektor                  varchar         not null,
    primarnaring            varchar(2)      not null,
    primarnaringskode       varchar(5)      not null,
    rectype                 varchar(1),
    importert               timestamp       default current_timestamp,
    unique (orgnr, arstall, kvartal)
);
create index idx_orgnr_virksomhet_metadata on virksomhet_metadata (orgnr);

create table publiseringsdatoer
(
    rapport_periode     int         primary key,
    offentlig_dato      date        not null,
    oppdatert_i_dvh     date        not null,
    importert           timestamp   default current_timestamp
);
