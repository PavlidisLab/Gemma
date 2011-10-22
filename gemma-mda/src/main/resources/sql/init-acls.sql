-- Note: The table names used here (including lack of capitalization) 
-- are those defined by default by Spring Security
-- Don't change them! 
-- See: http://static.springsource.org/spring-security/site/docs/3.0.x/reference/appendix-schema.html


drop table if exists acl_sid;
create table acl_sid (
	id bigint not null auto_increment, 
	principal tinyint not null, 
	sid varchar(255) character set latin1 collate latin1_swedish_ci not null, 
	primary key (id)
) Engine=innodb;


drop table if exists acl_class;
create table acl_class (
	id bigint not null auto_increment, 
	class varchar(255) character set latin1 collate latin1_swedish_ci not null, 
	primary key (id)
) Engine=innodb;

drop table if exists acl_object_identity;
create table acl_object_identity (
	id bigint not null auto_increment, 
	object_id_class bigint not null, 
	object_id_identity bigint not null,
	parent_object bigint,
	owner_sid bigint,  
	entries_inheriting tinyint,
	primary key (id)
) Engine=innodb;


drop table if exists acl_entry;
create table acl_entry (
	id bigint not null auto_increment, 
	ace_order integer not null, 
	mask integer not null, 
	granting tinyint not null, 
	audit_success tinyint not null, 
	audit_failure tinyint not null, 
	acl_object_identity bigint not null, 
	sid bigint not null, 
	primary key (id)
) Engine=innodb;


alter table acl_sid
	add unique index sidprini (sid, principal);
	
alter table acl_class
	add unique index classi (class);

alter table acl_object_identity  
	add unique index idob (object_id_class,object_id_identity), 
	add index owner_sid_index (owner_sid), 
	add constraint owner_sid_acl_sid_fkc foreign key (owner_sid) references acl_sid (id),
	add index object_id_class_index (object_id_class), 
	add constraint object_id_class_acl_class_fkc foreign key (object_id_class) references acl_class (id),
	add index parent_object_index (parent_object), 
	add constraint parent_object_acl_oi_fkc foreign key (parent_object) references acl_object_identity (id);
	
alter table acl_entry 
	add unique index acloiao (acl_object_identity,ace_order),
	add index acl_entry_sid_index (sid), 
	add constraint acl_sid_fkc foreign key (sid) references acl_sid (id),
	add index acloi (acl_object_identity), 
	add constraint acl_entry_acl_oi_fkc foreign key (acl_object_identity) references acl_object_identity (id);

 -- add object_identity etc. for intialization of initial users
insert into acl_class (id, class) values(1, "ubic.gemma.model.common.auditAndSecurity.UserImpl");
insert into acl_class (id, class) values(2, "ubic.gemma.model.common.auditAndSecurity.UserGroupImpl");

-- Base SIDs we'll need these (not all used by this script; the others would be inserted automagically when needed, but this
-- gives them predictable ids). Principal names must match init-entities script
insert into acl_sid (id, principal, sid) values(1, 0, "GROUP_ADMIN");
insert into acl_sid (id, principal, sid) values(2, 0, "GROUP_USER"); 
insert into acl_sid (id, principal, sid) values(3, 0, "GROUP_AGENT"); 
insert into acl_sid (id, principal, sid) values(4, 0, "IS_AUTHENTICATED_ANONYMOUSLY"); 
insert into acl_sid (id, principal, sid) values(5, 1, "administrator"); 
insert into acl_sid (id, principal, sid) values(6, 1, "gemmaAgent");

-- Add object identity (OI) for the admin user. There is no parent object, the owner = the administrator; non-inheriting.
insert into acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (1, 1, 1, null, 1, 0);

-- OI for the Admin group (assumes id of this group=1, see init-entities.sql)
insert into acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (2, 2, 1, null, 1, 0);

-- OI for the Agent group (assumes id of this group=2, see init-entities.sql)
insert into acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (3, 2, 2, null, 1, 0);

-- OI for the User group (assumes id of group=3, see init-entities.sql)
insert into acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (4, 2, 3, null, 1, 0);

-- Add object identity (OI) for the agent user. There is no parent object, the owner = the administrator; non-inheriting.
insert into acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (5, 1, 2, null, 1, 0);

--
-- give GROUP_ADMIN admin priv on everything - we don't need to give it to a specific user.
--
-- user 1 = administrator, grant admin to sid=1 (GROUP_ADMIN)
insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (1, 1, 16, 1, 0, 0, 1, 1);
insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (2, 1, 16, 1, 0, 0, 2, 1);
insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (3, 1, 16, 1, 0, 0, 3, 1);
insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (4, 1, 16, 1, 0, 0, 4, 1);
insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (5, 1, 16, 1, 0, 0, 5, 1);

-- Give GROUP_USER READ priv on user group sid=2, oi=2, perm=1. (is this necessary?)
-- insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (6, 2, 1, 1, 0, 0, 2, 2);

-- give user administrator admin priv on themselves (in addition to the group privileges)
insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (7, 2, 16, 1, 0, 0, 1, 5);

-- give agent admin priv on himself.(sid=6). (no group privileges)
insert into acl_entry (id, ace_order, mask, granting, audit_success, audit_failure, acl_object_identity, sid) values (8, 2, 16, 1, 0, 0, 5, 6);
