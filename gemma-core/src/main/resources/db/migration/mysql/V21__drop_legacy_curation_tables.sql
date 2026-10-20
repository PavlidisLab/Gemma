-- Retire CURATION_DRAFT and AGENT_PROPOSAL. Both tables are empty in every
-- environment (no clients have written to them yet); the new ANNOTATION_SET
-- (mysql/V20) is the unified replacement. Java callers and REST endpoints
-- have been repointed in the same commit train.
--
-- Drop CURATION_DRAFT first because it carries an FK to AGENT_PROPOSAL
-- (FK_CURATION_DRAFT_PROPOSAL, mysql/V12) — dropping AGENT_PROPOSAL with
-- the dependent FK in place would fail.

DROP TABLE IF EXISTS CURATION_DRAFT;
DROP TABLE IF EXISTS AGENT_PROPOSAL;
