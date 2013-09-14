-- update to revised ACL setup. Here is the schema for the new tables. They are named distinctly so we don't clash with old tables during transition.

drop table if exists ACLENTRY;
drop table if exists ACLOBJECTIDENTITY;
drop table if exists ACLSID;

create table ACLOBJECTIDENTITY (
ID BIGINT not null auto_increment, 
OBJECT_ID BIGINT not null, 
OBJECT_CLASS VARCHAR(255) character set latin1 collate latin1_swedish_ci not null, 
ENTRIES_INHERITING TINYINT not null, 
OWNER_SID_FK BIGINT not null, 
PARENT_OBJECT_FK BIGINT, 
primary key (ID));

create table ACLENTRY (
ID BIGINT not null auto_increment, 
GRANTING TINYINT not null, 
MASK INTEGER not null, 
ACE_ORDER INTEGER not null, 
SID_FK BIGINT not null, 
OBJECTIDENTITY_FK BIGINT, primary key (ID));

create table ACLSID (
ID BIGINT not null auto_increment, 
class varchar(255) not null, 
PRINCIPAL VARCHAR(255) character set latin1 collate latin1_swedish_ci unique, 
GRANTED_AUTHORITY VARCHAR(255) character set latin1 collate latin1_swedish_ci unique, 
primary key (ID));

-- old schema
--`id` bigint(20) NOT NULL AUTO_INCREMENT,
--  `object_id_class` bigint(20) NOT NULL,
--  `object_id_identity` bigint(20) NOT NULL,
--  `parent_object` bigint(20) DEFAULT NULL,
--  `owner_sid` bigint(20) DEFAULT NULL,
--  `entries_inheriting` tinyint(4) DEFAULT NULL,



 
insert into ACLSID (ID,PRINCIPAL,class) select id,sid,"PrincipalSid" from acl_sid os where os.principal=1; 
insert into ACLSID (ID,GRANTED_AUTHORITY,class) select id,sid,"GrantedAuthoritySid" from acl_sid os where os.principal=0;

insert into ACLOBJECTIDENTITY (ID, OBJECT_ID,ENTRIES_INHERITING,OWNER_SID_FK,PARENT_OBJECT_FK) 
  select id,object_id_identity,entries_inheriting,owner_sid,parent_object from acl_object_identity;
  
insert into ACLENTRY(ID,GRANTING,ACE_ORDER,OBJECTIDENTITY_FK,SID_FK,MASK) 
  select id,granting,ace_order,acl_object_identity,sid,mask from acl_entry;

update ACLOBJECTIDENTITY aoi, acl_class ac,acl_object_identity oldaoi 
  set aoi.OBJECT_CLASS = ac.class where ac.id=oldaoi.object_id_class and aoi.id=oldaoi.id;

-- add indices and constraints
alter table ACLENTRY 
  add index FK2FB5F83D3F8EF1C4 (SID_FK), 
  add constraint FK2FB5F83D3F8EF1C4 foreign key (SID_FK) references ACLSID (ID);

alter table ACLENTRY 
  add index ACL_ENTRY_OBJECTIDENTITY_FKC (OBJECTIDENTITY_FK), 
  add constraint ACL_ENTRY_OBJECTIDENTITY_FKC foreign key (OBJECTIDENTITY_FK) references ACLOBJECTIDENTITY (ID);

alter table ACLOBJECTIDENTITY 
  add index FK988CEFE926291CD0 (OWNER_SID_FK), 
  add constraint FK988CEFE926291CD0 foreign key (OWNER_SID_FK) references ACLSID (ID);

alter table ACLOBJECTIDENTITY 
  add index FK988CEFE95223826D (PARENT_OBJECT_FK), 
  add constraint FK988CEFE95223826D foreign key (PARENT_OBJECT_FK) references ACLOBJECTIDENTITY (ID);

alter table ACLOBJECTIDENTITY add unique key acloid (OBJECT_CLASS,OBJECT_ID);
  
-- delete cruft from aclentry? These SecuredChild classes should not have their own ACEs.
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.bioAssay.BioAssayImpl" ;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" ;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.biomaterial.BioMaterialImpl";
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl";
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl" ;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" ;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" ;

delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.bioAssay.BioAssayImpl" ;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" ;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.biomaterial.BioMaterialImpl";
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl";
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl" ;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" ;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" ;

-- not appearing: samplecoexpressionanalysis; principalcomponentanalysis. These seem to lack security?

-- later ...
--drop table acl_permission;
--drop table acl_object_identity;
--drop table acl_entry;
--drop table acl_class
--drop table acl_sid;

-- remove unused data from CONTACT table.
--ALTER TABLE CONTACT drop column U_R_L;
--ALTER TABLE CONTACT drop column ADDRESS;
--ALTER TABLE CONTACT drop column PHONE;
--ALTER TABLE CONTACT drop column TOLL_FREE_PHONE;
--ALTER TABLE CONTACT drop column FAX;
-- ALTER TABLE CONTACT drop COLUMN PARENT_FK;
--ALTER TABLE CONTACT drop constraint ORGANIZATION_PARENT_FKC;




