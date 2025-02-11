ALTER TABLE sykefravarsstatistikk_land
    ALTER COLUMN prosent type numeric(4,1),
    ALTER COLUMN prosent set not null;

ALTER TABLE sykefravarsstatistikk_sektor
    ALTER COLUMN prosent type numeric(4,1),
    ALTER COLUMN prosent set not null;

ALTER TABLE sykefravarsstatistikk_naring
    ALTER COLUMN prosent type numeric(4,1),
    ALTER COLUMN prosent set not null;

ALTER TABLE sykefravarsstatistikk_naringskode
    ALTER COLUMN prosent type numeric(4,1),
    ALTER COLUMN prosent set not null;

ALTER TABLE sykefravarsstatistikk_bransje
    ALTER COLUMN prosent type numeric(4,1),
    ALTER COLUMN prosent set not null;

ALTER TABLE sykefravarsstatistikk_virksomhet
    ALTER COLUMN prosent type numeric(4,1),
    ALTER COLUMN prosent set not null;
