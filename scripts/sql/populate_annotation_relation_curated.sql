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
       C.VALUE_URI,
       -- The category belongs to the SUBJECT. A statement has one category, and inventing one for
       -- the object would assert something the curator did not -- hence no OBJECT_CATEGORY here.
       C.CATEGORY,
       C.CATEGORY_URI,
       C.PREDICATE,
       C.PREDICATE_URI,
       C.OBJECT,
       C.OBJECT_URI,
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
-- Predicate-agnostic on purpose. An allow-list would need maintaining in step with the curators'
-- vocabulary and would silently drop whatever was added to it last.
WHERE C.OBJECT IS NOT NULL
  AND (C.PREDICATE IS NOT NULL OR C.PREDICATE_URI IS NOT NULL);

-- Second clause. A statement can carry two predicate/object pairs and the second is not decoration:
-- a dose or a duration rides there, as does the second half of anything expressed as two clauses.
INSERT INTO ANNOTATION_RELATION (SUBJECT_VALUE, SUBJECT_VALUE_URI, SUBJECT_CATEGORY, SUBJECT_CATEGORY_URI,
                                 PREDICATE, PREDICATE_URI, OBJECT_VALUE, OBJECT_VALUE_URI,
                                 TAXON_FK, BASIS, EVIDENCE_CODE, EXPRESSION_EXPERIMENT_FK, `LEVEL`,
                                 ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, GENERATED_AT)
SELECT C.`VALUE`,
       C.VALUE_URI,
       C.CATEGORY,
       C.CATEGORY_URI,
       C.SECOND_PREDICATE,
       C.SECOND_PREDICATE_URI,
       C.SECOND_OBJECT,
       C.SECOND_OBJECT_URI,
       I.TAXON_FK,
       'CURATED',
       C.EVIDENCE_CODE,
       C.EXPRESSION_EXPERIMENT_FK,
       C.`LEVEL`,
       C.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK,
       NOW(3)
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC C
         JOIN INVESTIGATION I ON I.ID = C.EXPRESSION_EXPERIMENT_FK
WHERE C.SECOND_OBJECT IS NOT NULL
  AND (C.SECOND_PREDICATE IS NOT NULL OR C.SECOND_PREDICATE_URI IS NOT NULL);

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
