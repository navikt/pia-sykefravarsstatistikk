ALTER TABLE virksomhet_metadata
    ALTER COLUMN primarnaring DROP not null,
    ALTER COLUMN primarnaringskode DROP not null;
