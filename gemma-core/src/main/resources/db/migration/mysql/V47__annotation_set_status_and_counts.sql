-- A proposal gets a review STATUS, and a list row gets the two counts its card shows.
-- Asked for by uib 2026-09-04, measured against gemma2 rather than inferred from the spec.
--
-- STATUS: pending | needs_changes | accepted | rejected, the curation store's own four,
-- adopted rather than re-spelled. Paul, asked directly: "it seems to me that status needs to
-- be added to gemma ... we're not evaluating the algorithm formally so much, but those four
-- states seem reasonable."
--
-- 🛑 It is NOT any of the three signals already on an annotation set, all of which uib checked
-- before asking. FINALIZED_AT says the review was closed, not which way it went.
-- ANNOTATION_SET_TRIAGE holds a JUDGE's fine/wont_fix/might_fix/must_fix on the whole set.
-- ANNOTATION_SET_DISPOSITION holds a curator's ruling on ONE finding. Rolling the last of those
-- up to a set-level status needs a rule -- all / any / majority -- that nobody has chosen, so
-- the roll-up would be an invention rather than a reading. Hence a stored column.
--
-- 🛑 PENDING IS STORED HERE, which is the opposite of what the neighbouring vocabularies do:
-- TriageVerdict and FindingDisposition both refuse a pending value because absence of a ROW is
-- the state. This is a COLUMN on a row that already exists, so absence is not available to mean
-- "nobody has ruled" -- NULL has to mean something else, and it means the set is not a kind that
-- gets reviewed at all (a draft, a snapshot, a commit). Backfilled to PENDING for existing
-- proposals below so the two readings never overlap.
--
-- FACTOR_COUNT / TAG_COUNT: what the inbox card prints. The cross-corpus list serves a thin
-- projection with no PAYLOAD_JSON, so today drawing the list costs one full fetch per row -- 94 KB
-- on the set uib sampled, scaling with the corpus rather than with the screen.
--
-- ⚠️ These two are a derived HINT, not a contract. PAYLOAD_JSON's shape is owned by its producer
-- and Gemma persists it verbatim; the counts are read off it best-effort at write time and stay
-- NULL when the shape is not recognized. A caller must treat NULL as "unknown", never as zero.
-- Deliberately not backfilled: the payloads are on disk and can be recounted, but doing it in a
-- migration would bake today's reading of a producer-owned shape into the schema's history.
ALTER TABLE ANNOTATION_SET
    ADD COLUMN STATUS VARCHAR(32) NULL,
    ADD COLUMN FACTOR_COUNT INT NULL,
    ADD COLUMN TAG_COUNT INT NULL;

-- Every proposal that already exists is one nobody has ruled on.
UPDATE ANNOTATION_SET SET STATUS = 'PENDING' WHERE ROLE = 'PROPOSAL';
