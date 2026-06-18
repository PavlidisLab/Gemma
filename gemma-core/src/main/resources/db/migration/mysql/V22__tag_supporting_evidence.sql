-- Per-tag curation provenance. The curation agents emit a verbatim FindingEvidence[]
-- (quote / source / location, ...) backing each curated tag so a curator can see "did
-- the agent read this off the data or guess?" without leaving the dataset page.
--
-- Gemma stores the array opaquely as a JSON blob in a single nullable TEXT column: it
-- never parses or queries the evidence, so the agents repo owns the FindingEvidence
-- schema and can evolve it without a further Gemma migration. Nullable with no default —
-- plain/legacy tags (and ontology-term hits never accepted from a proposal) carry no
-- evidence and stay NULL, so existing rows backfill without an UPDATE.
ALTER TABLE CHARACTERISTIC
    ADD COLUMN SUPPORTING_EVIDENCE TEXT NULL;
