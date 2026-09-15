-- Unified AnnotationSet entity (handoffs/GEB_HANDOFF_2026_06_12_UNIFIED_ANNOTATION_SET.md).
--
-- One table for "any annotation hypothesis attached to an Investigation",
-- discriminated by ROLE:
--   PROPOSAL  immutable agent-emitted hypothesis (today's AgentProposal,
--             both kind=proposal and kind=audit live here; KIND column
--             carries the sub-discriminator).
--   DRAFT     mutable curator WIP (today's CurationDraft.payloadJson),
--             with PARENT_FK pointing at the PROPOSAL it was seeded from;
--             PARKED_ELEMENTS is the DRAFT-specific sidecar (parked element
--             keys, JSON array). Per-element disposition is still derived
--             at read time by CurationDraftDispositions diffing payload vs
--             parent payload.
--   SNAPSHOT  immutable capture of the experiment's current annotation
--             state. A SNAPSHOT with FINALIZED_AT set is the "polished"
--             view a curator has blessed as canonical; without it, the row
--             is a raw capture (e.g. promotion artifact, comparison probe).
--
-- This migration is DDL only — no backfill from AGENT_PROPOSAL or
-- CURATION_DRAFT; those tables stay in place and continue to back the
-- existing endpoints. A subsequent migration will backfill + flip the
-- controllers + drop the old tables after a stabilisation window.
--
-- Idempotency: UNIQUE(INVESTIGATION_FK, ROLE, RUN_ID). The service layer
-- derives RUN_ID per role so the same constraint serves three different
-- semantics:
--   PROPOSAL  RUN_ID = the agent runner's run id (today's contract).
--   DRAFT     RUN_ID = "draft-{created_by}" so the unique key enforces
--             "one DRAFT per (investigation, curator)" without a separate
--             column.
--   SNAPSHOT  RUN_ID = generated UUID at create time (append-only).
--
-- PAYLOAD_JSON is MySQL JSON on prod (queryable via JSON_EXTRACT etc.);
-- the H2 sibling (V21) uses CLOB. Hibernate's MaterializedClobType reads
-- and writes the same String against either column type.

CREATE TABLE ANNOTATION_SET (
    ID                  BIGINT       NOT NULL AUTO_INCREMENT,
    INVESTIGATION_FK    BIGINT       NOT NULL,
    ROLE                VARCHAR(32)  NOT NULL,
    SOURCE              VARCHAR(32)  NOT NULL,
    KIND                VARCHAR(32)  NULL,
    RUN_ID              VARCHAR(255) NOT NULL,
    PARENT_FK           BIGINT       NULL,
    CREATED_BY          VARCHAR(255) NULL,
    CREATED_AT          DATETIME(3)  NOT NULL,
    UPDATED_AT          DATETIME(3)  NOT NULL,
    FINALIZED_AT        DATETIME(3)  NULL,
    FINALIZED_BY        VARCHAR(255) NULL,
    AGENT_VERSION       VARCHAR(255) NULL,
    MODEL               VARCHAR(255) NULL,
    RAN_AT              DATETIME     NULL,
    PAYLOAD_JSON        JSON         NULL,
    PARKED_ELEMENTS     LONGTEXT     NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ANNOTATION_SET_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_ANNOTATION_SET_PARENT
        FOREIGN KEY (PARENT_FK) REFERENCES ANNOTATION_SET (ID)
        ON DELETE SET NULL,
    CONSTRAINT UK_ANNOTATION_SET_INVESTIGATION_ROLE_RUN
        UNIQUE KEY (INVESTIGATION_FK, ROLE, RUN_ID),
    INDEX IDX_ANNOTATION_SET_INVESTIGATION_ROLE (INVESTIGATION_FK, ROLE),
    INDEX IDX_ANNOTATION_SET_PARENT (PARENT_FK),
    INDEX IDX_ANNOTATION_SET_RUN (RUN_ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
