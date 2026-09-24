--
-- V3 seed data for the H2 test path. Replaces the InitialDataPopulator(slim=true) +
-- DatabaseSchemaPopulator("h2") seed-INSERT chain that BaseDatabaseTest formerly applied.
--
-- Sources mirrored here, INSERTs only:
--   - sql/init-data-slim.sql  (3 AUDIT_TRAIL rows, 3 USER_GROUP rows, 1 CONTACT row)
--   - sql/init-acls.sql       (acl_class + acl_sid + acl_object_identity + acl_entry baseline)
--
-- These rows are what test SecurityContext fixtures (administrator, gemmaAgent, anonymous) and
-- AclService callers rely on. Any test that swaps in a different SecurityContext still works as
-- long as these baseline rows are present.
--

-- ============================================================
-- From sql/init-data-slim.sql
-- ============================================================

insert into AUDIT_TRAIL (ID)
values (1),
       (2),
       (3);

insert into USER_GROUP (ID, AUDIT_TRAIL_FK, NAME, DESCRIPTION)
values (1, 1, 'Administrators', NULL),
       (2, 2, 'Users', NULL),
       (3, 3, 'Agent', NULL);

insert into CONTACT (ID, class, NAME, DESCRIPTION, EMAIL, LAST_NAME, USER_NAME, PASSWORD, PASSWORD_HINT, ENABLED,
                     SIGNUP_TOKEN, SIGNUP_TOKEN_DATESTAMP)
values (1, 'User', 'admin', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- ============================================================
-- From sql/init-acls.sql (INSERTs only -- schema is in V2)
-- ============================================================

-- Class lookup table -- seed the classes used by ACL object identities below
-- AND the Securable entity classes that AclQueryUtils.resolveAclClassId looks up
-- on every ACL-filtered HQL query (ExpressionExperiment, ArrayDesign,
-- BibliographicReference). In production, JdbcMutableAclService inserts these
-- rows on the first createAcl() per entity type; H2 unit tests rarely go
-- through that path, so we pre-seed the rows here. Other Securable subclasses
-- (Gene, CompositeSequence, FactorValue) are never passed to addAclParameters
-- and so don't need a row.
INSERT INTO acl_class (id, class) VALUES (1, 'ubic.gemma.model.common.auditAndSecurity.User');
INSERT INTO acl_class (id, class) VALUES (2, 'ubic.gemma.model.common.auditAndSecurity.UserGroup');
INSERT INTO acl_class (id, class) VALUES (3, 'ubic.gemma.model.expression.experiment.ExpressionExperiment');
INSERT INTO acl_class (id, class) VALUES (4, 'ubic.gemma.model.expression.arrayDesign.ArrayDesign');
INSERT INTO acl_class (id, class) VALUES (5, 'ubic.gemma.model.common.description.BibliographicReference');

-- Base SIDs. Predictable ids so init-entities.sql + tests can reference them.
-- principal=0 -> AclGrantedAuthoritySid; principal=1 -> AclPrincipalSid.
-- Principal names must match init-entities.sql.
INSERT INTO acl_sid (id, principal, sid) VALUES (1, 0, 'GROUP_ADMIN');
INSERT INTO acl_sid (id, principal, sid) VALUES (2, 0, 'GROUP_USER');
INSERT INTO acl_sid (id, principal, sid) VALUES (3, 0, 'GROUP_AGENT');
INSERT INTO acl_sid (id, principal, sid) VALUES (7, 0, 'GROUP_CURATOR');
INSERT INTO acl_sid (id, principal, sid) VALUES (4, 0, 'IS_AUTHENTICATED_ANONYMOUSLY');
INSERT INTO acl_sid (id, principal, sid) VALUES (5, 1, 'administrator');
INSERT INTO acl_sid (id, principal, sid) VALUES (6, 1, 'gemmaAgent');

-- Object identities for the admin user, the three baseline groups, and the agent user.
-- entries_inheriting=0 (no parent ACL -- these are top-level entities).
-- object_id_class FK lookup: 1 = User, 2 = UserGroup (see acl_class inserts above).
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
VALUES (1, 1, 1, NULL, 1, 0);  -- User (administrator, user.id=1)
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
VALUES (2, 2, 1, NULL, 1, 0);  -- UserGroup (admin group, group.id=1)
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
VALUES (3, 2, 2, NULL, 1, 0);  -- UserGroup (agent group, group.id=2)
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
VALUES (4, 2, 3, NULL, 1, 0);  -- UserGroup (user group, group.id=3)
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
VALUES (5, 1, 2, NULL, 1, 0);  -- User (gemmaAgent, user.id=2)

-- Give GROUP_ADMIN (sid=1) admin permission (mask=16) on each baseline AOI. ace_order=1.
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (1, 1, 1, 1, 16, 1, 0, 0);
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (2, 2, 1, 1, 16, 1, 0, 0);
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (3, 3, 1, 1, 16, 1, 0, 0);
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (4, 4, 1, 1, 16, 1, 0, 0);
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (5, 5, 1, 1, 16, 1, 0, 0);

-- Give administrator (sid=5) admin on themselves; gemmaAgent (sid=6) admin on themselves. ace_order=2.
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (7, 1, 2, 5, 16, 1, 0, 0);
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (8, 5, 2, 6, 16, 1, 0, 0);
