DROP TABLE IF EXISTS acl_object_identity;
CREATE TABLE acl_object_identity (
  id int(11) NOT NULL auto_increment,
  object_identity varchar(255) NOT NULL,
  parent_object int(11) default NULL,
  acl_class varchar(255) NOT NULL,
  PRIMARY KEY  (id),
  UNIQUE KEY object_identity (object_identity)
) TYPE=InnoDB;

DROP TABLE IF EXISTS acl_permission;
CREATE TABLE acl_permission (
  id int(11) NOT NULL auto_increment,
  acl_object_identity int(11) NOT NULL,
  recipient varchar(100) NOT NULL,
  mask int(11) NOT NULL,
  PRIMARY KEY  (id)
) TYPE=InnoDB;

--- (id, object identity (of form class:getId()), parent object, acl class)
INSERT INTO acl_object_identity VALUES (1, 'dummy:1', null, 'net.sf.acegisecurity.acl.basic.SimpleAclEntry');

--- (id, acl object identity, recepient (principal username), mask)
INSERT INTO acl_permission VALUES (null, 1, 'administrator', 1);

--- Add the user pavlab to the database.  Due to the fact that this user is used specifically for testing acegi security (acl), I have
--- put the insert statements in this acegi specific .sql file as opposed to the init-script.sql.
insert into contact (CLASS, NAME, LAST_NAME, FIRST_NAME, MIDDLE_NAME) values ("PersonImpl", "aPersonWithUserPriviledges", "lastname", "firstname", "middlename");
insert into contact (CLASS, NAME, USER_NAME, PASSWORD, CONFIRM_PASSWORD, ENABLED) values ("UserImpl", "aUserWithUserPriviledges", "pavlab", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", 1);
insert into contact (CLASS, NAME, EMAIL) values ("edu.columbia.gemma.common.auditAndSecurity.ContactImpl", "contact", "keshav@cu-genome.org");
insert into user_role (NAME, USER_NAME) values ("user", "pavlab");

--- Mask integer 0  = no permissions
--- Mask integer 1  = administor
--- Mask integer 2  = read
--- Mask integer 6  = read and write permissions
--- Mask integer 14 = read and write and create permissions
