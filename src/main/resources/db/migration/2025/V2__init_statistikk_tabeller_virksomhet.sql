create table sykefravarsstatistikk_virksomhet
(
    id                                 serial primary key,
    orgnr                              varchar(20)    not null,
    arstall                            smallint       not null,
    kvartal                            smallint       not null,
    tapte_dagsverk_gradert             numeric(17, 6) not null,
    antall_personer                    numeric(17, 0) not null,
    tapte_dagsverk                     numeric(17, 6) not null,
    mulige_dagsverk                    numeric(17, 6) not null,
    prosent                            numeric(3, 2)  not null,
    rectype                            varchar(1)     not null,
    opprettet                          timestamp default current_timestamp,
    constraint orgnr_arstall_kvartal unique (orgnr, arstall, kvartal)
);
create index idx_orgnr_sykefravarsstatistikk_virksomhet on sykefravarsstatistikk_virksomhet (orgnr);

create table sykefravarsstatistikk_virksomhet_med_varighet
(
    id                                 serial primary key,
    orgnr                              varchar(20)    not null,
    arstall                            smallint       not null,
    kvartal                            smallint       not null,
    varighet                           varchar(1)     not null,
    tapte_dagsverk                     numeric(17, 6) not null,
    opprettet                          timestamp default current_timestamp,
    constraint orgnr_arstall_kvartal_varighet unique (orgnr, arstall, kvartal, varighet)
);
create index idx_orgnr_sykefravarsstatistikk_virksomhet_med_varighet on sykefravarsstatistikk_virksomhet_med_varighet (orgnr);

