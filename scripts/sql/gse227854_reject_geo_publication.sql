-- GSE227854: record that GEO's own !Series_pubmed_id names the wrong paper.
--
-- READ THIS BEFORE RUNNING. This is a curation decision about one dataset, not part of the
-- V25 migration, and it is deliberately not bundled into one: a migration is applied
-- everywhere and forever, and a judgement about a single experiment should be reviewable
-- and revertible on its own.
--
-- ── the case ─────────────────────────────────────────────────────────────────────────
-- GEO's !Series_pubmed_id for GSE227854 is 38088204, "Global analysis of binding sites of
-- U2AF1 and ZRSR2 reveals RNA elements required for mutually exclusive splicing" (NAR,
-- 2024-02-09). The series title is "Dissolution of RNA condensates by the embryonic stem
-- cell protein L1TD1 [RNA-seq]", which names 38165001, "Dissolution of ribonucleoprotein
-- condensates by the embryonic stem cell protein L1TD1" (NAR, 2024-04-12), almost verbatim.
-- Both are NAR 2024, two months apart, from the same lab: this reads as a submitter pasting
-- the wrong one of their own two papers. GEO's link is wrong on the record's own evidence,
-- not on anyone's authority. Verified by hand from GEO's SOFT record on 2026-08-13 (Rachel;
-- eval repo data/paper_assignment_corrections/corrections_2026-08-17b_gse227854.json).
--
-- This is the one experiment of nineteen whose publication error is UPSTREAM. The other
-- eighteen are the eval gold's own errors and do not describe anything wrong in Gemma, so
-- they are deliberately NOT loaded here.
--
-- ── why it is worth recording even though Gemma is not currently wrong ───────────────
-- As of 2026-08-17 GSE227854 (id 27929) has NO publication in Gemma at all. Nothing needs
-- correcting. What needs preventing is the next GEO refresh: the importer sets a primary
-- publication precisely when there is none, so it will take 38088204 the first time it
-- runs. Recording the rejection now is what makes that a no-op instead of a four-day-silent
-- error. On the eval side the same thing has already happened once — a correction applied
-- on 08-13 was undone by a cache rebuild on 08-14 that re-fetched from GEO.
--
-- ── preconditions ────────────────────────────────────────────────────────────────────
-- Requires migration V25 (PUBLICATION_ASSOCIATION). Enforcement requires the code that
-- reads it — GeoServiceImpl, the publication CLIs and PUT /datasets/{id}/publications — so
-- running this before that deploy records the decision without yet acting on it, which is
-- harmless and is the right order.
--
-- 38088204 must already exist as a BIBLIOGRAPHIC_REFERENCE. Deliberately no INSERT for it:
-- hand-building a reference plus its DATABASE_ENTRY in SQL duplicates what
-- BibliographicReferenceService does from PubMed, and gets the metadata wrong. If the
-- SELECT below returns nothing, use the REST route instead, which fetches the reference on
-- demand:
--
--   PUT /rest/v2/datasets/GSE227854/publications
--   {"primaryPublication": {"pubMedId": "38165001", "source": "curator",
--                           "evidenceCode": "IC",
--                           "evidence": "The series title names this paper almost verbatim."},
--    "otherRelevantPublications": [],
--    "rejectedPublications": [{"pubMedId": "38088204", "source": "curator",
--                              "evidenceCode": "IC",
--                              "evidence": "GEO's !Series_pubmed_id, but it names a different NAR 2024 paper by the same lab; the series title names 38165001."}]}
--
-- Run as a single transaction and check the SELECT before committing.

START TRANSACTION;

-- What we are about to assert, and against what. Zero rows from this means the reference is
-- not in Gemma yet -- take the REST route above.
SELECT ee.ID          AS investigation_id,
       ee.SHORT_NAME  AS short_name,
       br.ID          AS publication_id,
       de.ACCESSION   AS pubmed_id
FROM INVESTIGATION ee
         JOIN DATABASE_ENTRY eede ON eede.ID = ee.ACCESSION_FK
         JOIN BIBLIOGRAPHIC_REFERENCE br ON TRUE
         JOIN DATABASE_ENTRY de ON de.ID = br.PUB_ACCESSION_FK
         JOIN EXTERNAL_DATABASE ped ON ped.ID = de.EXTERNAL_DATABASE_FK
WHERE eede.ACCESSION = 'GSE227854'
  AND ped.NAME = 'PubMed'
  AND de.ACCESSION = '38088204';

INSERT INTO PUBLICATION_ASSOCIATION
    (INVESTIGATION_FK, PUBLICATION_FK, STATUS, ROLE, SOURCE, EVIDENCE, EVIDENCE_CODE, ASSERTED_BY, ASSERTED_AT)
SELECT ee.ID,
       br.ID,
       'REJECTED',
       NULL,
       'CURATOR',
       'GEO !Series_pubmed_id names this paper (NAR 2024-02-09, U2AF1/ZRSR2 splicing), but the series title "Dissolution of RNA condensates by the embryonic stem cell protein L1TD1" names PMID 38165001 (NAR 2024-04-12) almost verbatim. Same lab, two NAR papers two months apart; the submitter cross-linked the wrong one. Verified by hand from GEO''s SOFT record, 2026-08-13.',
       'IC',
       'rachel',
       NOW(3)
FROM INVESTIGATION ee
         JOIN DATABASE_ENTRY eede ON eede.ID = ee.ACCESSION_FK
         JOIN BIBLIOGRAPHIC_REFERENCE br ON TRUE
         JOIN DATABASE_ENTRY de ON de.ID = br.PUB_ACCESSION_FK
         JOIN EXTERNAL_DATABASE ped ON ped.ID = de.EXTERNAL_DATABASE_FK
WHERE eede.ACCESSION = 'GSE227854'
  AND ped.NAME = 'PubMed'
  AND de.ACCESSION = '38088204'
  -- Idempotent: re-running must not violate the unique key, and must not overwrite a later
  -- ruling by someone who has looked at this again.
  AND NOT EXISTS (SELECT 1
                  FROM PUBLICATION_ASSOCIATION pa
                  WHERE pa.INVESTIGATION_FK = ee.ID
                    AND pa.PUBLICATION_FK = br.ID);

-- Expect exactly one row, STATUS='REJECTED'.
SELECT pa.*
FROM PUBLICATION_ASSOCIATION pa
         JOIN INVESTIGATION ee ON ee.ID = pa.INVESTIGATION_FK
         JOIN DATABASE_ENTRY eede ON eede.ID = ee.ACCESSION_FK
WHERE eede.ACCESSION = 'GSE227854';

COMMIT;
