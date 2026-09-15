-- 8-state workflow lifecycle (HANDOFF_WORKFLOW_STATE_STORAGE.md).
--
-- Adds a first-class WORKFLOW_STATE column to INVESTIGATION so the
-- workflow position is queryable as a single indexed equality
-- predicate ("everything in Audit state") rather than derived from a
-- combination of curationDetails flags + audit-event timestamps.
--
-- Backfill is intentionally conservative on this first pass: every
-- existing row defaults to 'Loaded'. The handoff §"Open question 2"
-- describes the curator-approved refinement (mapping troubled /
-- needsAttention / public / unprocessed flags onto the eight-state
-- enum) and explicitly defers it to a follow-on migration that has
-- curator sign-off. Backfill rows do NOT emit audit events (per the
-- handoff's "Backfill does NOT emit audit events" rule).
--
-- WORKFLOW_STATE_ENTERED_AT is NULL on backfilled rows -- the
-- transition timestamp is unknown for state the row already had at
-- migration time. The service layer populates it on every real
-- transition.

ALTER TABLE INVESTIGATION
    ADD COLUMN WORKFLOW_STATE VARCHAR(32) NOT NULL DEFAULT 'Loaded',
    ADD COLUMN WORKFLOW_STATE_ENTERED_AT DATETIME NULL;

CREATE INDEX INVESTIGATION_WORKFLOW_STATE ON INVESTIGATION (WORKFLOW_STATE);
