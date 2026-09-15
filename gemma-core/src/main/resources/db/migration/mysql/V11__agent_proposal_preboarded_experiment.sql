-- Proposed-experiment workflow tables (HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md).
--
-- Two changes:
--
-- 1. Adds a new sibling discriminator (PreboardedExperiment) on INVESTIGATION.
--    Single-table inheritance, so the only schema cost is three new columns
--    on INVESTIGATION (NULL for existing EE / Subset rows; populated only on
--    preboarded rows). Column names are prefixed PREBOARDED_ to avoid collision
--    with EE's SOURCE / ACCESSION_FK columns. The Java entity defaults
--    workflowState=Preboarded in its constructor (no SQL `default=` on the
--    discriminator column; see d19dcf45d8 for why default= halts hbm2ddl).
--
-- 2. Adds AGENT_PROPOSAL: append-only record of one curation-agents proposal
--    payload. FK -> INVESTIGATION so the FK works for both ExpressionExperiment
--    and PreboardedExperiment discriminator rows; promotion rebinds the FK
--    from the preboarded row to the EE row (new-row + FK rebind approach).
--
-- The unique key (INVESTIGATION_FK, RUN_ID) enforces the "idempotency on
-- run_id" guarantee in the handoff §"Failure modes + idempotency": re-posting
-- the same run's payload is a no-op that returns the existing proposal row.

ALTER TABLE INVESTIGATION
    ADD COLUMN PREBOARDED_ACCESSION VARCHAR(255) NULL,
    ADD COLUMN PREBOARDED_SOURCE VARCHAR(32) NULL,
    ADD COLUMN PREBOARDED_IDENTIFYING_METADATA LONGTEXT NULL;

-- Lookup by accession for `GET /preboarded?accession=...` and for the
-- 409-on-existing check in POST /preboarded. Not unique because two preboarded rows
-- could in principle share an accession across `source` boundaries; the
-- service-level check guarantees uniqueness within a (source, accession).
CREATE INDEX INVESTIGATION_PREBOARDED_ACCESSION ON INVESTIGATION (PREBOARDED_ACCESSION);

CREATE TABLE AGENT_PROPOSAL (
    ID                  BIGINT       NOT NULL AUTO_INCREMENT,
    INVESTIGATION_FK    BIGINT       NOT NULL,
    RUN_ID              VARCHAR(255) NOT NULL,
    AGENT_VERSION       VARCHAR(255) NULL,
    MODEL               VARCHAR(255) NULL,
    RAN_AT              DATETIME     NULL,
    -- MySQL JSON column on prod gives queryability over the payload
    -- (JSON_EXTRACT, JSON_CONTAINS) without dropping to LONGTEXT semantics.
    -- The Hibernate mapping uses MaterializedClobType which reads/writes the
    -- string the same way against either column type, so the H2 sibling
    -- migration (V13) uses CLOB and tests are agnostic.
    PAYLOAD_JSON        JSON         NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_AGENT_PROPOSAL_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID),
    CONSTRAINT UK_AGENT_PROPOSAL_INVESTIGATION_RUN
        UNIQUE KEY (INVESTIGATION_FK, RUN_ID),
    INDEX IDX_AGENT_PROPOSAL_INVESTIGATION (INVESTIGATION_FK)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
