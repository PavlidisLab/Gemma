-- Curation provenance at the FACTOR and FACTOR VALUE levels, matching what
-- CHARACTERISTIC already carries (V22__tag_supporting_evidence.sql).
--
-- Same opaque JSON array of FindingEvidence items (quote / source / location, ...), same
-- single nullable TEXT column, same rule that Gemma never parses or queries it — the agents
-- repo owns the schema and can evolve it without another migration here.
--
-- WHY two more columns rather than reusing CHARACTERISTIC.SUPPORTING_EVIDENCE: a statement
-- and a tag are Characteristics and already have the column, but a FactorValue is not one —
-- its statements are separate rows — so evidence about the VALUE itself (68 of the 78 blocks
-- measured across the reference 500) has nowhere to go. An ExperimentalFactor does own a
-- category Characteristic, but a factor's category is nullable and is replaced during
-- curation, which would drop the factor's evidence as a side effect of an unrelated edit.
--
-- Nullable with no default: every existing factor and factor value carries no evidence and
-- stays NULL, so the two ALTERs backfill without an UPDATE.
ALTER TABLE EXPERIMENTAL_FACTOR
    ADD COLUMN SUPPORTING_EVIDENCE TEXT NULL;

ALTER TABLE FACTOR_VALUE
    ADD COLUMN SUPPORTING_EVIDENCE TEXT NULL;
