-- ØVRIGE KATEGORIER KOLONNER --
ALTER TABLE sykefravarsstatistikk_land ADD COLUMN sist_endret timestamp default null;

ALTER TABLE sykefravarsstatistikk_sektor ADD COLUMN sist_endret timestamp default null;

ALTER TABLE sykefravarsstatistikk_naring ADD COLUMN sist_endret timestamp default null;
ALTER TABLE sykefravarsstatistikk_naring_med_varighet ADD COLUMN sist_endret timestamp default null;

ALTER TABLE sykefravarsstatistikk_naringskode ADD COLUMN sist_endret timestamp default null;
ALTER TABLE sykefravarsstatistikk_naringskode_med_varighet ADD COLUMN sist_endret timestamp default null;

ALTER TABLE sykefravarsstatistikk_bransje ADD COLUMN sist_endret timestamp default null;
ALTER TABLE sykefravarsstatistikk_bransje_med_varighet ADD COLUMN sist_endret timestamp default null;

-- VIRKSOMHET KOLONNER --
ALTER TABLE sykefravarsstatistikk_virksomhet ADD COLUMN sist_endret timestamp default null;
ALTER TABLE sykefravarsstatistikk_virksomhet_med_varighet ADD COLUMN sist_endret timestamp default null;

-- METADATA KOLONNER --
ALTER TABLE virksomhet_metadata ADD COLUMN sist_endret timestamp default null;
ALTER TABLE publiseringsdatoer ADD COLUMN sist_endret timestamp default null;
