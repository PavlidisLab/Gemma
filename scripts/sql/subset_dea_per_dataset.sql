-- ---------------------------------------------------------------------------------------
-- Does a dataset have a SUBSET differential expression analysis run on it?
--
-- Verified against prod `gemd` 2026-08-15 (read-only tunnel, 127.0.0.1:8000).
--
-- WHAT A "SUBSET DEA" IS
--
-- When Gemma runs a DEA with a subset factor, it splits the experiment by that factor's
-- levels, materializes one ExpressionExperimentSubSet per level, and runs a SEPARATE
-- analysis on each. So a subset run is not one analysis with a flag on it -- it is N
-- analyses, each pointing at a different subset of the same source experiment.
--
-- In the schema (all single-table inheritance, discriminated by the `class` column):
--
--   ANALYSIS.class = 'DifferentialExpressionAnalysis'   -- the DEA rows
--   ANALYSIS.EXPERIMENT_ANALYZED_FK -> INVESTIGATION.ID -- an EE *or* a subset
--   INVESTIGATION.class = 'ExpressionExperiment' | 'ExpressionExperimentSubSet'
--   INVESTIGATION.SOURCE_EXPERIMENT_FK                  -- subset -> its parent EE
--   ANALYSIS.SUBSET_FACTOR_VALUE_FK -> FACTOR_VALUE.ID  -- which level this subset is
--
-- THE TEST IS `analyzed.class = 'ExpressionExperimentSubSet'`, not the subset FV column.
-- The two signals almost always agree, but not quite -- prod cross-tab of the 33,213 DEAs:
--
--   analyzed class                 SUBSET_FACTOR_VALUE_FK set?   n
--   ExpressionExperiment           no                            19177
--   ExpressionExperimentSubSet     yes                           14035
--   ExpressionExperimentSubSet     no                                1   <-- analysis 320189
--
-- That single outlier (GSE5285, "Subset for FactorValue 16223: organism part:cortex of
-- kidney") is unmistakably a subset analysis with a null FK. Keying on the class column
-- catches it; keying on SUBSET_FACTOR_VALUE_FK IS NOT NULL silently drops it. Use the class.
--
-- TWO THINGS THAT LOOK LIKE THIS SIGNAL AND ARE NOT
--
-- 1. "The dataset has subsets." Prod has 47,127 subsets and 33,123 of them have NO DEA --
--    subsets get created for other reasons (single-cell cell-type aggregation, etc.).
--    219 datasets have subsets but no subset DEA. Existence of a subset proves nothing.
--
-- 2. ">1 DEA means it's a subset run." A decent rule of thumb, wrong both ways. Measured:
--
--                          has subset DEA    no subset DEA
--        >1 DEA                     3496               60
--        exactly 1 DEA               420            18775
--
--    The 420 are subset runs MISSING THEIR SIBLINGS. Checked: of the 476 datasets carrying
--    exactly one subset DEA, every single one has a subset factor with >= 2 levels
--    (428 have 2 levels, the rest up to 16) -- so the run necessarily produced >= 2
--    analyses and only one survives. They are not single-level factors. Only 24 still have
--    an orphaned sibling subset on disk, so the deleted analysis usually took its subset
--    with it. Consistent with an analysis being deleted after the fact as useless.
--
--    The 60 are datasets carrying multiple WHOLE-EXPERIMENT analyses. 51 of the 60 are
--    nested re-analyses -- one analysis on a narrow factor set plus a wider one adding a
--    covariate (age, biological sex, developmental stage, collection of material). The
--    other 9 have byte-identical factor sets and look like genuine duplicates. See
--    docs/recce/MULTI_DEA_NON_SUBSET_DATASETS.md for the full table.
--
--    Counting analyses gets ~2.5% of datasets wrong; the class column gets none wrong.
--
-- ACL NOTE: these tables carry no ACL predicate, so this reads the whole corpus including
-- non-public datasets. Fine for admin/analysis use; do not surface the raw output to users.
-- ---------------------------------------------------------------------------------------


-- ========================================================================================
-- QUERY 1 -- the answer, one row per dataset that has ANY differential expression analysis.
--
--   has_subset_dea   1 if at least one DEA ran on a subset of this dataset
--   n_dea            all DEAs attributable to the dataset (its own + its subsets')
--   n_subset_dea     how many of those ran on a subset (== number of subsets analyzed)
--   n_whole_ee_dea   how many ran on the whole experiment
--   subset_factors   the factor(s) the split was made on, e.g. 'cell type', 'treatment'
--
-- Add a WHERE on the outer select, or a HAVING, to filter. Examples at the bottom.
-- ========================================================================================

SELECT ee.ID                                              AS ee_id,
       ee.SHORT_NAME                                      AS short_name,
       ee.NAME                                            AS ee_name,
       SUM(sub.ID IS NOT NULL) > 0                        AS has_subset_dea,
       COUNT(*)                                           AS n_dea,
       SUM(sub.ID IS NOT NULL)                            AS n_subset_dea,
       SUM(sub.ID IS NULL)                                AS n_whole_ee_dea,
       COUNT(DISTINCT ef.ID)                              AS n_subset_factors,
       GROUP_CONCAT(DISTINCT ef.NAME ORDER BY ef.NAME SEPARATOR '; ') AS subset_factors
FROM ANALYSIS a
         -- what the analysis was actually run on: an EE, or a subset of one
         JOIN INVESTIGATION analyzed ON analyzed.ID = a.EXPERIMENT_ANALYZED_FK
         -- non-null only when that thing is a subset -- this join IS the subset test
         LEFT JOIN INVESTIGATION sub ON sub.ID = analyzed.ID
             AND sub.class = 'ExpressionExperimentSubSet'
         -- roll a subset's analysis up to its parent experiment
         JOIN INVESTIGATION ee ON ee.ID = COALESCE(sub.SOURCE_EXPERIMENT_FK, analyzed.ID)
         -- which factor level defined the subset (null on the one outlier above)
         LEFT JOIN FACTOR_VALUE sfv ON sfv.ID = a.SUBSET_FACTOR_VALUE_FK
         LEFT JOIN EXPERIMENTAL_FACTOR ef ON ef.ID = sfv.EXPERIMENTAL_FACTOR_FK
WHERE a.class = 'DifferentialExpressionAnalysis'
GROUP BY ee.ID, ee.SHORT_NAME, ee.NAME
ORDER BY n_subset_dea DESC, ee.SHORT_NAME;

-- Datasets with NO DEA at all are absent from the above (they cannot have a subset DEA).
-- If you need every dataset in the corpus with a 0/1 flag, use QUERY 3.


-- ========================================================================================
-- QUERY 2 -- single dataset, yes/no. Swap the accession.
-- ========================================================================================

SELECT ee.SHORT_NAME,
       EXISTS (
           SELECT 1
           FROM ANALYSIS a
                    JOIN INVESTIGATION sub ON sub.ID = a.EXPERIMENT_ANALYZED_FK
                        AND sub.class = 'ExpressionExperimentSubSet'
           WHERE a.class = 'DifferentialExpressionAnalysis'
             AND sub.SOURCE_EXPERIMENT_FK = ee.ID
       ) AS has_subset_dea
FROM INVESTIGATION ee
WHERE ee.class = 'ExpressionExperiment'
  AND ee.SHORT_NAME = 'GSE96760';


-- ========================================================================================
-- QUERY 3 -- flag every experiment in the corpus, including those with no DEA.
-- Use this one when you are joining the flag onto another per-dataset table.
-- ========================================================================================

SELECT ee.ID AS ee_id,
       ee.SHORT_NAME,
       EXISTS (
           SELECT 1
           FROM ANALYSIS a
                    JOIN INVESTIGATION sub ON sub.ID = a.EXPERIMENT_ANALYZED_FK
                        AND sub.class = 'ExpressionExperimentSubSet'
           WHERE a.class = 'DifferentialExpressionAnalysis'
             AND sub.SOURCE_EXPERIMENT_FK = ee.ID
       ) AS has_subset_dea
FROM INVESTIGATION ee
WHERE ee.class = 'ExpressionExperiment'
ORDER BY ee.SHORT_NAME;


-- ========================================================================================
-- QUERY 4 -- drill down: the individual subset analyses for one dataset, with the factor
-- value each one represents. This is what QUERY 1 aggregates over.
-- ========================================================================================

SELECT a.ID                       AS analysis_id,
       sub.ID                     AS subset_id,
       sub.NAME                   AS subset_name,
       ef.NAME                    AS subset_factor,
       ef.TYPE                    AS subset_factor_type,
       COALESCE(c.`VALUE`, m.`VALUE`, CONCAT('FactorValue ', sfv.ID)) AS subset_level
FROM ANALYSIS a
         JOIN INVESTIGATION sub ON sub.ID = a.EXPERIMENT_ANALYZED_FK
             AND sub.class = 'ExpressionExperimentSubSet'
         JOIN INVESTIGATION ee ON ee.ID = sub.SOURCE_EXPERIMENT_FK
         LEFT JOIN FACTOR_VALUE sfv ON sfv.ID = a.SUBSET_FACTOR_VALUE_FK
         LEFT JOIN EXPERIMENTAL_FACTOR ef ON ef.ID = sfv.EXPERIMENTAL_FACTOR_FK
         -- a factor value is either categorical (characteristics) or a measurement
         LEFT JOIN CHARACTERISTIC c ON c.FACTOR_VALUE_FK = sfv.ID
         LEFT JOIN MEASUREMENT m ON m.ID = sfv.MEASUREMENT_FK
WHERE a.class = 'DifferentialExpressionAnalysis'
  AND ee.SHORT_NAME = 'GSE96760'
ORDER BY subset_level;


-- ========================================================================================
-- Filters to paste onto QUERY 1
-- ========================================================================================
--
--   only subset datasets            HAVING n_subset_dea > 0
--   only non-subset datasets        HAVING n_subset_dea = 0
--   the 60 false positives of the   HAVING n_subset_dea = 0 AND n_dea > 1
--     ">1 DEA" heuristic
--   the 420 false negatives         HAVING n_subset_dea > 0 AND n_dea = 1
--   heavily split datasets          HAVING n_subset_dea >= 20
--   split on more than one factor   HAVING n_subset_factors > 1
--
-- ========================================================================================
-- Running it
-- ========================================================================================
--
--   MYSQL_PWD=$(security find-generic-password -s mysql-gemd-pavlab-8000 -a gemmaadmin -w) \
--     mysql -h 127.0.0.1 -P 8000 -u gemmaadmin gemd < scripts/sql/subset_dea_per_dataset.sql
--
-- 127.0.0.1, not localhost -- the client treats `localhost` as a unix socket and ignores
-- -P, which quietly connects you to a local mysqld instead of the prod tunnel.
