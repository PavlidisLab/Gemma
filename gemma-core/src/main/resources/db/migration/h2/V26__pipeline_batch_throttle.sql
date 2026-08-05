-- See db/migration/mysql/V25__pipeline_batch_throttle.sql for the canonical description.
-- The version number differs because the H2 + MySQL migration streams are keyed
-- independently. Unlike MySQL, H2 does NOT accept repeated `ADD COLUMN a, ADD COLUMN b`
-- clauses in one ALTER TABLE; it uses the parenthesized bulk form `ADD ( col ..., col ... )`.

ALTER TABLE PIPELINE_JOB_BATCH
    ADD (
        MAX_CONCURRENT INT     NULL,
        HELD           BOOLEAN NOT NULL DEFAULT FALSE
    );
