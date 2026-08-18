-- Populate ANNOTATION_RELATION with the CURATED basis: the triples curators already wrote.
--
-- This is the same projection `TableMaintenanceUtil.updateAnnotationRelationEntries` runs, written
-- out so the table can be filled without waiting for a deploy. Once the build carrying that method
-- is on the server, prefer the CLI -- it is the same SQL and it keeps the maintenance path the only
-- writer:
--
--     gemma-cli updateEe2c --relations
--
-- WHAT THIS WRITES. Nothing is inferred here. A curator wrote
-- `disease model: left ventricular hypertrophy - induced by -> aortic banding` and it went into
-- CHARACTERISTIC.PREDICATE/OBJECT, where the only index on it is per-experiment. This copies those
-- triples into a table keyed from both ends so the relation can be read from the object side too.
--
-- SAFETY. Additive: it writes to ANNOTATION_RELATION and touches nothing else. Gemma 1.32.x does not
-- know this table exists and no existing reader joins to it, so a wrong result here cannot surface
-- anywhere in 1.0. It is still a write to the production database -- run it deliberately.
--
-- REBUILD, NOT UPSERT. The DELETE first is not housekeeping. Every row is derived, so re-running has
-- to be able to REMOVE a relation whose statement a curator has since deleted. An upsert can only
-- correct rows the new query still produces, which is how EXPRESSION_EXPERIMENT2CHARACTERISTIC ended
-- up with 1,008 rows a full rebuild could not fix.
--
-- Reads EE2C rather than CHARACTERISTIC because EE2C has already resolved every annotation to its
-- experiment, at every level, with the anonymous ACL mask alongside. Harvesting from the normalized
-- table would mean re-deriving all of that and keeping a second, divergent copy of it. So: make sure
-- EE2C is current first.


-- 🛑 TWO CORRECTIONS after the first production run (2026-08-17). Re-running this replaces the
-- CURATED rows wholesale, so it is also the cleanup.
--
-- 1. EMPTY-STRING URIs. Some curated rows carry PREDICATE_URI = '' rather than NULL. An empty
--    string passes an `IS NOT NULL` test and then matches nothing: a consumer filtering on the URI
--    never finds the row, and a consumer checking for NULL does not either. Every URI column is now
--    NULLIF(TRIM(...), '').
--
-- 2. CONTROL-ARM MARKERS. A statement whose OBJECT is a baseline marker is not a relation between
--    two concepts -- it is how a control arm is flagged. `OBI_0000220 reference subject role`
--    appeared as the object of 10 curated statements and IS grounded, so a gate seeded with an
--    experiment's term URIs would reach it and conclude that every disease which ever had a control
--    arm was implied by having one. The lists below are BaselineSelection's, copied here because
--    this file runs outside the application; the Java harvest reads them from the class itself so
--    there is one list and not two.

-- 🛑 AFTER RUNNING THIS, FLUSH THE QUERY CACHE. The relation reads are Hibernate-cacheable, and
-- Hibernate invalidates them on writes it performs itself -- which these are not. A rebuild done in
-- SQL leaves every already-warmed query answering from the pre-rebuild result set, so the rows you
-- just deleted keep being served and only the queries nobody had run yet look correct. Verified the
-- hard way on 2026-08-17: `reference subject role` kept returning rows after its 1,967 rows were
-- deleted, while `control` and `wild type genotype` -- never queried before -- were already right.
--
--     DELETE /rest/v2/admin/caches/default-query-results-region
--     DELETE /rest/v2/admin/caches/default-update-timestamps-region
--
-- (admin auth; the unified cache surface, see AdminWebService.)
--
-- 🛑 THE CLI PATH NEEDS THIS TOO. An earlier version of this note said it did not, on the grounds
-- that updateAnnotationRelationEntries writes through Hibernate and synchronizes the query space.
-- It synchronizes THE CLI'S SessionFactory. gemma-cli runs outside the container, in its own JVM,
-- with its own caches; the REST server is a different JVM and learns nothing from that write. Seen
-- on 2026-08-18: after the producer rebuilt CLO from 2,262 rows to 8,410, MEC-1 read 0 through a
-- query that had been warmed before the run and 1 through one that had not. Flush after ANY rebuild,
-- by SQL or by CLI.

-- Expect roughly: 10,040 datasets carry a GENO_0000222 has_genotype statement, 1,829 an
-- RO_0002573 has modifier, 469 a TGEMO_00171 induced by. Row counts run higher than dataset counts,
-- since a dataset may carry several.
SELECT COUNT(*) AS ee2c_rows_with_a_predicate
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC
WHERE OBJECT IS NOT NULL AND (PREDICATE IS NOT NULL OR PREDICATE_URI IS NOT NULL);

DELETE FROM ANNOTATION_RELATION WHERE BASIS = 'CURATED';

-- First clause of the statement.
INSERT INTO ANNOTATION_RELATION (SUBJECT_VALUE, SUBJECT_VALUE_URI, SUBJECT_CATEGORY, SUBJECT_CATEGORY_URI,
                                 PREDICATE, PREDICATE_URI, OBJECT_VALUE, OBJECT_VALUE_URI,
                                 TAXON_FK, BASIS, EVIDENCE_CODE, EXPRESSION_EXPERIMENT_FK, `LEVEL`,
                                 ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, GENERATED_AT)
SELECT C.`VALUE`,
       NULLIF(TRIM(C.VALUE_URI), ''),
       -- The category belongs to the SUBJECT. A statement has one category, and inventing one for
       -- the object would assert something the curator did not -- hence no OBJECT_CATEGORY here.
       C.CATEGORY,
       NULLIF(TRIM(C.CATEGORY_URI), ''),
       C.PREDICATE,
       NULLIF(TRIM(C.PREDICATE_URI), ''),
       C.OBJECT,
       NULLIF(TRIM(C.OBJECT_URI), ''),
       -- Taxon is part of the grain: it is what decides whether a genotype MODELS a disease or HAS
       -- it. Null where the experiment has none, which reads as the weaker claim.
       I.TAXON_FK,
       'CURATED',
       C.EVIDENCE_CODE,
       C.EXPRESSION_EXPERIMENT_FK,
       C.`LEVEL`,
       -- Carried, not recomputed, so support can be counted behind the caller's ACL at read time
       -- without joining the ACL tables per query.
       C.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK,
       NOW(3)
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC C
         JOIN INVESTIGATION I ON I.ID = C.EXPRESSION_EXPERIMENT_FK
-- Predicate-agnostic on purpose, with ONE carve-out. An allow-list would need maintaining in step
-- with the curators' vocabulary and would silently drop whatever was added to it last. But predicates
-- whose OBJECT is a QUANTITY relate no two concepts and are excluded: `delivered at dose` (6,039 rows,
-- objects `10 uM` 497, `1 uM` 311, `100 nM` 142, `10 mg/kg` 67), `delivered for duration` (3,231),
-- `sampled after` (334) and the ungrounded label `timepoint` (2) -- 9,606 of 36,073, a quarter of the
-- harvest that no reader can use and every reader had to filter. Relation.terms.txt already says so:
-- it opens "Terms usable for relations among CONCEPTS" and then lists three of them.
-- The Java harvest reads this list from RelationTopicality so there is one list and not two.
WHERE NULLIF(TRIM(C.OBJECT), '') IS NOT NULL
  AND (NULLIF(TRIM(C.PREDICATE), '') IS NOT NULL OR NULLIF(TRIM(C.PREDICATE_URI), '') IS NOT NULL)
  AND C.OBJECT NOT IN (
      'baseline participant role','baseline','control diet','control group','control',
      'initial time point','normal','placebo','reference subject role','reference substance role',
      'to be treated with placebo role','untreated','wild type control','wild type genotype',
      'wild type','control role','negative control role','normal control group','normal littermate',
      'normal littermates')
  AND (C.OBJECT_URI IS NULL OR C.OBJECT_URI NOT IN (
      'http://purl.obolibrary.org/obo/OBI_0000025','http://purl.obolibrary.org/obo/OBI_0000143',
      'http://purl.obolibrary.org/obo/OBI_0000220','http://purl.obolibrary.org/obo/OBI_0000825',
      'http://purl.obolibrary.org/obo/OBI_0100046','http://www.ebi.ac.uk/efo/EFO_0001461',
      'http://www.ebi.ac.uk/efo/EFO_0001674','http://www.ebi.ac.uk/efo/EFO_0004425',
      'http://www.ebi.ac.uk/efo/EFO_0005168'))
  AND (C.PREDICATE_URI IS NULL OR C.PREDICATE_URI NOT IN (
      'http://gemma.msl.ubc.ca/ont/TGEMO_00166','http://gemma.msl.ubc.ca/ont/TGEMO_00167',
      'http://gemma.msl.ubc.ca/ont/TGEMO_00202'))
  AND (C.PREDICATE IS NULL OR TRIM(C.PREDICATE) NOT IN (
      'delivered at dose','delivered for duration','sampled after','timepoint'));

-- Second clause. A statement can carry two predicate/object pairs and the second is not decoration:
-- the second half of anything a curator expressed as two clauses rides there. (A dose or a duration
-- often does too, and is excluded by the same quantity filter as the first clause.)
INSERT INTO ANNOTATION_RELATION (SUBJECT_VALUE, SUBJECT_VALUE_URI, SUBJECT_CATEGORY, SUBJECT_CATEGORY_URI,
                                 PREDICATE, PREDICATE_URI, OBJECT_VALUE, OBJECT_VALUE_URI,
                                 TAXON_FK, BASIS, EVIDENCE_CODE, EXPRESSION_EXPERIMENT_FK, `LEVEL`,
                                 ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, GENERATED_AT)
SELECT C.`VALUE`,
       NULLIF(TRIM(C.VALUE_URI), ''),
       C.CATEGORY,
       NULLIF(TRIM(C.CATEGORY_URI), ''),
       C.SECOND_PREDICATE,
       NULLIF(TRIM(C.SECOND_PREDICATE_URI), ''),
       C.SECOND_OBJECT,
       NULLIF(TRIM(C.SECOND_OBJECT_URI), ''),
       I.TAXON_FK,
       'CURATED',
       C.EVIDENCE_CODE,
       C.EXPRESSION_EXPERIMENT_FK,
       C.`LEVEL`,
       C.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK,
       NOW(3)
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC C
         JOIN INVESTIGATION I ON I.ID = C.EXPRESSION_EXPERIMENT_FK
WHERE NULLIF(TRIM(C.SECOND_OBJECT), '') IS NOT NULL
  AND (NULLIF(TRIM(C.SECOND_PREDICATE), '') IS NOT NULL OR NULLIF(TRIM(C.SECOND_PREDICATE_URI), '') IS NOT NULL)
  AND C.SECOND_OBJECT NOT IN (
      'baseline participant role','baseline','control diet','control group','control',
      'initial time point','normal','placebo','reference subject role','reference substance role',
      'to be treated with placebo role','untreated','wild type control','wild type genotype',
      'wild type','control role','negative control role','normal control group','normal littermate',
      'normal littermates')
  AND (C.SECOND_OBJECT_URI IS NULL OR C.SECOND_OBJECT_URI NOT IN (
      'http://purl.obolibrary.org/obo/OBI_0000025','http://purl.obolibrary.org/obo/OBI_0000143',
      'http://purl.obolibrary.org/obo/OBI_0000220','http://purl.obolibrary.org/obo/OBI_0000825',
      'http://purl.obolibrary.org/obo/OBI_0100046','http://www.ebi.ac.uk/efo/EFO_0001461',
      'http://www.ebi.ac.uk/efo/EFO_0001674','http://www.ebi.ac.uk/efo/EFO_0004425',
      'http://www.ebi.ac.uk/efo/EFO_0005168'))
  AND (C.SECOND_PREDICATE_URI IS NULL OR C.SECOND_PREDICATE_URI NOT IN (
      'http://gemma.msl.ubc.ca/ont/TGEMO_00166','http://gemma.msl.ubc.ca/ont/TGEMO_00167',
      'http://gemma.msl.ubc.ca/ont/TGEMO_00202'))
  AND (C.SECOND_PREDICATE IS NULL OR TRIM(C.SECOND_PREDICATE) NOT IN (
      'delivered at dose','delivered for duration','sampled after','timepoint'));

-- What landed, by predicate. Sanity: has_genotype should dominate.
SELECT PREDICATE,
       PREDICATE_URI,
       COUNT(*)                              AS rows_written,
       COUNT(DISTINCT EXPRESSION_EXPERIMENT_FK) AS datasets,
       COUNT(DISTINCT OBJECT_VALUE)          AS distinct_objects
FROM ANNOTATION_RELATION
WHERE BASIS = 'CURATED'
GROUP BY PREDICATE, PREDICATE_URI
ORDER BY rows_written DESC;

-- The question that had no query before this: which manipulations or genotypes are asserted against
-- a given disease? Read from the SUBJECT end, which is where a curated statement puts the disease.
SELECT SUBJECT_VALUE,
       PREDICATE,
       OBJECT_VALUE,
       COUNT(DISTINCT EXPRESSION_EXPERIMENT_FK) AS datasets
FROM ANNOTATION_RELATION
WHERE BASIS = 'CURATED'
  AND SUBJECT_CATEGORY IN ('disease', 'disease model')
GROUP BY SUBJECT_VALUE, PREDICATE, OBJECT_VALUE
ORDER BY datasets DESC
LIMIT 40;

-- 🛑 GENERIC OBJECTS. An object related to MANY distinct subjects identifies nothing, and a gate
-- seeded with it implies all of them. `Heterozygous` (GENO_0000135) and `Homozygous negative`
-- (TGEMO_00001) reach every disease that ever had such an arm; `surgical manipulation` reaches every
-- surgically induced one. This is the object-side of the same argument that says support is not
-- evidence -- breadth is what separates `MPTP` from `surgical manipulation`, and it is measurable
-- rather than a list somebody has to maintain.
SELECT OBJECT_VALUE,
       OBJECT_VALUE_URI,
       COUNT(DISTINCT SUBJECT_VALUE)            AS distinct_subjects,
       COUNT(DISTINCT EXPRESSION_EXPERIMENT_FK) AS datasets
FROM ANNOTATION_RELATION
WHERE BASIS = 'CURATED'
GROUP BY OBJECT_VALUE, OBJECT_VALUE_URI
ORDER BY distinct_subjects DESC
LIMIT 30;
