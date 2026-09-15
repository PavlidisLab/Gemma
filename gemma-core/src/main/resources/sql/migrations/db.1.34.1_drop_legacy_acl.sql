--
-- Follow-up to db.1.33.0.sql: drop the legacy gsec ACL tables.
--
-- db.1.33.0.sql migrated ACLs to Spring Security's canonical schema on 2026-05-18 and said:
--
--     "Old tables are NOT dropped here -- they are kept as a safety net until the application
--      has run successfully against the new tables for a release. A follow-up migration will
--      drop them."
--
-- This is that follow-up. It was not written at the time, and in the interval a second fact
-- emerged that the original plan did not account for: Gemma 1.32.x is still live against the
-- same database and still maps to the OLD tables, so it never stopped writing them. The safety
-- net became a second live store, and the two have diverged in both directions.
--
-- 🛑 DO NOT RUN WHILE 1.32.x IS SERVING. Run only once it is off and no longer accepting
--    logins or registrations. While it runs, the gap below keeps growing.
--
-- 🛑 RUN THE REPORT SECTION FIRST AND READ IT. Measured 2026-08-24, but every number moves for
--    as long as 1.32.x is up, so re-measure at drop time rather than trusting these:
--
--      87,521 rows exist ONLY in the old store; 73,951 of them carry their own ACEs.
--      Almost all are child objects (BioAssay/BioMaterial 37,436 each, FactorValue 5,201, ...)
--      whose permissions 2.0 resolves through the parent experiment -- verified: an affected
--      experiment (GSE277230.1, ee 92410) serves /datasets, /samples, /design and /platforms
--      normally, so their loss is not expected to change 2.0 behaviour.
--
--      The two classes where loss is NOT obviously harmless:
--        * User  -- 4 accounts (vmmb94, nayrouz, rajathalbhat, EthanHuang) had an ACL only in
--          the old store, being the newest registrations. Registration still runs through
--          1.32.x, so this set GROWS until 1.32.x is off. Section 2 carries them over.
--        * ExpressionExperiment -- 5 (GSE273690, .1, .2, GSE278808.1, .2). Checked: their old
--          ACEs are GROUP_ADMIN(16) + GROUP_AGENT(1) with NO IS_AUTHENTICATED_ANONYMOUSLY, so
--          they are genuinely private and 2.0 already treats them so. No action needed, but
--          re-check at drop time in case newer ones differ.
--

-- ============================ 1. REPORT (read-only) ============================

SELECT '--- rows only in the OLD store, by class ---' AS report;
SELECT u.OBJECT_CLASS AS class_name, COUNT(*) AS only_in_old,
       SUM(CASE WHEN e.n > 0 THEN 1 ELSE 0 END) AS with_own_aces
FROM ACLOBJECTIDENTITY u
LEFT JOIN acl_class c ON c.class = u.OBJECT_CLASS
LEFT JOIN acl_object_identity l
       ON l.object_id_identity = u.OBJECT_ID AND l.object_id_class = c.id
LEFT JOIN (SELECT OBJECTIDENTITY_FK, COUNT(*) n FROM ACLENTRY GROUP BY OBJECTIDENTITY_FK) e
       ON e.OBJECTIDENTITY_FK = u.ID
WHERE l.id IS NULL
GROUP BY u.OBJECT_CLASS ORDER BY only_in_old DESC;

SELECT '--- users that would lose their ACL (section 2 carries these over) ---' AS report;
SELECT u.OBJECT_ID AS user_id, ct.USER_NAME, ct.EMAIL, ct.ENABLED
FROM ACLOBJECTIDENTITY u
LEFT JOIN acl_class c ON c.class = u.OBJECT_CLASS
LEFT JOIN acl_object_identity l
       ON l.object_id_identity = u.OBJECT_ID AND l.object_id_class = c.id
LEFT JOIN CONTACT ct ON ct.ID = u.OBJECT_ID
WHERE u.OBJECT_CLASS = 'ubic.gemma.model.common.auditAndSecurity.User' AND l.id IS NULL;

SELECT '--- experiments that would lose their ACL: check for anonymous READ ---' AS report;
SELECT u.OBJECT_ID AS ee_id, i.SHORT_NAME,
       MAX(CASE WHEN s.GRANTED_AUTHORITY = 'IS_AUTHENTICATED_ANONYMOUSLY'
                 AND e.GRANTING = 1 AND (e.MASK & 1) = 1 THEN 1 ELSE 0 END) AS is_public
FROM ACLOBJECTIDENTITY u
LEFT JOIN acl_class c ON c.class = u.OBJECT_CLASS
LEFT JOIN acl_object_identity l
       ON l.object_id_identity = u.OBJECT_ID AND l.object_id_class = c.id
LEFT JOIN INVESTIGATION i ON i.ID = u.OBJECT_ID
LEFT JOIN ACLENTRY e ON e.OBJECTIDENTITY_FK = u.ID
LEFT JOIN ACLSID s ON s.ID = e.SID_FK
WHERE u.OBJECT_CLASS = 'ubic.gemma.model.expression.experiment.ExpressionExperiment'
  AND l.id IS NULL
GROUP BY u.OBJECT_ID, i.SHORT_NAME;
-- 🛑 any row with is_public = 1 is PUBLIC in 1.32.x and would go dark in 2.0. Stop and decide.

-- ==================== 2. CARRY USER ACLs INTO THE NEW STORE ====================
-- Users are carried because a user with no ACL in the store 2.0 reads is not a child of
-- anything -- there is no parent for permissions to fall back to. Child objects are
-- deliberately NOT carried: they inherit from their experiment, which already has an ACL.

START TRANSACTION;

INSERT INTO acl_sid (principal, sid)
SELECT DISTINCT 1, ct.USER_NAME
FROM ACLOBJECTIDENTITY u
LEFT JOIN acl_class c ON c.class = u.OBJECT_CLASS
LEFT JOIN acl_object_identity l
       ON l.object_id_identity = u.OBJECT_ID AND l.object_id_class = c.id
JOIN CONTACT ct ON ct.ID = u.OBJECT_ID
LEFT JOIN acl_sid existing ON existing.sid = ct.USER_NAME AND existing.principal = 1
WHERE u.OBJECT_CLASS = 'ubic.gemma.model.common.auditAndSecurity.User'
  AND l.id IS NULL AND existing.id IS NULL AND ct.USER_NAME IS NOT NULL;

INSERT INTO acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
SELECT (SELECT id FROM acl_class WHERE class = 'ubic.gemma.model.common.auditAndSecurity.User'),
       u.OBJECT_ID, NULL, sid.id, 0
FROM ACLOBJECTIDENTITY u
LEFT JOIN acl_class c ON c.class = u.OBJECT_CLASS
LEFT JOIN acl_object_identity l
       ON l.object_id_identity = u.OBJECT_ID AND l.object_id_class = c.id
JOIN CONTACT ct ON ct.ID = u.OBJECT_ID
JOIN acl_sid sid ON sid.sid = ct.USER_NAME AND sid.principal = 1
WHERE u.OBJECT_CLASS = 'ubic.gemma.model.common.auditAndSecurity.User' AND l.id IS NULL;

SELECT 'users still missing from the new store (want 0)' AS metric, COUNT(*) AS n
FROM ACLOBJECTIDENTITY u
LEFT JOIN acl_class c ON c.class = u.OBJECT_CLASS
LEFT JOIN acl_object_identity l
       ON l.object_id_identity = u.OBJECT_ID AND l.object_id_class = c.id
WHERE u.OBJECT_CLASS = 'ubic.gemma.model.common.auditAndSecurity.User' AND l.id IS NULL;

COMMIT;

-- 🛑 The ACEs on those user ACLs are NOT copied -- the old schema's MASK/SID rows would need
--    translating and the 4 known cases carry 6 ACEs each of unexamined meaning. Inspect them
--    with the report above and decide before dropping, or re-grant through the application.

-- ============================== 3. THE DROP ==============================
-- Uncomment only after sections 1 and 2 are done and 1.32.x is off for good.
-- Take a backup of the three tables first: they are the last copy of 87k ACL rows.
--
--   DROP TABLE ACLENTRY;            -- FK-child of ACLOBJECTIDENTITY, goes first
--   DROP TABLE ACLOBJECTIDENTITY;
--   DROP TABLE ACLSID;
--
-- Also then: gemma-core/src/main/resources/ubic/gemma/core/security/sql/init-acl-indices.sql
-- still ALTERs ACLOBJECTIDENTITY, and V1__prod_baseline.sql still CREATEs all three, so
-- Flyway-built databases keep making them. Clean those up in the same change or a fresh
-- gemdtest build will still carry the dead tables.
