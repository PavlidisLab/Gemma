-- Unified per-(investigation, curator) curation draft buffer.
--
-- See STATUS_UNIFIED_CURATION_DRAFT.md for the design rationale. One mutable
-- row carries BOTH the curator's WIP payload AND (when seeded from an agent
-- proposal) a snapshot of the proposal payload, so per-element dispositions
-- (retained/edited/rejected/parked) can be DERIVED at read time by diffing
-- the two JSON blobs. Only PARKED_ELEMENTS needs explicit storage.
--
-- UNIQUE(INVESTIGATION_FK, CURATOR_FK) — one draft per (EE, curator) pair.
-- Cascade-delete the FKs to investigation + curator so the buffer never
-- outlives its targets. Proposal FK is ON DELETE SET NULL so a proposal
-- drop leaves the WIP payload intact (sans the disposition diff baseline).

CREATE TABLE CURATION_DRAFT (
    ID                      BIGINT      NOT NULL AUTO_INCREMENT,
    INVESTIGATION_FK        BIGINT      NOT NULL,
    CURATOR_FK              BIGINT      NOT NULL,
    PAYLOAD_JSON            LONGTEXT    NOT NULL,
    PROPOSAL_FK             BIGINT      NULL,
    PROPOSAL_SNAPSHOT_JSON  LONGTEXT    NULL,
    PARKED_ELEMENTS         LONGTEXT    NULL,
    STARTED_AT              DATETIME    NOT NULL,
    LAST_EDITED_AT          DATETIME    NOT NULL,
    FINALIZED_AT            DATETIME    NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_CURATION_DRAFT_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_CURATION_DRAFT_CURATOR
        FOREIGN KEY (CURATOR_FK) REFERENCES CONTACT (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_CURATION_DRAFT_PROPOSAL
        FOREIGN KEY (PROPOSAL_FK) REFERENCES AGENT_PROPOSAL (ID)
        ON DELETE SET NULL,
    CONSTRAINT UQ_CURATION_DRAFT_PER_CURATOR_EE
        UNIQUE KEY (INVESTIGATION_FK, CURATOR_FK)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- "My recent drafts" listing.
CREATE INDEX IDX_CURATION_DRAFT_CURATOR_RECENT
    ON CURATION_DRAFT (CURATOR_FK, LAST_EDITED_AT DESC);

-- "All reviews of proposal X" listing.
CREATE INDEX IDX_CURATION_DRAFT_PROPOSAL
    ON CURATION_DRAFT (PROPOSAL_FK);
