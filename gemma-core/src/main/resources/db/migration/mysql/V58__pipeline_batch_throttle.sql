-- Batch-level dispatch throttle + hold (§3.4 #1 of PIPELINE_COMPUTE_AND_JOB_MANAGEMENT.md).
--
-- A 500-EE batch shouldn't fire 500 sbatches at once: MAX_CONCURRENT caps how many of a
-- batch's jobs are in flight (QUEUED/RUNNING) at a time; the dispatcher tops up as jobs
-- finish. HELD pauses new dispatches (already-running jobs keep going). Both are
-- scheduler-agnostic — pure Gemma-side dispatch bookkeeping.

ALTER TABLE PIPELINE_JOB_BATCH
    ADD COLUMN MAX_CONCURRENT INT     NULL,                    -- NULL = unlimited (dispatch all at once)
    ADD COLUMN HELD           BOOLEAN NOT NULL DEFAULT FALSE;  -- TRUE = dispatcher skips this batch
