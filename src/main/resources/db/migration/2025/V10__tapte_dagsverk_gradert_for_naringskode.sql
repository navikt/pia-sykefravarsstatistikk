ALTER TABLE sykefravarsstatistikk_naringskode
    ADD COLUMN tapte_dagsverk_gradert numeric(17, 6);

UPDATE sykefravarsstatistikk_naringskode set tapte_dagsverk_gradert = 0 where tapte_dagsverk_gradert is null;

ALTER TABLE sykefravarsstatistikk_naringskode
    ALTER COLUMN tapte_dagsverk_gradert set not null;
