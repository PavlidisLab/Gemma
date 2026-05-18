--
-- Seed ACL data for the test database, populating Spring Security 6's canonical
-- four-table ACL schema (acl_class, acl_sid, acl_object_identity, acl_entry).
--
-- Hibernate creates acl_sid, acl_object_identity, and acl_entry from gsec's HBM
-- mappings on hbm2ddl=create. acl_class is NOT Hibernate-mapped so we create it
-- here. In prod the equivalent migration is db.1.33.0.sql.
--
-- The id-numbering convention is preserved from the legacy init-acls.sql for
-- continuity with init-entities.sql cross-references.
--

CREATE TABLE IF NOT EXISTS acl_class (
  id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  class  VARCHAR(255) NOT NULL UNIQUE
);

-- Class lookup table -- seed only the classes used by ACL object identities below.
-- New entity classes get their acl_class row inserted automatically on first
-- createAcl() by JdbcMutableAclService.
INSERT INTO acl_class (id, class) VALUES (1, 'ubic.gemma.model.common.auditAndSecurity.User');
INSERT INTO acl_class (id, class) VALUES (2, 'ubic.gemma.model.common.auditAndSecurity.UserGroup');

-- Base SIDs. Predictable ids so init-entities.sql + tests can reference them.
-- principal=0 -> AclGrantedAuthoritySid; principal=1 -> AclPrincipalSid.
-- Principal names must match init-entities.sql.
INSERT INTO acl_sid (id, principal, sid) VALUES (1, 0, 'GROUP_ADMIN');
INSERT INTO acl_sid (id, principal, sid) VALUES (2, 0, 'GROUP_USER');
INSERT INTO acl_sid (id, principal, sid) VALUES (3, 0, 'GROUP_AGENT');
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
