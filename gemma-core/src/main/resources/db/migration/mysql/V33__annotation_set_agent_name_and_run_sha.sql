-- ANNOTATION_SET.RUN_SHA + AGENT_NAME -- columns the entity has always mapped and no migration
-- ever added.
--
-- 🛑 The failure this fixes is total, not partial. Hibernate's select lists every mapped column,
-- so a database missing either one answers EVERY annotation-set read with
--
--     Unknown column 'as1_0.AGENT_NAME' in 'field list'
--
-- and the draft / proposal / snapshot / commit routes all 500. It is not a missing-value problem
-- that degrades gracefully.
--
-- Why it went unnoticed: the two schema paths had diverged and only one of them is exercised.
--   * gemdtest and every other hbm2ddl=create database builds its schema FROM THE ENTITY, so the
--     columns are simply there and the whole test suite passes.
--   * Production carries both, added out of band -- they sit at the end of the table, after
--     PARKED_ELEMENTS, in an order no migration would have produced.
--   * A database built from these migrations alone gets neither. gemdsandbox is the first one
--     anybody queried: Flyway-managed, at V32, and every draft route 500ing (2026-08-25).
--
-- Verified the same day that this is the whole of the drift: comparing gemdtest (entity-built)
-- against gemdsandbox (migration-built) over every shared table yields exactly these two columns
-- and zero missing tables.
--
-- GUARDED, deliberately. Production already has the columns and no flyway_schema_history -- its
-- migrations are hand-run -- so an unguarded ALTER would fail there with "Duplicate column name"
-- for whoever runs it. MySQL has no ADD COLUMN IF NOT EXISTS (that is MariaDB), hence the
-- information_schema check plus PREPARE. The H2 twin needs none of this and is a one-liner.

SET @ddl := (
    SELECT IF( COUNT(*) = 0,
               'ALTER TABLE ANNOTATION_SET ADD COLUMN RUN_SHA VARCHAR(255) NULL',
               'DO 0' )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ANNOTATION_SET'
      AND COLUMN_NAME = 'RUN_SHA' );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF( COUNT(*) = 0,
               'ALTER TABLE ANNOTATION_SET ADD COLUMN AGENT_NAME VARCHAR(255) NULL',
               'DO 0' )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ANNOTATION_SET'
      AND COLUMN_NAME = 'AGENT_NAME' );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
