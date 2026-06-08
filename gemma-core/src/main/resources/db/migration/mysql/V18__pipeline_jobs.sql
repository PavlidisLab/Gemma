-- DRAFT — pending Paul's approval before promoting to
-- gemma-core/src/main/resources/db/migration/mysql/V18__pipeline_jobs.sql
--
-- Tables for curator-driven pipeline batch submissions to an external
-- scheduler (Luigi or Nextflow). Scheduler-agnostic at the schema
-- level: scheduler-specific bits live in SCHEDULER_KIND + opaque
-- SCHEDULER_HANDLE on PIPELINE_JOB.

CREATE TABLE PIPELINE_JOB_BATCH (
    ID                BIGINT       NOT NULL AUTO_INCREMENT,
    -- Auditable: standard AUDIT_TRAIL_FK + NAME + DESCRIPTION inherited from AbstractAuditable
    AUDIT_TRAIL_FK    BIGINT       NOT NULL,
    NAME              VARCHAR(255) NOT NULL,                       -- batch title (curator-supplied or autogen)
    DESCRIPTION       TEXT         NULL,                           -- free-form curator note
    PIPELINE          VARCHAR(64)  NOT NULL,                       -- e.g. "rnaseq-quant"
    SUBMITTED_BY_FK   BIGINT       NOT NULL,                       -- FK to CONTACT (curator)
    SUBMITTED_AT      DATETIME(3)  NOT NULL,
    PARAMS_JSON       LONGTEXT     NULL,                           -- pipeline params (versioned by caller)
    STATE             VARCHAR(16)  NOT NULL DEFAULT 'OPEN',        -- OPEN | CLOSED | CANCELLED
    KILL_REQUESTED_AT DATETIME(3)  NULL,
    CLOSED_AT         DATETIME(3)  NULL,
    PRIMARY KEY (ID),
    CONSTRAINT CK_PIPELINE_JOB_BATCH_STATE
        CHECK (STATE IN ('OPEN', 'CLOSED', 'CANCELLED')),
    UNIQUE KEY UK_PIPELINE_JOB_BATCH_AUDIT_TRAIL (AUDIT_TRAIL_FK),
    KEY IDX_PIPELINE_JOB_BATCH_SUBMITTED_BY (SUBMITTED_BY_FK),
    KEY IDX_PIPELINE_JOB_BATCH_PIPELINE_STATE (PIPELINE, STATE),
    CONSTRAINT FK_PIPELINE_JOB_BATCH_AUDIT_TRAIL
        FOREIGN KEY (AUDIT_TRAIL_FK) REFERENCES AUDIT_TRAIL (ID),
    CONSTRAINT FK_PIPELINE_JOB_BATCH_SUBMITTED_BY
        FOREIGN KEY (SUBMITTED_BY_FK) REFERENCES CONTACT (ID)
);

CREATE TABLE PIPELINE_JOB (
    ID                  BIGINT       NOT NULL AUTO_INCREMENT,
    BATCH_FK            BIGINT       NOT NULL,
    EXPERIMENT_FK       BIGINT       NOT NULL,                       -- the EE this job operates on
    STATE               VARCHAR(16)  NOT NULL DEFAULT 'PENDING',     -- see CHECK below
    SCHEDULER_KIND      VARCHAR(16)  NULL,                           -- 'luigi' | 'nextflow' | NULL pre-dispatch
    SCHEDULER_HANDLE    VARCHAR(255) NULL,                           -- opaque id from the scheduler
    SUBMITTED_AT        DATETIME(3)  NULL,                           -- when WE pushed it to the scheduler
    STARTED_AT          DATETIME(3)  NULL,
    FINISHED_AT         DATETIME(3)  NULL,
    LAST_EVENT_AT       DATETIME(3)  NULL,
    LAST_EVENT_KIND     VARCHAR(32)  NULL,
    LAST_PROGRESS_JSON  TEXT         NULL,                           -- snapshot of latest progress payload
    ERROR_MESSAGE       TEXT         NULL,
    PRIMARY KEY (ID),
    CONSTRAINT CK_PIPELINE_JOB_STATE
        CHECK (STATE IN ('PENDING', 'QUEUED', 'RUNNING', 'DONE', 'FAILED', 'CANCELLING', 'CANCELLED')),
    CONSTRAINT CK_PIPELINE_JOB_SCHEDULER_KIND
        CHECK (SCHEDULER_KIND IS NULL OR SCHEDULER_KIND IN ('luigi', 'nextflow', 'mock')),
    KEY IDX_PIPELINE_JOB_BATCH (BATCH_FK),
    KEY IDX_PIPELINE_JOB_EXPERIMENT (EXPERIMENT_FK),
    KEY IDX_PIPELINE_JOB_STATE (STATE),
    KEY IDX_PIPELINE_JOB_LAST_EVENT (LAST_EVENT_AT),
    -- Reconciler poll-loop seek: "non-terminal jobs whose last event is stale"
    KEY IDX_PIPELINE_JOB_RECONCILE (STATE, LAST_EVENT_AT),
    UNIQUE KEY UK_PIPELINE_JOB_SCHEDULER_HANDLE (SCHEDULER_KIND, SCHEDULER_HANDLE),
    CONSTRAINT FK_PIPELINE_JOB_BATCH
        FOREIGN KEY (BATCH_FK) REFERENCES PIPELINE_JOB_BATCH (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_PIPELINE_JOB_EXPERIMENT
        FOREIGN KEY (EXPERIMENT_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE
);

CREATE TABLE PIPELINE_JOB_EVENT (
    ID            BIGINT      NOT NULL AUTO_INCREMENT,
    JOB_FK        BIGINT      NOT NULL,
    OCCURRED_AT   DATETIME(3) NOT NULL,
    KIND          VARCHAR(32) NOT NULL,                              -- progress | stage | stderr | killed | error | completed
    PAYLOAD_JSON  TEXT        NULL,                                  -- shape varies by kind
    PRIMARY KEY (ID),
    KEY IDX_PIPELINE_JOB_EVENT_JOB_AT (JOB_FK, OCCURRED_AT),
    CONSTRAINT FK_PIPELINE_JOB_EVENT_JOB
        FOREIGN KEY (JOB_FK) REFERENCES PIPELINE_JOB (ID)
        ON DELETE CASCADE
);

-- Notes on design choices baked in here:
--
-- 1. KIND on PIPELINE_JOB_EVENT is VARCHAR (not enum CHECK) so new event
--    kinds can be added by the scheduler/pipeline without a migration.
--    The consumer side coerces unknown kinds to a default rendering.
--
-- 2. SCHEDULER_KIND values intentionally lowercase to match the wire
--    discriminator pattern locked in for CurationReview.kind.
--
-- 3. UK on (SCHEDULER_KIND, SCHEDULER_HANDLE) lets the push callback
--    look up a job by scheduler id alone if we don't want to embed
--    Gemma-side job_id in the callback URL.
--
-- 4. PARAMS_JSON / PAYLOAD_JSON kept as TEXT (not JSON column type)
--    because gemd is on MySQL 5.7 and the JSON type's query-side
--    behaviour differs across MySQL versions; if we ever need to
--    query into them we can ALTER later.
--
-- 5. No ACL coverage at the schema level: PipelineJobBatch is owner-
--    scoped via SUBMITTED_BY_FK + admin permission. ACL object identity
--    is added on the entity side if Securable is needed (probably yes —
--    same pattern as Ticket).
