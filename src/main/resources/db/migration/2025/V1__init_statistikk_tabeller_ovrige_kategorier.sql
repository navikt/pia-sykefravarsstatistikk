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

create table sykefravarsstatistikk_sektor
(
    id              serial primary key,
    sektor          varchar        not null,
    arstall         smallint       not null,
    kvartal         smallint       not null,
    antall_personer numeric(17, 0) not null,
    tapte_dagsverk  numeric(17, 6) not null,
    mulige_dagsverk numeric(17, 6) not null,
    prosent         numeric(3, 2)  not null,
    opprettet       timestamp default current_timestamp,
    constraint sektor_arstall_kvartal unique (sektor, arstall, kvartal)
);
create index idx_sektor_sykefravarsstatistikk_sektor on sykefravarsstatistikk_sektor (sektor);

create table sykefravarsstatistikk_naring
(
    id              serial         primary key,
    naring          varchar        not null,
    arstall         smallint       not null,
    kvartal         smallint       not null,
    antall_personer numeric(17, 0) not null,
    tapte_dagsverk  numeric(17, 6) not null,
    mulige_dagsverk numeric(17, 6) not null,
    tapte_dagsverk_gradert numeric(17, 6) not null,
    prosent         numeric(3, 2)  not null,
    opprettet       timestamp default current_timestamp,
    constraint naring_arstall_kvartal unique (naring, arstall, kvartal)
);
create index idx_naring_sykefravarsstatistikk_naring on sykefravarsstatistikk_naring (naring);

create table sykefravarsstatistikk_naring_med_varighet
(
    id              serial         primary key,
    naring          varchar        not null,
    arstall         smallint       not null,
    kvartal         smallint       not null,
    varighet        varchar(1)     not null,
    tapte_dagsverk  numeric(17, 6) not null,
    opprettet       timestamp default current_timestamp,
    constraint naring_arstall_kvartal_varighet unique (naring, arstall, kvartal, varighet)
);
create index idx_naring_sykefravarsstatistikk_naring_med_varighet on sykefravarsstatistikk_naring_med_varighet (naring);

create table sykefravarsstatistikk_naringskode
(
    id              serial primary key,
    naringskode     varchar        not null,
    arstall         smallint       not null,
    kvartal         smallint       not null,
    antall_personer numeric(17, 0) not null,
    tapte_dagsverk  numeric(17, 6) not null,
    mulige_dagsverk numeric(17, 6) not null,
    prosent         numeric(3, 2)  not null,
    opprettet       timestamp default current_timestamp,
    constraint naringskode_arstall_kvartal unique (naringskode, arstall, kvartal)
);
create index idx_naringskode_sykefravarsstatistikk_naringskode on sykefravarsstatistikk_naringskode (naringskode);

create table sykefravarsstatistikk_naringskode_med_varighet
(
    id             serial primary key,
    naringskode    varchar        not null,
    arstall        smallint       not null,
    kvartal        smallint       not null,
    varighet       varchar(1)     not null,
    tapte_dagsverk numeric(17, 6) not null,
    opprettet      timestamp default current_timestamp,
    constraint naringskode_arstall_kvartal_varighet unique (naringskode, arstall, kvartal, varighet)
);
create index idx_naringskode_sykefravarsstatistikk_naringskode_med_varighet on sykefravarsstatistikk_naringskode_med_varighet (naringskode);

create table sykefravarsstatistikk_bransje
(
    id              serial primary key,
    bransje         varchar        not null,
    arstall         smallint       not null,
    kvartal         smallint       not null,
    antall_personer numeric(17, 0) not null,
    tapte_dagsverk  numeric(17, 6) not null,
    mulige_dagsverk numeric(17, 6) not null,
    prosent         numeric(3, 2)  not null,
    opprettet       timestamp default current_timestamp,
    constraint bransje_arstall_kvartal unique (bransje, arstall, kvartal)
);
create index idx_bransje_sykefravarsstatistikk_bransje on sykefravarsstatistikk_bransje (bransje);

create table sykefravarsstatistikk_bransje_med_varighet
(
    id             serial primary key,
    bransje        varchar        not null,
    arstall        smallint       not null,
    kvartal        smallint       not null,
    varighet       varchar(1)     not null,
    tapte_dagsverk numeric(17, 6) not null,
    opprettet      timestamp default current_timestamp,
    constraint bransje_arstall_kvartal_varighet unique (bransje, arstall, kvartal, varighet)
);
create index idx_bransje_sykefravarsstatistikk_bransje_med_varighet on sykefravarsstatistikk_bransje_med_varighet (bransje);