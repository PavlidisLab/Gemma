-- Attempt/retry chain for PIPELINE_JOB (§3.2 of PIPELINE_COMPUTE_AND_JOB_MANAGEMENT.md).
--
-- A retry mints a NEW PipelineJob for the same (batch, experiment) linked to its
-- predecessor; the failed job is never mutated (immutable history — the audit trail of
-- "OOM -> bumped mem -> bad SRA -> swapped -> DONE"). The current attempt for a
-- (batch, ee) is the row with SUPERSEDED_BY_FK IS NULL.
--
-- Delegated model: Nextflow's -resume does the compute-level rerun; these columns are
-- Gemma's durable record of *that* a reattempt happened, with what params and outcome.

ALTER TABLE PIPELINE_JOB
    ADD COLUMN ATTEMPT          INT         NOT NULL DEFAULT 1,   -- 1-based, denormalized for display/sort
    ADD COLUMN RETRY_OF_FK      BIGINT      NULL,                 -- previous attempt (walk the chain)
    ADD COLUMN SUPERSEDED_BY_FK BIGINT      NULL,                 -- the retry that replaced this (monotonic; NULL = current)
    ADD COLUMN FAILURE_CLASS    VARCHAR(16) NULL,                 -- TRANSIENT | PERMANENT | UNKNOWN (pipeline-reported)
    ADD COLUMN PARAMS_JSON      LONGTEXT    NULL;                 -- params this attempt ran with (per-attempt provenance)

ALTER TABLE PIPELINE_JOB
    ADD CONSTRAINT CK_PIPELINE_JOB_FAILURE_CLASS
        CHECK (FAILURE_CLASS IS NULL OR FAILURE_CLASS IN ('TRANSIENT', 'PERMANENT', 'UNKNOWN'));

-- "current attempt for this (batch, ee)" + attempt ordering.
ALTER TABLE PIPELINE_JOB
    ADD KEY IDX_PIPELINE_JOB_BATCH_EE_ATTEMPT (BATCH_FK, EXPERIMENT_FK, ATTEMPT);

-- Self-references. ON DELETE SET NULL so the batch's ON DELETE CASCADE (BATCH_FK) can
-- drop a whole chain without the self-FKs blocking the cascade order.
ALTER TABLE PIPELINE_JOB
    ADD CONSTRAINT FK_PIPELINE_JOB_RETRY_OF
        FOREIGN KEY (RETRY_OF_FK) REFERENCES PIPELINE_JOB (ID) ON DELETE SET NULL,
    ADD CONSTRAINT FK_PIPELINE_JOB_SUPERSEDED_BY
        FOREIGN KEY (SUPERSEDED_BY_FK) REFERENCES PIPELINE_JOB (ID) ON DELETE SET NULL;
