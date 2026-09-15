-- AUDIT_EVENT.ON_BEHALF_OF: the curator an action was taken FOR, when the performer carried it
-- for someone else.
--
-- PERFORMER_FK keeps its meaning and does not move: it is the CREDENTIAL that wrote the row. An
-- agent authenticated as `gemmaAgent` committing a curator's draft makes PERFORMER_FK gemmaAgent,
-- which is true and is not an answer to "who decided this". This column is that answer. Both facts
-- or neither -- overwriting the performer with the curator would lose which key was used, which is
-- the half that matters when the question is "how did this get written".
--
-- 🛑 VARCHAR, not an FK to CONTACT, matching CURATION_LOCK.LOCKED_BY and ANNOTATION_SET.CREATED_BY.
-- An FK makes the row un-writable for any identity without a Gemma account, and an audit write that
-- fails takes the commit down with it. The cost is real and accepted: no referential integrity, and
-- a renamed user leaves the old name behind on rows already written.
--
-- NULL on the ordinary case, which is nearly every row: performer and actor are the same person.
-- Nothing is backfilled -- a historical row genuinely does not know, and inventing a value would be
-- worse than the null that says so.

-- Idempotent, because this was applied to production by hand on 2026-09-05 and Flyway's history
-- therefore has no row for it. MySQL has no ADD COLUMN IF NOT EXISTS (MariaDB does; MySQL does not),
-- so the guard is an information_schema check around a prepared statement. Without it, a later Flyway
-- run against a database that already has the column fails on ER_DUP_FIELDNAME and stops the whole
-- migration chain behind it.
SET @have_column := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'AUDIT_EVENT'
       AND COLUMN_NAME = 'ON_BEHALF_OF' );

SET @ddl := IF( @have_column = 0,
    'ALTER TABLE AUDIT_EVENT ADD COLUMN ON_BEHALF_OF VARCHAR(255) NULL',
    'DO 0' );

PREPARE add_on_behalf_of FROM @ddl;
EXECUTE add_on_behalf_of;
DEALLOCATE PREPARE add_on_behalf_of;
