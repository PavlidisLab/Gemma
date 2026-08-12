-- ---------------------------------------------------------------------------------------
-- Re-sync EXPRESSION_EXPERIMENT2CHARACTERISTIC's denormalized text from CHARACTERISTIC.
--
-- WHY THIS EXISTS: `gemma-cli updateEe2c` DOES NOT FIX THESE ROWS.
--
-- Measured on prod 2026-08-12, immediately after a full `updateEe2c` run that reported
-- 2,556,578 entries updated against a 2,547,341-row table: 1,008 EE2C rows still disagreed
-- with the CHARACTERISTIC row they point at.
--
--     36  EE2C.VALUE is NULL while the characteristic has a value
--    146  whitespace-only drift (the pre-cleanup spelling, left behind by
--         normalize_characteristic_whitespace.sql -- see that script's step 4)
--    732  text differing for older, unrelated reasons
--     94  CATEGORY / OBJECT / SECOND_OBJECT disagreements
--
-- The cause is structural, not a bad run. TableMaintenanceUtil's rebuild is
--     INSERT ... SELECT ... GROUP BY I.ID, COALESCE(CATEGORY_URI,CATEGORY), COALESCE(VALUE_URI,VALUE)
--     ON DUPLICATE KEY UPDATE ...
-- so each (experiment, category, value) group emits exactly ONE characteristic id. Any other
-- characteristic in that group has an EE2C row that the upsert never touches -- and never
-- deletes, because an upsert cannot delete. Those rows keep whatever text they were written
-- with, forever. Collapsing whitespace MERGES groups, which is why a cleanup creates fresh
-- orphans: the loser drops out of the SELECT the moment its value matches the winner's.
--
-- `updateEe2c --truncate` is the only path that would clear them, and it is not usable:
-- it binds a Class<?> against the varchar LEVEL column, matches nothing when no --level is
-- given, and has zero test coverage (TableMaintenanceUtilIntegrationTest). Verify it before
-- trusting it; until then, this script is the remedy.
--
-- WHAT THIS DOES: copies the four text columns across the ID FK. EE2C.ID is a FK to
-- CHARACTERISTIC.ID (EE2C_CHARACTERISTIC_FKC), so the join is exact and one-to-one -- this
-- cannot mix up rows the way a value/URI-matched update could. Nothing is inserted or
-- deleted, so no annotation can be lost.
--
-- WHAT THIS DOES NOT DO: it does not remove the duplicate EE2C rows a group merge leaves
-- behind (140 of the 146 whitespace rows have a clean sibling on the same experiment).
-- Those are cosmetic -- read paths dedup, ExpressionExperimentReadServiceImpl#addIfNovel
-- normalizes -- and deleting them is a separate, riskier decision: 7 of the 146 have NO
-- clean sibling, so a blanket DELETE would drop the annotation from EE2C entirely.
--
-- The URI columns are deliberately NOT copied. A URI drift is a curation question, not a
-- denormalization bug, and the 2026-08-08 efo-base incident is what happens when a script
-- rewrites URIs in bulk. Text only.
--
-- Run AFTER `updateEe2c`, not instead of it -- the CLI is what inserts rows for new
-- annotations; this only repairs rows it left stale. Reindex after (see step 3).
-- ---------------------------------------------------------------------------------------

-- Step 1 -- PREFLIGHT. Count and classify. Record these before applying.
SELECT 'total disagreeing' AS chk, COUNT(*) AS n
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC e JOIN CHARACTERISTIC c ON c.ID = e.ID
WHERE NOT (BINARY e.`VALUE`         <=> BINARY c.`VALUE`)
   OR NOT (BINARY e.CATEGORY        <=> BINARY c.CATEGORY)
   OR NOT (BINARY e.OBJECT          <=> BINARY c.OBJECT)
   OR NOT (BINARY e.SECOND_OBJECT   <=> BINARY c.SECOND_OBJECT)
UNION ALL
SELECT 'VALUE null in EE2C only', COUNT(*)
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC e JOIN CHARACTERISTIC c ON c.ID = e.ID
WHERE e.`VALUE` IS NULL AND c.`VALUE` IS NOT NULL
UNION ALL
SELECT 'whitespace-only drift', COUNT(*)
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC e JOIN CHARACTERISTIC c ON c.ID = e.ID
WHERE NOT (BINARY e.`VALUE` <=> BINARY c.`VALUE`)
  AND e.`VALUE` IS NOT NULL AND c.`VALUE` IS NOT NULL
  AND TRIM(REPLACE(REPLACE(REPLACE(REPLACE(e.`VALUE`,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.`VALUE`,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '));

-- Eyeball 20 before / after pairs.
SELECT e.ID, e.EXPRESSION_EXPERIMENT_FK AS ee,
       CONCAT('[', IFNULL(e.`VALUE`,'<NULL>'), ']') AS ee2c_now,
       CONCAT('[', IFNULL(c.`VALUE`,'<NULL>'), ']') AS characteristic_says
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC e JOIN CHARACTERISTIC c ON c.ID = e.ID
WHERE NOT (BINARY e.`VALUE` <=> BINARY c.`VALUE`)
LIMIT 20;

-- Step 2 -- APPLY. Check the row count against step 1 before COMMIT.
START TRANSACTION;

UPDATE EXPRESSION_EXPERIMENT2CHARACTERISTIC e
  JOIN CHARACTERISTIC c ON c.ID = e.ID
   SET e.`VALUE`       = c.`VALUE`,
       e.CATEGORY      = c.CATEGORY,
       e.OBJECT        = c.OBJECT,
       e.SECOND_OBJECT = c.SECOND_OBJECT
 WHERE NOT (BINARY e.`VALUE`       <=> BINARY c.`VALUE`)
    OR NOT (BINARY e.CATEGORY      <=> BINARY c.CATEGORY)
    OR NOT (BINARY e.OBJECT        <=> BINARY c.OBJECT)
    OR NOT (BINARY e.SECOND_OBJECT <=> BINARY c.SECOND_OBJECT);

-- POSTFLIGHT -- inside the transaction, so a bad count can still be rolled back. Must be 0.
SELECT 'still disagreeing (must be 0)' AS chk, COUNT(*) AS n
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC e JOIN CHARACTERISTIC c ON c.ID = e.ID
WHERE NOT (BINARY e.`VALUE`         <=> BINARY c.`VALUE`)
   OR NOT (BINARY e.CATEGORY        <=> BINARY c.CATEGORY)
   OR NOT (BINARY e.OBJECT          <=> BINARY c.OBJECT)
   OR NOT (BINARY e.SECOND_OBJECT   <=> BINARY c.SECOND_OBJECT);

-- COMMIT;      -- uncomment once the counts look right
-- ROLLBACK;    -- if they do not

-- Step 3 -- AFTERWARDS. Lucene still holds the old strings; raw SQL does not touch it:
--
--     POST /rest/v2/admin/search/indices?entity=datasets     (admin, 202, ~3 min)
--
-- Watch GET /rest/v2/admin/search/indices for a fresh `lastModified` and a plateaued
-- `documentCount`. Do NOT poll `reindexStatus` -- it is not in the serialized VO and always
-- reads null.
--
-- Then evict the EE2C query cache on the instance you actually care about:
--
--     GET /rest/v2/datasets/annotations/refresh             (admin, 201)
--
-- 🛑 `updateEe2c` calls that refresh itself, but against `gemma.hosturl`, which defaults to
-- https://gemma.msl.ubc.ca (default.properties). On a Gemma 2.0 box that is the WRONG
-- instance, and the CLI logs "Refreshed all EE2C associations from ..." either way because
-- it discards the response and GemmaRestApiClientImpl returns an error object rather than
-- throwing. Set GEMMA_HOSTURL, or fire the refresh by hand.
