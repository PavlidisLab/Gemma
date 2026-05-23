-- H2 sibling of mysql/V15__agent_proposal_lifecycle.sql. Differences:
--   * H2 does not honour the `ON UPDATE CURRENT_TIMESTAMP` clause — the
--     service layer stamps LAST_UPDATED on every save() so the test profile
--     stays behaviourally aligned with MySQL prod.
--   * H2 ignores `DESC` inside CREATE INDEX (parsed but no-op since H2
--     walks both directions on btrees) — keyword dropped.
--
-- See mysql/V15 for the motivation (lifecycle + disposition columns
-- backing the curation-UI mutation endpoints).

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN STATUS            VARCHAR(32) NOT NULL DEFAULT 'OPEN';

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN DISPOSITION       VARCHAR(32) NULL;

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN DISPOSITION_NOTE  CLOB        NULL;

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN FINALIZED_AT      TIMESTAMP(3) NULL;

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN LAST_UPDATED      TIMESTAMP(3) NULL;

CREATE INDEX IF NOT EXISTS IDX_AGENT_PROPOSAL_KIND_STATUS_RAN_AT
    ON AGENT_PROPOSAL (KIND, STATUS, RAN_AT);
