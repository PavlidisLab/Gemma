-- ---------------------------------------------------------------------------------------
-- Annotation URI census -- every ontology term in use in the corpus, by slot, with counts.
--
-- READ-ONLY. Nothing here writes. Safe against prod gemd.
--
-- 🛑 TARGET IS MySQL 5.7 (prod is 5.7.44). No CTEs, no window functions, and no
-- REGEXP_REPLACE -- that one is 8.0.4+, and an earlier draft of Q3 used it and would have
-- failed on prod. Q3's label normalization is a REPLACE chain for this reason; keep it that
-- way until prod's MySQL upgrade lands.
--
-- OUTPUT DOES NOT BELONG IN THIS REPO. This repo is code. The census output is large and
-- regenerable, so it lands in ~/Data/gemma-curation-agents-data/annotation_uri_census/
-- (see its README for the invocation); small derived findings go in
-- gemma-curation-agents-eval/analysis/<date>_<topic>/.
--
-- ONE QUERY, THREE CONSUMERS. It was asked for three times in one day, for three jobs, and
-- they want the same table:
--
--   * uib  -- "annotations grouped by value URI where the category is cell line", the
--            axis-C number for the duplicate-term lane
--            (UIB_TO_GEMMA_BACKEND_2026_08_18_A_CELL_LINE_BLACKLIST... sec.5.1)
--   * cab  -- "distinct value/category URIs in use, with counts", step 1 of the obsolete-
--            term sweep, which they mark BLOCKING
--            (CAB_PLAN_2026_08_18_OBSOLETE_TERM_SWEEP_AS_A_PERIODIC_AGENT_JOB.md sec.6)
--   * Paul -- the cross-match table, over "just regular ontology entities"
--
-- Both letters ask for it as SQL for the same reason: the corpus CANNOT be enumerated
-- through REST. /annotations/search 400s on an empty query, and the cell-line parents
-- (EFO_0000322) have no children in the index, so there is no traversal that reaches the
-- used terms. This file is the enumeration step for anything that needs to ask "what terms
-- are we actually using".
--
-- 🛑 THIS IS DETECTION. IT IS NOT A MIGRATION, AND MUST NOT BECOME ONE YET.
-- Two independent holds are in force:
--   1. The Gemma 2.0 write path is HELD -- a 2.0 write emits audit event types 1.32.7
--      cannot load (21 discriminators; PR #1667, still open). Detection and proposal run
--      today; application does not.
--   2. Categories are read by Gemma 1.0 while it is live, so they cannot move much.
-- Paul, 2026-08-18: "it's more about being ready." Build the inventory and the mechanism;
-- run the harmonization later, deliberately.
--
-- ---------------------------------------------------------------------------------------
-- 🛑 SCOPE: TERM VALUES ONLY, NOT CATEGORIES OR PREDICATES
--
-- Paul, 2026-08-18: "honestly put categories and predicates aside for now -- just regular
-- ontology entities." Categories are read by Gemma 1.0 while it is live and cannot move
-- much, and the predicate vocabulary is already constrained by Relation.terms.txt, so
-- neither is where the duplicate problem lives. The three slots below are the annotation
-- VALUES -- the ontology entities themselves.
--
-- 🛑 WHY THIS READS THREE SLOTS AND NOT ONE
--
-- A Statement has THREE annotatable value slots, not one: the subject is the inherited
-- VALUE column, and OBJECT / SECOND_OBJECT are their own columns with their own URIs. The
-- same split exists on EXPRESSION_EXPERIMENT2CHARACTERISTIC. A census over VALUE_URI alone silently drops most
-- of the annotation surface: the drug/treatment term is frequently in the object position,
-- and the CLO predicates in Relation.terms.txt (CLO_0037209 derives from cell, CLO_0037210
-- derived from cell line, CLO_0037227/0037229) put cell lines there too.
--
-- Q0 measures how wrong a one-slot census would have been. Run it first, and quote it
-- alongside any number taken from a query that reads only VALUE_URI.
--
-- SURFACE: EXPRESSION_EXPERIMENT2CHARACTERISTIC, the denormalized table aggregating EE
-- direct tags + BioMaterial characteristics + FactorValue characteristics uniformly. That
-- is what the REST annotation endpoints read, so these counts are the ones uib and cab see
-- through the API. Q4 cross-checks it against CHARACTERISTIC (the source of truth): EE2C is
-- known to carry stale rows -- 1,008 survived a full updateEe2c on 2026-08-12, see
-- resync_ee2c_from_characteristic.sql. Non-zero does not invalidate the census; it means
-- say so when quoting it.
-- ---------------------------------------------------------------------------------------

-- Reusable: every URI-bearing slot, unpivoted. Everything below is a cut of this.
-- (Repeated per query rather than a view, so each query can be run on its own.)

-- Q0 -- slot distribution. How much of the surface does each slot carry? If object /
-- second_object are non-trivial, a VALUE_URI-only census undercounts by exactly this much.
SELECT slot, COUNT(*) AS n_annotations, COUNT(DISTINCT uri) AS n_distinct_uris
FROM (
  SELECT 'subject' AS slot, VALUE_URI AS uri FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC WHERE VALUE_URI IS NOT NULL
  UNION ALL SELECT 'object',        OBJECT_URI        FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC WHERE OBJECT_URI IS NOT NULL
  UNION ALL SELECT 'second_object', SECOND_OBJECT_URI FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC WHERE SECOND_OBJECT_URI IS NOT NULL
) s
GROUP BY slot ORDER BY n_annotations DESC;

-- Q1 -- THE CENSUS. Every distinct term URI in use, in every value slot, with its label,
-- ontology namespace, annotation count and experiment count. This is cab's step-1 input:
-- classify per distinct URI (thousands), never per annotation (millions).
SELECT CASE
         WHEN uri LIKE '%/CLO\_%'                     THEN 'CLO'
         WHEN uri LIKE '%CVCL\_%'                     THEN 'Cellosaurus'
         WHEN uri LIKE '%/EFO\_%'                     THEN 'EFO'
         WHEN uri LIKE '%/MONDO\_%'                   THEN 'MONDO'
         WHEN uri LIKE '%/UBERON\_%'                  THEN 'UBERON'
         WHEN uri LIKE '%/CL\_%'                      THEN 'CL'
         WHEN uri LIKE '%/CHEBI\_%'                   THEN 'CHEBI'
         WHEN uri LIKE '%/PATO\_%'                    THEN 'PATO'
         WHEN uri LIKE '%/GO\_%'                      THEN 'GO'
         WHEN uri LIKE '%/RO\_%'                      THEN 'RO'
         WHEN uri LIKE '%/OBI\_%'                     THEN 'OBI'
         WHEN uri LIKE '%/MP\_%'                      THEN 'MP'
         WHEN uri LIKE '%/HP\_%'                      THEN 'HP'
         WHEN uri LIKE '%/GENO\_%'                    THEN 'GENO'
         WHEN uri LIKE '%TGEMO\_%'                    THEN 'TGEMO'
         WHEN uri LIKE '%ncbi\_gene%'                 THEN 'NCBI Gene'
         ELSE 'other' END                             AS ns,
       slot, uri, label,
       COUNT(*) AS n_annotations, COUNT(DISTINCT ee) AS n_experiments
FROM (
  SELECT 'subject' AS slot, VALUE_URI AS uri, `VALUE` AS label, EXPRESSION_EXPERIMENT_FK AS ee
    FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC WHERE VALUE_URI IS NOT NULL
  UNION ALL SELECT 'object',        OBJECT_URI,        OBJECT,        EXPRESSION_EXPERIMENT_FK FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC WHERE OBJECT_URI IS NOT NULL
  UNION ALL SELECT 'second_object', SECOND_OBJECT_URI, SECOND_OBJECT, EXPRESSION_EXPERIMENT_FK FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC WHERE SECOND_OBJECT_URI IS NOT NULL
) s
GROUP BY ns, slot, uri, label
ORDER BY n_annotations DESC;

-- Q2 -- uib's question exactly as asked: the cell-line category cut, by slot.
-- Category is matched by URI *or* label, per the DAO's own convention
-- (getAnnotationsUsageFrequencyInternal: URIs match CATEGORY_URI, anything else matches
-- CATEGORY). Gemma loads the canonical English label, so neither form alone is safe.
--
-- 🛑 This is NARROWER than the harmonization needs. A CLO term can sit under category
-- `cell type`, or in the object of a derives-from statement, with no cell-line category
-- present -- Q1b's CLO/Cellosaurus rows are the real population. Report both; the gap
-- between them is itself a finding.
SELECT slot, uri, label, COUNT(*) AS n_annotations, COUNT(DISTINCT ee) AS n_experiments
FROM (
  SELECT 'subject' AS slot, VALUE_URI AS uri, `VALUE` AS label, EXPRESSION_EXPERIMENT_FK AS ee
    FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC
   WHERE VALUE_URI IS NOT NULL
     AND (CATEGORY_URI = 'http://www.ebi.ac.uk/efo/EFO_0000322' OR CATEGORY = 'cell line')
  UNION ALL
  SELECT 'object', OBJECT_URI, OBJECT, EXPRESSION_EXPERIMENT_FK
    FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC
   WHERE OBJECT_URI IS NOT NULL
     AND (CATEGORY_URI = 'http://www.ebi.ac.uk/efo/EFO_0000322' OR CATEGORY = 'cell line')
  UNION ALL
  SELECT 'second_object', SECOND_OBJECT_URI, SECOND_OBJECT, EXPRESSION_EXPERIMENT_FK
    FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC
   WHERE SECOND_OBJECT_URI IS NOT NULL
     AND (CATEGORY_URI = 'http://www.ebi.ac.uk/efo/EFO_0000322' OR CATEGORY = 'cell line')
) s
GROUP BY slot, uri, label
ORDER BY n_annotations DESC;

-- 🛑 GROUP_CONCAT defaults to 1024 bytes and TRUNCATES SILENTLY -- a three-member group
-- with long URIs loses members with no warning, which reads as a two-member group. Raise it
-- for the session before Q3. (Session-scoped, affects nothing outside this connection.)
SET SESSION group_concat_max_len = 1048576;

-- Q3 -- candidate twin groups: same normalized label, more than one URI in use. This is
-- uib's axis-A "17 groups" recomputed from the database rather than the ontology index, so
-- the two numbers can be compared. Restricted to CLO here; drop the LIKE to run corpus-wide.
--
-- Normalization mirrors uib's: lowercase, drop a trailing ' cell', strip non-alphanumerics.
-- 🛑 A normalized collision is a CANDIDATE, never a verdict -- a clone and its parent line
-- normalize alike. Every group is eyeballed before anything acts on it.
-- 🛑 And do NOT break a tie with usage. cab measured CLO_0002950: obsolete, 4 uses, while
-- its declared successor CLO_0002949 has 0. That is the steady state, not an outlier --
-- curators and resolvers keep reaching the familiar label -- so usage is systematically
-- biased TOWARD the obsolete term, and "most-used wins" re-enshrines it.
SELECT norm_label,
       COUNT(DISTINCT uri) AS n_uris,
       SUM(n) AS n_annotations,
       GROUP_CONCAT(CONCAT(uri, ' (', label, ') x', n) ORDER BY n DESC SEPARATOR '  |  ') AS members
FROM (
  SELECT uri, label,
         LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           CASE WHEN RIGHT(label, 5) = ' cell' THEN LEFT(label, CHAR_LENGTH(label) - 5) ELSE label END,
           ' ',''), '-',''), '_',''), '.',''), '/',''), '(',''), ')',''), ',',''), '+','')) AS norm_label,
         COUNT(*) AS n
  FROM (
    SELECT VALUE_URI AS uri, `VALUE` AS label FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC
     WHERE VALUE_URI LIKE '%/CLO\_%' AND `VALUE` IS NOT NULL
    UNION ALL
    SELECT OBJECT_URI, OBJECT FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC
     WHERE OBJECT_URI LIKE '%/CLO\_%' AND OBJECT IS NOT NULL
  ) raw
  GROUP BY uri, label
) per_uri
WHERE norm_label <> ''
GROUP BY norm_label
HAVING COUNT(DISTINCT uri) > 1
ORDER BY n_annotations DESC;

-- Q4 -- EE2C staleness cross-check. Non-zero means the counts above are read through a
-- surface that disagrees with CHARACTERISTIC; the census is still usable, but say so.
SELECT 'ee2c rows whose URI disagrees with CHARACTERISTIC' AS chk, COUNT(*) AS n
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC e JOIN CHARACTERISTIC c ON c.ID = e.ID
WHERE NOT (BINARY e.VALUE_URI         <=> BINARY c.VALUE_URI)
   OR NOT (BINARY e.OBJECT_URI        <=> BINARY c.OBJECT_URI)
   OR NOT (BINARY e.SECOND_OBJECT_URI <=> BINARY c.SECOND_OBJECT_URI);
