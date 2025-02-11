ALTER TABLE virksomhet_metadata
    RENAME COLUMN importert TO opprettet;

ALTER TABLE publiseringsdatoer
    RENAME COLUMN importert TO opprettet;
