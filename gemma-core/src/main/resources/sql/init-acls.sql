-- The table for this are now created from our hibernate config for ACLs.

-- Base SIDs we'll need these (not all used by this script; the others would be inserted automagically when needed, but this
-- gives them predictable ids). Principal names must match init-entities script
INSERT INTO ACLSID (ID, class, GRANTED_AUTHORITY) VALUES (1, "GrantedAuthoritySid", "GROUP_ADMIN");
INSERT INTO ACLSID (ID, class, GRANTED_AUTHORITY) VALUES (2, "GrantedAuthoritySid", "GROUP_USER");
INSERT INTO ACLSID (ID, class, GRANTED_AUTHORITY) VALUES (3, "GrantedAuthoritySid", "GROUP_AGENT");
INSERT INTO ACLSID (ID, class, GRANTED_AUTHORITY) VALUES (4, "GrantedAuthoritySid", "IS_AUTHENTICATED_ANONYMOUSLY");
INSERT INTO ACLSID (ID, class, PRINCIPAL) VALUES (5, "PrincipalSid", "administrator");
INSERT INTO ACLSID (ID, class, PRINCIPAL) VALUES (6, "PrincipalSid", "gemmaAgent");

-- Add object identity (OI) for the admin user. There is no parent object, the owner = the administrator; non-inheriting.
INSERT INTO ACLOBJECTIDENTITY (ID, OBJECT_CLASS, OBJECT_ID, OWNER_SID_FK, ENTRIES_INHERITING)
VALUES (1, "ubic.gemma.model.common.auditAndSecurity.User", 1, 1, 0);

-- OI for the Admin group (assumes id of this group=1, see init-entities.sql)
INSERT INTO ACLOBJECTIDENTITY (ID, OBJECT_CLASS, OBJECT_ID, OWNER_SID_FK, ENTRIES_INHERITING)
VALUES (2, "ubic.gemma.model.common.auditAndSecurity.UserGroup", 1, 1, 0);

-- OI for the Agent group (assumes id of this group=2, see init-entities.sql)
INSERT INTO ACLOBJECTIDENTITY (ID, OBJECT_CLASS, OBJECT_ID, OWNER_SID_FK, ENTRIES_INHERITING)
VALUES (3, "ubic.gemma.model.common.auditAndSecurity.UserGroup", 2, 1, 0);

-- OI for the User group (assumes id of group=3, see init-entities.sql)
INSERT INTO ACLOBJECTIDENTITY (ID, OBJECT_CLASS, OBJECT_ID, OWNER_SID_FK, ENTRIES_INHERITING)
VALUES (4, "ubic.gemma.model.common.auditAndSecurity.UserGroup", 3, 1, 0);

-- Add object identity (OI) for the agent user. There is no parent object, the owner = the administrator; non-inheriting.
INSERT INTO ACLOBJECTIDENTITY (ID, OBJECT_CLASS, OBJECT_ID, OWNER_SID_FK, ENTRIES_INHERITING)
VALUES (5, "ubic.gemma.model.common.auditAndSecurity.User", 2, 1, 0);

--
-- give GROUP_ADMIN admin priv on everything - we don't need to give it to a specific user.
--
-- user 1 = administrator, grant admin to sid=1 (GROUP_ADMIN)
INSERT INTO ACLENTRY (ID, ACE_ORDER, MASK, GRANTING, OBJECTIDENTITY_FK, SID_FK) VALUES (1, 1, 16, 1, 1, 1);
INSERT INTO ACLENTRY (ID, ACE_ORDER, MASK, GRANTING, OBJECTIDENTITY_FK, SID_FK) VALUES (2, 1, 16, 1, 2, 1);
INSERT INTO ACLENTRY (ID, ACE_ORDER, MASK, GRANTING, OBJECTIDENTITY_FK, SID_FK) VALUES (3, 1, 16, 1, 3, 1);
INSERT INTO ACLENTRY (ID, ACE_ORDER, MASK, GRANTING, OBJECTIDENTITY_FK, SID_FK) VALUES (4, 1, 16, 1, 4, 1);
INSERT INTO ACLENTRY (ID, ACE_ORDER, MASK, GRANTING, OBJECTIDENTITY_FK, SID_FK) VALUES (5, 1, 16, 1, 5, 1);

-- Give GROUP_USER READ priv on user group sid=2, oi=2, perm=1. (is this necessary?)
-- insert into ACLENTRY (id, ace_order, mask, granting, audit_success, audit_failure, ACLOBJECTIDENTITY, sid) values (6, 2, 1, 1, 0, 0, 2, 2);

-- give user administrator admin priv on themselves (in addition to the group privileges)
INSERT INTO ACLENTRY (ID, ACE_ORDER, MASK, GRANTING, OBJECTIDENTITY_FK, SID_FK) VALUES (7, 2, 16, 1, 1, 5);

-- give agent admin priv on himself.(sid=6). (no group privileges)
INSERT INTO ACLENTRY (ID, ACE_ORDER, MASK, GRANTING, OBJECTIDENTITY_FK, SID_FK) VALUES (8, 2, 16, 1, 5, 6);

-- start our ID sequences properly.
-- insert into ACLHIBERNATESEQUENCES (sequence_name,sequence_next_hi_value) values ('ACLOBJECTIDENTITY', 1);
-- insert into ACLHIBERNATESEQUENCES (sequence_name,sequence_next_hi_value) values ('ACLENTRY', 1);

