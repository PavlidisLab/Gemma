-- GROUP_CURATOR: full authority over dataset CONTENT and VISIBILITY, none over user
-- accounts or server operations.
--
-- Three parts, and the third is the one that matters. A group and its authority make
-- `hasAuthority('GROUP_CURATOR')` satisfiable at the REST layer, but every write below
-- that layer is gated by ACL_SECURABLE_EDIT, which needs an ACE on the object itself.
-- Administrators pass that check only because BaseAclAdvice.setupBaseAces grants
-- GROUP_ADMIN an ADMINISTRATION ace on every object AS IT IS CREATED -- which does
-- nothing for the objects that already exist. Hence the backfill.
--
-- ADMINISTRATION rather than WRITE, deliberately: AclAuthorizationStrategyImpl falls back
-- to an ADMINISTRATION check when the caller lacks the configured authority, so it is the
-- one grant that covers reading, editing AND changing visibility. A WRITE ace satisfies
-- ACL_SECURABLE_EDIT and then fails on makePublic.
--
-- 🛑 User and UserGroup objects are EXCLUDED. They are securable like anything else, so an
-- unconditional backfill hands a curator BaseUserService.removeUserFromGroup -- gated on
-- hasPermission(#group, 'administration') -- and with it the ability to add themselves to
-- Administrators. Applied unscoped against production on 2026-09-05 and corrected the same
-- hour; 668 aces (661 users + the groups) had to be deleted back out.
--
-- Reversible: `DELETE FROM acl_entry WHERE sid = (SELECT id FROM acl_sid WHERE sid = 'GROUP_CURATOR');`
-- undoes the grant without touching anything else. Nothing here drops or rewrites an
-- existing row.
--
-- Idempotent throughout: every insert is guarded, because this was first applied by hand and a
-- later automated run must not create a second Curators group.

-- 1. the group itself
INSERT INTO AUDIT_TRAIL (ID)
SELECT NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USER_GROUP WHERE NAME = 'Curators');
SET @curator_trail = LAST_INSERT_ID();

INSERT INTO USER_GROUP (NAME, DESCRIPTION, AUDIT_TRAIL_FK)
SELECT 'Curators',
       'Curators: full authority over dataset content and visibility, none over user accounts or server operations.',
       @curator_trail
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USER_GROUP WHERE NAME = 'Curators');

SET @curator_group = (SELECT ID FROM USER_GROUP WHERE NAME = 'Curators');

INSERT INTO GROUP_AUTHORITY (AUTHORITY, GROUP_FK)
SELECT 'CURATOR', @curator_group
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM GROUP_AUTHORITY WHERE AUTHORITY = 'CURATOR' AND GROUP_FK = @curator_group );

-- 2. the ACL security identity
INSERT INTO acl_sid (principal, sid)
SELECT 0, 'GROUP_CURATOR'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM acl_sid WHERE sid = 'GROUP_CURATOR' AND principal = 0);

SET @curator_sid = (SELECT id FROM acl_sid WHERE sid = 'GROUP_CURATOR' AND principal = 0);

-- 3. an ADMINISTRATION ace on every object that already exists.
--
-- ace_order is per-object and must not collide with an existing entry, so each row takes
-- one past that object's current maximum rather than a constant.
--
-- granting=1, audit_success/audit_failure=0 -- matching what setupBaseAces writes for
-- GROUP_ADMIN, so a backfilled object and a newly created one are indistinguishable.
INSERT INTO acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
SELECT oi.id,
       COALESCE((SELECT MAX(e.ace_order) + 1 FROM acl_entry e WHERE e.acl_object_identity = oi.id), 0),
       @curator_sid,
       16,   -- BasePermission.ADMINISTRATION
       1, 0, 0
FROM acl_object_identity oi
JOIN acl_class c ON c.id = oi.object_id_class
WHERE c.class NOT IN ( 'ubic.gemma.model.common.auditAndSecurity.User',
                       'ubic.gemma.model.common.auditAndSecurity.UserGroup' )
  -- Only objects that carry their own aces. BaseAclAdvice writes base aces under
  -- `if ( create && !inheritFromParent )` and sets entries_inheriting to the same flag, so an
  -- inheriting child has never had aces of its own and resolves through its parent -- an ace here
  -- would be redundant with the one on the parent and duplicate the row for every child in the
  -- tree. An entries_inheriting row with no parent has nothing to inherit FROM and keeps its ace.
  AND NOT ( oi.entries_inheriting = 1 AND oi.parent_object IS NOT NULL )
  AND NOT EXISTS (
    SELECT 1 FROM acl_entry e2
    WHERE e2.acl_object_identity = oi.id AND e2.sid = @curator_sid
);
