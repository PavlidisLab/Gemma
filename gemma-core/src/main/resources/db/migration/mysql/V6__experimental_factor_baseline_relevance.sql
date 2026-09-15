-- Add curator/agent baseline-relevance hint columns to EXPERIMENTAL_FACTOR.
-- Mirrors the curation-ui Factor shape (`baseline_relevance`,
-- `baseline_relevance_reason`); the curation pipeline writes them, the
-- new /datasets/{id}/heatmap-data endpoint reads them. Nullable since
-- legacy factors won't have values.

ALTER TABLE EXPERIMENTAL_FACTOR
    ADD COLUMN BASELINE_RELEVANCE VARCHAR(32) NULL,
    ADD COLUMN BASELINE_RELEVANCE_REASON TEXT NULL;
