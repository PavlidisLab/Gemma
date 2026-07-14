-- See db/migration/mysql/V24__pipeline_batch_throttle.sql for the canonical description.
-- The version number differs because the H2 + MySQL migration streams are keyed
-- independently. H2 supports the same ADD COLUMN / BOOLEAN DEFAULT syntax.

ALTER TABLE PIPELINE_JOB_BATCH
    ADD COLUMN MAX_CONCURRENT INT     NULL,
    ADD COLUMN HELD           BOOLEAN NOT NULL DEFAULT FALSE;
