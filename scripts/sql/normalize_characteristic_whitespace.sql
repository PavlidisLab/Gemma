-- Collapse whitespace on the free-text term columns of CHARACTERISTIC.
--
-- Companion to bb88025b5c, which normalizes the same fields at the setters so new writes
-- arrive clean. This is the one-off cleanup for rows written before that.
--
-- Matches Java's StringUtils.normalizeSpace exactly: tabs become spaces, internal runs
-- collapse to one space, ends are trimmed. A space-only collapse would NOT match it --
-- 1,014 VALUE rows contain a literal TAB.
--
-- ORIGINAL_VALUE is deliberately untouched. It is the submitter's verbatim string and the
-- only record of what GEO actually sent; normalizing it would destroy the evidence that
-- makes the VALUE normalization safe.
--
--   Target: prod `gemd`. MySQL 5.7 (no REGEXP_REPLACE -- hence the marker-byte idiom).
--   Verified 2026-08-11: no row in these columns contains CHAR(1) or CHAR(2), so the
--   markers cannot collide with real data. Re-check (step 0) before running.
--
-- Rows affected, measured BEFORE the scoping clause below existed:
--   VALUE 14,174 | CATEGORY 8,866 | OBJECT 157 | SECOND_OBJECT 32
-- Those numbers included rows this script must NOT touch. Re-measure at step 1.
--
-- ---------------------------------------------------------------------------------------
-- CHARACTERISTIC IS A SHARED TABLE -- SCOPE EVERY WRITE (read this before copying this file)
--
-- CHARACTERISTIC does not hold only experiment annotations. Gene-GO annotations live here
-- too: GENE2GO_ASSOCIATION.ONTOLOGY_ENTRY_FK -> CHARACTERISTIC.ID (Gene2GOAssociation.java).
-- So do gene sets, phenotype associations and bibliographic-reference terms. Gene-GO rows
-- carry NO owner FK at all -- they are owned from the other side -- so a WHERE clause that
-- matches only on the text or the URI sweeps them in silently.
--
-- That is not hypothetical. The prod remap of the efo-base casualties (2026-08-08,
-- willie:~/Gemma2.0/remap_unsupported_uris.sql) matched on VALUE_URI alone and rewrote every
-- gene annotated with GO_0007565 "female pregnancy" to EFO_0002950 "pregnancy". Ogan repaired
-- it 2026-08-11. Prior art with the same shape: sql/migrations/db.1.27.13.sql (the TGEMO
-- prefix rewrite -- harmless only because TGEMO is not a GO namespace).
--
-- The scope is therefore defined ONCE, as a temp table of in-scope IDs, and every statement
-- JOINs to it. Do not re-express it as `AND (...)` appended to a predicate: the WHERE clauses
-- here are top-level ORs, and `A OR B AND scope` parses as `A OR (B AND scope)` -- which
-- silently un-scopes half of every statement. A JOIN cannot be got wrong that way.
-- ---------------------------------------------------------------------------------------

-- Step 0 -- SAFETY CHECK. Must return 0,0 or the marker idiom is unsafe; stop if not.
-- Deliberately whole-table: a marker byte anywhere makes the idiom unsafe, in scope or not.
SELECT
  SUM(VALUE LIKE CONCAT('%',CHAR(1),'%') OR CATEGORY LIKE CONCAT('%',CHAR(1),'%')
      OR OBJECT LIKE CONCAT('%',CHAR(1),'%') OR SECOND_OBJECT LIKE CONCAT('%',CHAR(1),'%')) AS has_x01,
  SUM(VALUE LIKE CONCAT('%',CHAR(2),'%') OR CATEGORY LIKE CONCAT('%',CHAR(2),'%')
      OR OBJECT LIKE CONCAT('%',CHAR(2),'%') OR SECOND_OBJECT LIKE CONCAT('%',CHAR(2),'%')) AS has_x02
FROM CHARACTERISTIC;

-- Step 0b -- BUILD THE SCOPE. Experiment-owned characteristics only.
--
-- Every owner FK an experiment annotation can hang from. A row with all of these NULL
-- belongs to something else (gene-GO, gene set, phenotype association, bibliographic
-- reference) and is out of bounds for a curation cleanup.
DROP TEMPORARY TABLE IF EXISTS _ws_scope;
CREATE TEMPORARY TABLE _ws_scope (ID BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB;

INSERT INTO _ws_scope (ID)
SELECT ID FROM CHARACTERISTIC
 WHERE BIO_MATERIAL_FK               IS NOT NULL
    OR INVESTIGATION_FK              IS NOT NULL
    OR EXPERIMENTAL_DESIGN_FK        IS NOT NULL
    OR EXPERIMENTAL_FACTOR_FK        IS NOT NULL
    OR FACTOR_VALUE_FK               IS NOT NULL
    OR CELL_TYPE_ASSIGNMENT_FK       IS NOT NULL
    OR CELL_LEVEL_CHARACTERISTICS_FK IS NOT NULL;

-- Sanity: how much of the table is in scope, and confirm no gene-GO row got in.
SELECT (SELECT COUNT(*) FROM _ws_scope)        AS in_scope,
       (SELECT COUNT(*) FROM CHARACTERISTIC)   AS total,
       (SELECT COUNT(*) FROM _ws_scope s
          JOIN GENE2GO_ASSOCIATION g ON g.ONTOLOGY_ENTRY_FK = s.ID) AS gene_go_leaked;  -- must be 0

-- Step 1 -- DRY RUN. Row counts per column, and a sample of what changes.
SELECT 'VALUE' AS col, COUNT(*) AS rows_affected FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.VALUE <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.VALUE) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
UNION ALL
SELECT 'CATEGORY', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.CATEGORY <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.CATEGORY) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
UNION ALL
SELECT 'OBJECT', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
UNION ALL
SELECT 'SECOND_OBJECT', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.SECOND_OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.SECOND_OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

-- Eyeball 20 before / after pairs.
SELECT c.ID,
       CONCAT('[', c.VALUE, ']') AS before_val,
       CONCAT('[', TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')), ']') AS after_val
FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
WHERE c.VALUE <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
   OR CHAR_LENGTH(c.VALUE) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
LIMIT 20;

-- Step 2 -- APPLY. One transaction; check the row counts against step 1 before COMMIT.
START TRANSACTION;

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.VALUE = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.VALUE <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.VALUE) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.CATEGORY = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.CATEGORY <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.CATEGORY) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.SECOND_OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.SECOND_OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(c.SECOND_OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(c.SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

-- Step 3 -- POSTFLIGHT. Every count must be 0.
-- NOTE: this now runs INSIDE the transaction, before the commit, so a bad count can still be
-- rolled back. The earlier version verified after committing.
SELECT 'VALUE dirty after' AS chk, COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE CHAR_LENGTH(c.VALUE) <> CHAR_LENGTH(TRIM(c.VALUE)) OR c.VALUE LIKE '%  %' OR c.VALUE LIKE CONCAT('%',CHAR(9),'%')
UNION ALL
SELECT 'CATEGORY dirty after', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE CHAR_LENGTH(c.CATEGORY) <> CHAR_LENGTH(TRIM(c.CATEGORY)) OR c.CATEGORY LIKE '%  %' OR c.CATEGORY LIKE CONCAT('%',CHAR(9),'%')
UNION ALL
SELECT 'OBJECT dirty after', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE CHAR_LENGTH(c.OBJECT) <> CHAR_LENGTH(TRIM(c.OBJECT)) OR c.OBJECT LIKE '%  %' OR c.OBJECT LIKE CONCAT('%',CHAR(9),'%')
UNION ALL
SELECT 'SECOND_OBJECT dirty after', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE CHAR_LENGTH(c.SECOND_OBJECT) <> CHAR_LENGTH(TRIM(c.SECOND_OBJECT)) OR c.SECOND_OBJECT LIKE '%  %' OR c.SECOND_OBJECT LIKE CONCAT('%',CHAR(9),'%');

-- COMMIT;      -- uncomment once the counts look right
-- ROLLBACK;    -- if they do not

-- ---------------------------------------------------------------------------------------
-- NO-BREAK SPACES. Separate pass: TRIM/CHAR(9) do not see U+00A0, U+202F, U+2007.
-- Same scope, same transaction discipline.
--
--   U+00A0 non-breaking space  : 2,392 VALUE rows, 24 CATEGORY  (utf8 bytes C2 A0)
--   U+202F narrow no-break     : 5 VALUE rows                    (utf8 bytes E2 80 AF)
--   U+2007 figure space        : 0 rows today, folded in for symmetry with the Java guard
--
-- Each statement maps the three to a plain space and THEN re-runs the collapse idiom. It
-- has to: step 2 has already run and could not see these bytes, so substituting without
-- collapsing turns an NBSP run into a run of ordinary double spaces that nothing else
-- catches. The Java guard has the same ordering for the same reason -- Java does not
-- classify U+202F/U+2007 as whitespace at all, and StringUtils.normalizeSpace maps U+00A0
-- to a space without re-collapsing. Drop the collapse here and the two normalizers disagree.
-- ---------------------------------------------------------------------------------------
SELECT 'VALUE with a no-break space' AS chk, COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.VALUE LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.VALUE LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.VALUE LIKE CONCAT('%', _utf8mb4 0xE28087, '%')
UNION ALL
SELECT 'CATEGORY with a no-break space', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.CATEGORY LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.CATEGORY LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.CATEGORY LIKE CONCAT('%', _utf8mb4 0xE28087, '%')
UNION ALL
SELECT 'OBJECT with a no-break space', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.OBJECT LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.OBJECT LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.OBJECT LIKE CONCAT('%', _utf8mb4 0xE28087, '%')
UNION ALL
SELECT 'SECOND_OBJECT with a no-break space', COUNT(*) FROM CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
 WHERE c.SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

START TRANSACTION;

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.VALUE = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(c.VALUE, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                   CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.VALUE LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.VALUE LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.VALUE LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.CATEGORY = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(c.CATEGORY, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                   CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.CATEGORY LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.CATEGORY LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.CATEGORY LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(c.OBJECT, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                   CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.OBJECT LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.OBJECT LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.OBJECT LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

UPDATE CHARACTERISTIC c JOIN _ws_scope s ON s.ID = c.ID
   SET c.SECOND_OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(c.SECOND_OBJECT, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                   CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE c.SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR c.SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR c.SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

-- COMMIT;
-- ROLLBACK;

DROP TEMPORARY TABLE IF EXISTS _ws_scope;

-- Step 4 -- AFTERWARDS. Three things do not follow automatically:
--   * EXPRESSION_EXPERIMENT2CHARACTERISTIC carries its own copy of VALUE / CATEGORY /
--     OBJECT / SECOND_OBJECT for every experiment-level and sample-level characteristic.
--     Raw SQL does not keep it in step, so search and the dataset filters read the OLD
--     strings until it is rebuilt. Do NOT hand-write a lockstep UPDATE -- run the CLI:
--
--         ./gemma-cli updateEe2c
--
--     TableMaintenanceUtil's insert is an ON DUPLICATE KEY UPDATE over exactly these
--     columns, so a full re-run rewrites them from CHARACTERISTIC. Pass no -s/--since.
--
--     🛑 WAITING FOR THE NIGHTLY JOB DOES NOT WORK. Ee2cUpdateJob passes its previous
--     fire time as :since, and the predicate is
--         (CD.LAST_UPDATED is null or :since is null or CD.LAST_UPDATED >= :since)
--     -- CURATION_DETAILS.LAST_UPDATED, which a raw SQL UPDATE never bumps. Every row
--     this script touches is invisible to the scheduled job permanently. Only a manual
--     run with :since null picks them up.
--
--     🛑 THE CLI IS NOT SUFFICIENT ON ITS OWN -- CONFIRMED ON PROD 2026-08-12. After a
--     full run reporting 2,556,578 entries updated, 1,008 EE2C rows still disagreed with
--     their CHARACTERISTIC row, 146 of them carrying exactly the pre-cleanup spelling.
--     The select groups by (EE, COALESCE(CATEGORY_URI,CATEGORY), COALESCE(VALUE_URI,VALUE)),
--     so each group emits ONE characteristic id; the others keep an EE2C row the upsert
--     never touches and cannot delete. Collapsing whitespace merges groups, which is
--     precisely how a cleanup mints new orphans. Follow up with:
--
--         scripts/sql/resync_ee2c_from_characteristic.sql
--
--     `--truncate` would clear them in principle but is not usable: it binds a Class<?>
--     against the varchar LEVEL column, no-ops when no --level is given, and has zero test
--     coverage. Verify it before trusting it.
--
--     The CLI does call /datasets/annotations/refresh itself -- but against `gemma.hosturl`,
--     which defaults to https://gemma.msl.ubc.ca, so on a Gemma 2.0 box it refreshes the
--     WRONG instance and still logs success (it discards the response). Set GEMMA_HOSTURL.
--   * The Hibernate Search index still holds the old strings. Reindex the affected entities
--     (or accept that search rows lag until the next reindex).
--   * Collapsing can leave a sample holding two now-identical characteristics where it
--     previously held 'high  fat diet' and 'high fat diet'. Measured 2026-08-11: of 13,800
--     dirty rows attached to a biomaterial, exactly 24 land on a sample that already carries
--     the clean spelling. Not a constraint violation, and read paths dedup
--     (ExpressionExperimentReadServiceImpl#addIfNovel normalizes), so this is cosmetic --
--     but this finds them if you want to prune:
--
--   SELECT BIO_MATERIAL_FK, VALUE, VALUE_URI, COUNT(*) n
--   FROM CHARACTERISTIC
--   WHERE BIO_MATERIAL_FK IS NOT NULL
--   GROUP BY BIO_MATERIAL_FK, VALUE, VALUE_URI, CATEGORY
--   HAVING n > 1;
