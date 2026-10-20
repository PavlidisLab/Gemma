-- Step 4 of the AgentCuration unification.
--
-- Adds the lifecycle + disposition columns the curation-UI's mutation
-- endpoints (PATCH /curation-proposals/{id}, PATCH /audits/{id},
-- POST /audits/{id}/finalize, POST /audits/{id}/reopen) need to flip from
-- the 501 stub returns to real handlers.
--
-- STATUS values: 'OPEN' (default; agent emitted, no curator action yet),
--   'FINALIZED' (curator finalized; audit lifecycle), 'REOPENED' (curator
--   un-finalized after FINALIZED — re-opens the edit surface).
--
-- DISPOSITION wire vocabulary (see RECCE §4.1): 'accept',
--   'accepted_with_edits', 'reject', 'edit', 'park'. Stored as the wire
--   string; validation happens at the REST handler boundary.
--
-- LAST_UPDATED carries an `ON UPDATE CURRENT_TIMESTAMP(3)` so MySQL stamps
-- the row on every UPDATE without requiring the service layer to do so. The
-- Java entity also stamps it on save() to keep H2 (no `ON UPDATE` semantics)
-- behaviourally aligned.

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN STATUS            VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN DISPOSITION       VARCHAR(32) NULL,
    ADD COLUMN DISPOSITION_NOTE  TEXT        NULL,
    ADD COLUMN FINALIZED_AT      DATETIME(3) NULL,
    ADD COLUMN LAST_UPDATED      DATETIME(3) NULL
        ON UPDATE CURRENT_TIMESTAMP(3);

-- Index the typical inbox filter: kind + status, newest first by ran_at.
CREATE INDEX IDX_AGENT_PROPOSAL_KIND_STATUS_RAN_AT
    ON AGENT_PROPOSAL (KIND, STATUS, RAN_AT DESC);
