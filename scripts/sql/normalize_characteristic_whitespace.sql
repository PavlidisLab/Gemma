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
-- Expected rows affected:  VALUE 14,174 | CATEGORY 8,866 | OBJECT 157 | SECOND_OBJECT 32
--
-- ---------------------------------------------------------------------------------------
-- WHY `<>` IS NOT ENOUGH IN THE PREDICATE
--
-- MySQL's PAD SPACE collation compares a TRAILING space away, so `VALUE <> TRIM(VALUE)` is
-- FALSE for 'x ' vs 'x' and those rows are silently skipped -- that is how the trailing-only
-- class stayed hidden through an earlier audit that reported 3,419 instead of 4,406. Every
-- predicate below therefore pairs `<>` (catches internal runs) with a CHAR_LENGTH comparison
-- (catches the ends). Keep both.
-- ---------------------------------------------------------------------------------------

-- Step 0 -- SAFETY CHECK. Must return 0,0 or the marker idiom is unsafe; stop if not.
SELECT
  SUM(VALUE LIKE CONCAT('%',CHAR(1),'%') OR CATEGORY LIKE CONCAT('%',CHAR(1),'%')
      OR OBJECT LIKE CONCAT('%',CHAR(1),'%') OR SECOND_OBJECT LIKE CONCAT('%',CHAR(1),'%')) AS has_x01,
  SUM(VALUE LIKE CONCAT('%',CHAR(2),'%') OR CATEGORY LIKE CONCAT('%',CHAR(2),'%')
      OR OBJECT LIKE CONCAT('%',CHAR(2),'%') OR SECOND_OBJECT LIKE CONCAT('%',CHAR(2),'%')) AS has_x02
FROM CHARACTERISTIC;

-- Step 1 -- DRY RUN. Row counts per column, and a sample of what changes.
SELECT 'VALUE' AS col, COUNT(*) AS rows_affected FROM CHARACTERISTIC
 WHERE VALUE <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(VALUE) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
UNION ALL
SELECT 'CATEGORY', COUNT(*) FROM CHARACTERISTIC
 WHERE CATEGORY <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(CATEGORY) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
UNION ALL
SELECT 'OBJECT', COUNT(*) FROM CHARACTERISTIC
 WHERE OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
UNION ALL
SELECT 'SECOND_OBJECT', COUNT(*) FROM CHARACTERISTIC
 WHERE SECOND_OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(SECOND_OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

-- Eyeball 20 before / after pairs.
SELECT ID,
       CONCAT('[', VALUE, ']') AS before_val,
       CONCAT('[', TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')), ']') AS after_val
FROM CHARACTERISTIC
WHERE VALUE <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
   OR CHAR_LENGTH(VALUE) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')))
LIMIT 20;

-- Step 2 -- APPLY. One transaction; check the row counts against step 1 before COMMIT.
START TRANSACTION;

UPDATE CHARACTERISTIC
   SET VALUE = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE VALUE <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(VALUE) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(VALUE,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

UPDATE CHARACTERISTIC
   SET CATEGORY = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE CATEGORY <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(CATEGORY) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(CATEGORY,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

UPDATE CHARACTERISTIC
   SET OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

UPDATE CHARACTERISTIC
   SET SECOND_OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE SECOND_OBJECT <> TRIM(REPLACE(REPLACE(REPLACE(REPLACE(SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
    OR CHAR_LENGTH(SECOND_OBJECT) <> CHAR_LENGTH(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(SECOND_OBJECT,CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' ')));

-- COMMIT;      -- uncomment once the counts look right
-- ROLLBACK;    -- if they do not

-- Step 3 -- VERIFY. Every count must be 0. Re-run step 1 for the same effect.
SELECT 'VALUE dirty after' AS chk, COUNT(*) FROM CHARACTERISTIC
 WHERE CHAR_LENGTH(VALUE) <> CHAR_LENGTH(TRIM(VALUE)) OR VALUE LIKE '%  %' OR VALUE LIKE CONCAT('%',CHAR(9),'%')
UNION ALL
SELECT 'CATEGORY dirty after', COUNT(*) FROM CHARACTERISTIC
 WHERE CHAR_LENGTH(CATEGORY) <> CHAR_LENGTH(TRIM(CATEGORY)) OR CATEGORY LIKE '%  %' OR CATEGORY LIKE CONCAT('%',CHAR(9),'%')
UNION ALL
SELECT 'OBJECT dirty after', COUNT(*) FROM CHARACTERISTIC
 WHERE CHAR_LENGTH(OBJECT) <> CHAR_LENGTH(TRIM(OBJECT)) OR OBJECT LIKE '%  %' OR OBJECT LIKE CONCAT('%',CHAR(9),'%')
UNION ALL
SELECT 'SECOND_OBJECT dirty after', COUNT(*) FROM CHARACTERISTIC
 WHERE CHAR_LENGTH(SECOND_OBJECT) <> CHAR_LENGTH(TRIM(SECOND_OBJECT)) OR SECOND_OBJECT LIKE '%  %' OR SECOND_OBJECT LIKE CONCAT('%',CHAR(9),'%');

-- ---------------------------------------------------------------------------------------
-- Step 3b -- THE NO-BREAK SPACES. Steps 1-3 only know about ' ' and TAB, so a clean bill of
-- health there says nothing about these. Run separately.
--
--   U+00A0 non-breaking space  : 2,392 VALUE rows, 24 CATEGORY  (utf8 bytes C2 A0)
--   U+202F narrow no-break     : 5 VALUE rows                    (utf8 bytes E2 80 AF)
--   U+2007 figure space        : 0 rows today, folded in for symmetry with the Java guard
--
-- These are why the code guard maps them to a plain space BEFORE collapsing: Java does not
-- classify U+202F/U+2007 as whitespace at all, and StringUtils.normalizeSpace maps U+00A0 to
-- a space without re-collapsing, so an NBSP run would otherwise emerge as a run of ordinary
-- double spaces. The SQL has to do the same or the two normalizers disagree.
-- ---------------------------------------------------------------------------------------

SELECT 'VALUE with a no-break space' AS chk, COUNT(*) FROM CHARACTERISTIC
 WHERE VALUE LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR VALUE LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR VALUE LIKE CONCAT('%', _utf8mb4 0xE28087, '%')
UNION ALL
SELECT 'CATEGORY with a no-break space', COUNT(*) FROM CHARACTERISTIC
 WHERE CATEGORY LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR CATEGORY LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR CATEGORY LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

START TRANSACTION;

UPDATE CHARACTERISTIC
   SET VALUE = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                 REPLACE(REPLACE(REPLACE(VALUE, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                 CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE VALUE LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR VALUE LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR VALUE LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

UPDATE CHARACTERISTIC
   SET CATEGORY = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                 REPLACE(REPLACE(REPLACE(CATEGORY, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                 CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE CATEGORY LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR CATEGORY LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR CATEGORY LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

UPDATE CHARACTERISTIC
   SET OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                 REPLACE(REPLACE(REPLACE(OBJECT, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                 CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE OBJECT LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR OBJECT LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR OBJECT LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

UPDATE CHARACTERISTIC
   SET SECOND_OBJECT = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(
                 REPLACE(REPLACE(REPLACE(SECOND_OBJECT, _utf8mb4 0xC2A0, ' '), _utf8mb4 0xE280AF, ' '), _utf8mb4 0xE28087, ' '),
                 CHAR(9),' '),' ',CONCAT(CHAR(1),CHAR(2))),CONCAT(CHAR(2),CHAR(1)),''),CONCAT(CHAR(1),CHAR(2)),' '))
 WHERE SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xC2A0, '%') OR SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xE280AF, '%') OR SECOND_OBJECT LIKE CONCAT('%', _utf8mb4 0xE28087, '%');

-- COMMIT;
-- ROLLBACK;

-- Step 4 -- AFTERWARDS. Two things do not follow automatically:
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
