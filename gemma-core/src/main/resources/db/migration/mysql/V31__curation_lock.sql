-- CURATION_LOCK -- an advisory, steal-able claim on a dataset's curation.
--
-- 🛑 THE LOCK IS ADVISORY. It is not the correctness guarantee and must never become one.
-- PUT /datasets/{id}/curation already checks baseline.lastModified and returns 409 when the
-- dataset moved (DatasetsWebService:2523); that check is what makes concurrent writes safe.
-- The lock exists so the 409 rarely happens and so a curator can see who else is in here.
-- If anyone later removes the baseline check because "the lock handles it", that is the bug.
--
-- STEALING IS ALWAYS PERMITTED. It costs nothing: the other curator's DRAFT
-- is a separate ANNOTATION_SET row and survives untouched, so a steal loses no work. What
-- the loser gets is a 409 on their next commit and a re-sync -- the same protection everyone
-- already has. STOLEN_FROM / STOLEN_AT keep the record of who was displaced.
--
-- 🛑 A SEPARATE TABLE, not columns on INVESTIGATION or CURATION_DETAILS, and this is not a
-- style preference. Any write that routes through the curatable-update path sets
-- curationDetails.lastUpdated (AbstractCuratableDao.updateCurationDetailsFromAuditEvent:55),
-- and lastUpdated IS the optimistic-concurrency token above. A lock that bumped it would
-- 409 every in-flight draft on the dataset the moment anyone took it -- the exact bug
-- bebe778980 fixed for snapshots, reintroduced. Hence: written directly, never through a
-- curatable update, and lock/steal/release emit NO audit event. The STOLEN_* columns are the
-- record of a steal for the same reason the SNAPSHOT row is its own record of a capture.
--
-- ONE ROW PER CURRENTLY-LOCKED DATASET, deleted on release, so the table stays small and a
-- PK on INVESTIGATION_FK is the natural uniqueness constraint -- a dataset cannot be locked
-- twice.
--
-- EXPIRED LOCKS ARE NOT SWEPT. Acquire treats EXPIRES_AT < now as free and overwrites the
-- row. A cleanup job would be a second thing to run, monitor, and forget; a dead row costs
-- one comparison on a table with at most as many rows as there are curators working right
-- now. An abandoned tab resolves itself.
--
-- ON DELETE CASCADE follows ANNOTATION_SET (V20) and SINGLE_CELL_DIMENSION_EXPERIMENT
-- (V23): deleting an experiment must not be blocked by a stale lock on it.
--
-- DATETIME(3) throughout, matching ANNOTATION_SET's timestamps, so "did the lock expire
-- before or after that draft was saved" is answerable at the same precision on both sides.
--
-- LOCKED_BY / STOLEN_FROM are VARCHAR(255) rather than an FK to CONTACT, matching
-- ANNOTATION_SET.CREATED_BY: the identity recorded is whatever the caller authenticated as,
-- and an FK would make a lock un-writable for any identity without a Gemma Contact row.
CREATE TABLE CURATION_LOCK
(
    INVESTIGATION_FK BIGINT       NOT NULL,
    LOCKED_BY        VARCHAR(255) NOT NULL,
    LOCKED_AT        DATETIME(3)  NOT NULL,
    EXPIRES_AT       DATETIME(3)  NOT NULL,
    STOLEN_FROM      VARCHAR(255) NULL,
    STOLEN_AT        DATETIME(3)  NULL,
    PRIMARY KEY (INVESTIGATION_FK),
    CONSTRAINT FK_CURATION_LOCK_INVESTIGATION FOREIGN KEY (INVESTIGATION_FK)
        REFERENCES INVESTIGATION (ID) ON DELETE CASCADE
) ENGINE = InnoDB;
