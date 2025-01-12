create table sykefravarsstatistikk_land
(
    id                                 serial primary key,
    land                               varchar(20)    not null,
    arstall                            smallint       not null,
    kvartal                            smallint       not null,
    antall_personer                    numeric(17, 0) not null,
    tapte_dagsverk                     numeric(17, 6) not null,
    mulige_dagsverk                    numeric(17, 6) not null,
    prosent                            numeric(3, 2) not null,
    opprettet                          timestamp default current_timestamp,
    constraint land_arstall_kvartal unique (land, arstall, kvartal)
);
create index idx_land_sykefravarsstatistikk_land on sykefravarsstatistikk_land (land);

create table sykefravarsstatistikk_virksomhet
(
    id                                 serial primary key,
    orgnr                              varchar(20)    not null,
    arstall                            smallint       not null,
    kvartal                            smallint       not null,
    tapte_dagsverk_gradert_sykemelding numeric(17, 6) not null,
    antall_personer                    numeric(17, 0) not null,
    tapte_dagsverk                     numeric(17, 6) not null,
    mulige_dagsverk                    numeric(17, 6) not null,
    prosent                            numeric(3, 2) not null,
    opprettet                          timestamp default current_timestamp,
    constraint orgnr_arstall_kvartal unique (orgnr, arstall, kvartal)
);
create index idx_orgnr_sykefravarsstatistikk_virksomhet on sykefravarsstatistikk_virksomhet (orgnr);
