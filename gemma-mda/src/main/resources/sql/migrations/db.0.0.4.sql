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

  
insert into ACLENTRY(ID,GRANTING,ACE_ORDER,OBJECTIDENTITY_FK,SID_FK,MASK) 
  select id,granting,ace_order,acl_object_identity,sid,mask from acl_entry;
  
  
insert into ACLOBJECTIDENTITY (ID, OBJECT_ID,ENTRIES_INHERITING,OWNER_SID_FK,PARENT_OBJECT_FK) 
  select id,object_id_identity,entries_inheriting,owner_sid,parent_object from acl_object_identity;

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


-- items which have aces, but shouldn't and they don't have a parent. These are probably for entities that no longer exist, but have to check.
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join BIO_ASSAY b on  b.ID=o.OBJECT_ID   where o.OBJECT_CLASS="ubic.gemma.model.expression.bioAssay.BioAssayImpl" and o.PARENT_OBJECT_FK is null and b.ID is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join EXPERIMENTAL_DESIGN b on  b.ID=o.OBJECT_ID  where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl"  and o.PARENT_OBJECT_FK is null and b.ID is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join BIO_MATERIAL b on b.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" and o.PARENT_OBJECT_FK is  null and b.ID is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join EXPERIMENTAL_FACTOR b on b.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" and o.PARENT_OBJECT_FK is  null and b.ID is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join FACTOR_VALUE b on b.ID=o.OBJECT_ID  where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl"  and o.PARENT_OBJECT_FK is  null and b.ID is null;

-- these are all zero.
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join ANALYSIS b on  b.ID=o.OBJECT_ID where b.class="DifferentialExpressionAnalysisImpl" AND o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is  null and b.ID is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join ANALYSIS b on  b.ID=o.OBJECT_ID where b.class="ProbeCoexpressionAnalysisImpl" AND o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is  null and b.ID is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join ANALYSIS b on  b.ID=o.OBJECT_ID where b.class="SampleCoexpressionAnalysisImpl" AND o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is  null and b.ID is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK left join ANALYSIS b on  b.ID=o.OBJECT_ID where b.class="PrincipalComponentAnalysisImpl" AND o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"  and o.PARENT_OBJECT_FK is  null and b.ID is null;

set FOREIGN_KEY_CHECKS=0;
delete e,o from ACLOBJECTIDENTITY o join ACLENTRY e on o.ID=e.OBJECTIDENTITY_FK left join BIO_ASSAY b on  b.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.bioAssay.BioAssayImpl"   and o.PARENT_OBJECT_FK is null and b.ID IS NULL; 
delete e,o from ACLOBJECTIDENTITY o join ACLENTRY e on o.ID=e.OBJECTIDENTITY_FK left join EXPERIMENTAL_DESIGN b on  b.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl"   and o.PARENT_OBJECT_FK is null and b.ID IS NULL; 
delete e,o from ACLOBJECTIDENTITY o join ACLENTRY e on o.ID=e.OBJECTIDENTITY_FK left join BIO_MATERIAL b on  b.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.biomaterial.BioMaterialImpl"   and o.PARENT_OBJECT_FK is null and b.ID IS NULL; 
delete e,o from ACLOBJECTIDENTITY o join ACLENTRY e on o.ID=e.OBJECTIDENTITY_FK left join EXPERIMENTAL_FACTOR b on  b.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl"   and o.PARENT_OBJECT_FK is null and b.ID IS NULL; 
delete e,o from ACLOBJECTIDENTITY o join ACLENTRY e on o.ID=e.OBJECTIDENTITY_FK left join FACTOR_VALUE b on  b.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl"   and o.PARENT_OBJECT_FK is null and b.ID IS NULL; 
set FOREIGN_KEY_CHECKS=1;

-- checking the previous to make sure they don't exist in the entity table. These should return 0.
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join BIO_ASSAY ba ON ba.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.bioAssay.BioAssayImpl" and o.PARENT_OBJECT_FK is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join EXPERIMENTAL_DESIGN ed on ed.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl"  and o.PARENT_OBJECT_FK is null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join BIO_MATERIAL bm on bm.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" and o.PARENT_OBJECT_FK is  null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join EXPERIMENTAL_FACTOR ef on ef.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" and o.PARENT_OBJECT_FK is  null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join FACTOR_VALUE fv on fv.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl"  and o.PARENT_OBJECT_FK is  null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join ANALYSIS a on a.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is  null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join ANALYSIS a on a.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"  and o.PARENT_OBJECT_FK is  null;


-- these are kind of messed up.
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join ANALYSIS a on a.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is  null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK inner join ANALYSIS a on a.ID=o.OBJECT_ID where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is  null;


select * from  ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a  WHERE 
a.ID=o.OBJECT_ID and 
a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
and a.class="DifferentialExpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;

select * from  ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a  WHERE 
a.ID=o.OBJECT_ID and 
a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
and a.class="PrincipalComponentAnalysisImpl"
and o.PARENT_OBJECT_FK is null;

select * from  ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a  WHERE 
a.ID=o.OBJECT_ID and 
a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
and a.class="ProbeCoexpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;


update ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a SET o.PARENT_OBJECT_FK=po.ID WHERE 
a.ID=o.OBJECT_ID and 
a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
and a.class="DifferentialExpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;

update ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a SET o.PARENT_OBJECT_FK=po.ID WHERE 
a.ID=o.OBJECT_ID and 
a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
and a.class="ProbeCoexpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;

update ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a SET o.PARENT_OBJECT_FK=po.ID WHERE 
a.ID=o.OBJECT_ID and 
a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
and a.class="PrincipalComponentAnalysisImpl"
and o.PARENT_OBJECT_FK is null;



-- mismatches between value in the acl table and the actual object class
select * from  ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a, ACLENTRY e   WHERE 
a.ID=o.OBJECT_ID 
and e.OBJECTIDENTITY_FK=o.ID
and a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
and a.class<>"DifferentialExpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;

set FOREIGN_KEY_CHECKS=0;
delete e, o from  ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a, ACLENTRY e  WHERE 
a.ID=o.OBJECT_ID  
and e.OBJECTIDENTITY_FK=o.ID
and a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
and a.class<>"DifferentialExpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;
set FOREIGN_KEY_CHECKS=1;

select * from  ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a  WHERE 
a.ID=o.OBJECT_ID and 
a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
and a.class<>"ProbeCoexpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;

delete e, o from  ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a, ACLENTRY e  WHERE 
a.ID=o.OBJECT_ID  
and e.OBJECTIDENTITY_FK=o.ID
and a.EXPERIMENT_ANALYZED_FK=po.OBJECT_ID 
and po.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
and a.class<>"ProbeCoexpressionAnalysisImpl"
and o.PARENT_OBJECT_FK is null;



update ACLOBJECTIDENTITY SET PARENT_OBJECT_FK=1305 WHERE ID=1032323 and PARENT_OBJECT_FK is NULL;

 
-- items which have aces, but shouldn't, and they have a parent object in the acl tables.
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.bioAssay.BioAssayImpl" and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl"  and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl"  and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is not null;
select count(*) from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"  and o.PARENT_OBJECT_FK is not null;


-- delete ones where the parent object is okay - don't need aces for these securedchildren.
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.bioAssay.BioAssayImpl"  and o.PARENT_OBJECT_FK is not null;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl"  and o.PARENT_OBJECT_FK is not null;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" and o.PARENT_OBJECT_FK is not null;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" and o.PARENT_OBJECT_FK is not null;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl"  and o.PARENT_OBJECT_FK is not null;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is not null;
delete e from ACLENTRY e inner join ACLOBJECTIDENTITY o on o.ID=e.OBJECTIDENTITY_FK where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"  and o.PARENT_OBJECT_FK is not null;

update ACLOBJECTIDENTITY set ENTRIES_INHERITING=1 where OBJECT_CLASS="ubic.gemma.model.expression.experiment.FactorValueImpl";
update ACLOBJECTIDENTITY set ENTRIES_INHERITING=1 where OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl";
update ACLOBJECTIDENTITY set ENTRIES_INHERITING=1 where OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl";
update ACLOBJECTIDENTITY set ENTRIES_INHERITING=1 where OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl";
update ACLOBJECTIDENTITY set ENTRIES_INHERITING=1 where OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl";
update ACLOBJECTIDENTITY set ENTRIES_INHERITING=1 where OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" ;


-- later ...
drop table acl_permission;
drop table acl_object_identity;
drop table acl_entry;
drop table acl_class;
drop table acl_sid;

-- remove unused data from CONTACT table.
ALTER TABLE CONTACT drop column U_R_L;
ALTER TABLE CONTACT drop column ADDRESS;
ALTER TABLE CONTACT drop column PHONE;
ALTER TABLE CONTACT drop column TOLL_FREE_PHONE;
ALTER TABLE CONTACT drop column FAX;
ALTER TABLE CONTACT drop foreign key ORGANIZATION_PARENT_FKC;
ALTER TABLE CONTACT drop COLUMN PARENT_FK;



