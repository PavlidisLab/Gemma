--
-- Phase 2 ACL migration: gsec denormalized schema -> Spring Security 6 canonical schema.
--
-- Replaces gsec's Hibernate-managed ACL tables (ACLOBJECTIDENTITY / ACLSID / ACLENTRY,
-- with OBJECT_CLASS as a denormalized VARCHAR) with Spring Security's four-table canonical
-- schema (acl_class / acl_sid / acl_object_identity / acl_entry). The new schema is what
-- Spring Security's stock JdbcMutableAclService expects out of the box. After this migration,
-- gsec's AclServiceImpl/AclDaoImpl are retired in favour of JdbcMutableAclService wired by
-- ubic.gemma.core.security.acl.GemmaAclConfiguration.
--
-- Old tables are NOT dropped here — they are kept as a safety net until the application has
-- run successfully against the new tables for a release. A follow-up migration will drop them.
--
-- Data caveats handled below:
--   * Orphans: ACLENTRY rows with OBJECTIDENTITY_FK IS NULL (3.1M rows on prod at migration
--     time). These come from an ON DELETE SET NULL cascade fired when the parent AOI was
--     hard-deleted. They are unreachable from the live ACL traversal and are dropped.
--   * (OBJECTIDENTITY_FK, ACE_ORDER) duplicates: 105 pairs on prod at migration time, mostly
--     literal repeats (same MASK/SID/GRANTING) with a handful of genuine ACE_ORDER collisions.
--     Spring Security's canonical schema enforces UNIQUE (acl_object_identity, ace_order),
--     so we keep the lowest-id row in each dup group; lost rows were either redundant
--     (literal repeats) or already nondeterministically ordered in the live system.
--
-- Validated against prod 2026-05-18: 6,598,028 source ACEs -> 3,470,753 dest rows
-- (3,127,170 orphans skipped + 105 dup-losers).
--

-- ====================================================================
-- 1. Schema
-- ====================================================================

CREATE TABLE acl_class (
  id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  class  VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE acl_sid (
  id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  principal  BIT          NOT NULL,
  sid        VARCHAR(255) NOT NULL,
  UNIQUE KEY (principal, sid)
);

CREATE TABLE acl_object_identity (
  id                  BIGINT  NOT NULL AUTO_INCREMENT PRIMARY KEY,
  object_id_class     BIGINT  NOT NULL,
  object_id_identity  BIGINT  NOT NULL,
  parent_object       BIGINT  NULL,
  owner_sid           BIGINT  NULL,
  entries_inheriting  BIT     NOT NULL,
  UNIQUE KEY (object_id_class, object_id_identity),
  CONSTRAINT fk_aoi_class     FOREIGN KEY (object_id_class) REFERENCES acl_class(id),
  CONSTRAINT fk_aoi_parent    FOREIGN KEY (parent_object)   REFERENCES acl_object_identity(id),
  CONSTRAINT fk_aoi_owner_sid FOREIGN KEY (owner_sid)       REFERENCES acl_sid(id)
);

CREATE TABLE acl_entry (
  id                   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  acl_object_identity  BIGINT NOT NULL,
  ace_order            INT    NOT NULL,
  sid                  BIGINT NOT NULL,
  mask                 INT    NOT NULL,
  granting             BIT    NOT NULL,
  audit_success        BIT    NOT NULL,
  audit_failure        BIT    NOT NULL,
  UNIQUE KEY (acl_object_identity, ace_order),
  CONSTRAINT fk_ace_aoi FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity(id),
  CONSTRAINT fk_ace_sid FOREIGN KEY (sid)                 REFERENCES acl_sid(id)
);

-- ====================================================================
-- 2. Data
-- ====================================================================
-- Wrap in a single transaction so any failure rolls back the data move
-- (the CREATE TABLE statements above will auto-commit either way).
--
-- IMPORTANT: do NOT rerun this section against a partially-populated target.
-- Prod hit this once: a prior dry-run committed the acl_entry INSERT but
-- left acl_class / acl_sid / acl_object_identity empty (root cause unknown
-- -- possibly an out-of-band COMMIT after the auto-commit on CREATE TABLE).
-- Re-running blindly hit `Duplicate entry '1' for key 'PRIMARY'` on the
-- acl_entry INSERT. The recovery path was to comment out the acl_entry
-- INSERT and run only the three smaller INSERTs against the already-correct
-- acl_entry rows (IDs preserved across the migration, so FKs reconcile).
--
-- Guard: bail out before any INSERT if ANY of the four target tables is
-- already non-empty. Use a stored-program SIGNAL so the script aborts in
-- MySQL clients that don't honor `\warning` or similar.
DELIMITER //
DROP PROCEDURE IF EXISTS check_acl_targets_empty//
CREATE PROCEDURE check_acl_targets_empty()
BEGIN
  DECLARE total BIGINT;
  SELECT (SELECT COUNT(*) FROM acl_class)
       + (SELECT COUNT(*) FROM acl_sid)
       + (SELECT COUNT(*) FROM acl_object_identity)
       + (SELECT COUNT(*) FROM acl_entry)
    INTO total;
  IF total > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
      'New ACL tables are non-empty; do not rerun this migration blindly. '
      'See the comment at the top of section 2.';
  END IF;
END//
DELIMITER ;
CALL check_acl_targets_empty();
DROP PROCEDURE check_acl_targets_empty;

START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;  -- parent_object self-reference

INSERT INTO acl_class (class)
  SELECT DISTINCT OBJECT_CLASS FROM ACLOBJECTIDENTITY;

INSERT INTO acl_sid (id, principal, sid)
  SELECT id,
         IF(class='PrincipalSid', 1, 0),
         COALESCE(PRINCIPAL, GRANTED_AUTHORITY)
  FROM ACLSID;

INSERT INTO acl_object_identity
    (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
  SELECT aoi.ID, ac.id, aoi.OBJECT_ID,
         aoi.PARENT_OBJECT_FK, aoi.OWNER_SID_FK, aoi.ENTRIES_INHERITING
  FROM ACLOBJECTIDENTITY aoi
  JOIN acl_class ac ON ac.class = aoi.OBJECT_CLASS;

-- Drop orphans (OBJECTIDENTITY_FK IS NULL) and the higher-id row of each
-- (OBJECTIDENTITY_FK, ACE_ORDER) duplicate pair. The NOT EXISTS pattern
-- avoids MySQL 5.7's user-variable / ORDER-BY-in-derived-table issues.
INSERT INTO acl_entry
    (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
  SELECT e.id, e.OBJECTIDENTITY_FK, e.ACE_ORDER,
         e.SID_FK, e.MASK, e.GRANTING, 0, 0
  FROM ACLENTRY e
  WHERE e.OBJECTIDENTITY_FK IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM ACLENTRY e2
      WHERE e2.OBJECTIDENTITY_FK = e.OBJECTIDENTITY_FK
        AND e2.ACE_ORDER = e.ACE_ORDER
        AND e2.id < e.id
    );

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- 3. Verification (manual review before COMMIT)
-- ====================================================================
-- All four checks should be 0 / matching:
--
--   SELECT COUNT(*) FROM acl_object_identity a
--     LEFT JOIN acl_object_identity p ON a.parent_object = p.id
--    WHERE a.parent_object IS NOT NULL AND p.id IS NULL;
--   -- expect: 0 (no dangling parent FKs)
--
--   SELECT COUNT(*) FROM acl_object_identity a
--     LEFT JOIN acl_sid s ON a.owner_sid = s.id
--    WHERE a.owner_sid IS NOT NULL AND s.id IS NULL;
--   -- expect: 0 (no dangling owner FKs)
--
--   SELECT COUNT(*) FROM acl_entry e
--     LEFT JOIN acl_object_identity a ON e.acl_object_identity = a.id
--    WHERE a.id IS NULL;
--   -- expect: 0 (no dangling acl_object_identity FKs)
--
--   SELECT COUNT(*) FROM acl_entry e
--     LEFT JOIN acl_sid s ON e.sid = s.id
--    WHERE s.id IS NULL;
--   -- expect: 0 (no dangling SID FKs)
--
-- If all pass:
--   COMMIT;
-- otherwise:
--   ROLLBACK;
