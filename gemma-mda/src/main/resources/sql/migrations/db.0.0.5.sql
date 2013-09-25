-- fix lingering problems with acls. 






alter table ACLOBJECTIDENTITY add column OLD_OBJECT_CLASS varchar(255);
update ACLOBJECTIDENTITY set OLD_OBJECT_CLASS=OBJECT_CLASS;
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.ExternalDatabaseEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.GenericEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl';

-- reverse
--update ACLOBJECTIDENTITY SET OBJECT_CLASS = OLD_OBJECT_CLASS WHERE OLD_OBJECT_CLASS is not null  and  OBJECT_CLASS = "ubic.gemma.model.association.phenotype.PhenotypeAssociation";

-- later ...
alter table ACLOBJECTIDENTITY drop column OLD_OBJECT_CLASS ;

-- fill in parent for expressionexperimentsubset

select distinct * from  ACLOBJECTIDENTITY o, INVESTIGATION subset, ACLOBJECTIDENTITY op  
 where o.OBJECT_ID=subset.ID  and subset.SOURCE_EXPERIMENT_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl" and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" 
 and o.PARENT_OBJECT_FK IS NULL limit 1 \G
 
update ACLOBJECTIDENTITY o, INVESTIGATION subset, ACLOBJECTIDENTITY op set o.PARENT_OBJECT_FK=op.ID
 where o.OBJECT_ID=subset.ID  and subset.SOURCE_EXPERIMENT_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl" and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
 and o.PARENT_OBJECT_FK IS NULL;
 
 -- fill in parent for analyses
 
 
 select count(*) from ACLOBJECTIDENTITY where OBJECT_CLASS='ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl' and PARENT_OBJECT_FK is null;
 select count(*) from ACLOBJECTIDENTITY where OBJECT_CLASS='ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl' and PARENT_OBJECT_FK is not null;
 
 select count(*) from ACLOBJECTIDENTITY where OBJECT_CLASS='ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl' and PARENT_OBJECT_FK is null;
 select count(*) from ACLOBJECTIDENTITY where OBJECT_CLASS='ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl' and PARENT_OBJECT_FK is not null;
 
 select count(*) from ACLOBJECTIDENTITY where OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl' and PARENT_OBJECT_FK is null;
 select count(*) from ACLOBJECTIDENTITY where OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl' and PARENT_OBJECT_FK is not null;
 
 --- coexpression
 
select * from ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op 
 where o.OBJECT_ID=an.ID  and an.EXPERIMENT_ANALYZED_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" 
 and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" 
 and o.PARENT_OBJECT_FK IS NULL limit 1 \G
 -- none.
 
 -- orphans:
 select count(*) from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
  delete e from ACLENTRY e join ACLOBJECTIDENTITY o on e.OBJECTIDENTITY_FK=o.ID   left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
  delete o from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
 
 -----------------------------
 
 select * from ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op 
 where o.OBJECT_ID=an.ID  and an.EXPERIMENT_ANALYZED_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" 
 and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" 
 and o.PARENT_OBJECT_FK IS NULL limit 1 \G
 
 -- these are messed up because they never leave the dao - so they don't actually need security.
 select count(*) from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
  select count(*) from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
 
  delete e from ACLENTRY e join ACLOBJECTIDENTITY o on e.OBJECTIDENTITY_FK=o.ID left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
 delete o from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
 -----------------------------------
 
 select * from ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op 
 where o.OBJECT_ID=an.ID  and an.EXPERIMENT_ANALYZED_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" 
 and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" 
 and o.PARENT_OBJECT_FK IS NULL limit 1 \G
 -- none
 
 --orphans:
 select count(*) from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
  select count(*) from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" 
   and an.ID is null;
  
 delete e from ACLENTRY e join ACLOBJECTIDENTITY o on e.OBJECTIDENTITY_FK=o.ID left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
 delete o from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
 
  
  ---------------
  
  select * from ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op 
 where o.OBJECT_ID=an.ID  and an.EXPERIMENT_ANALYZED_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" 
 and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" 
 and o.PARENT_OBJECT_FK IS NULL limit 1 \G
 -- none
 
 --orphans:
 select count(*) from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" 
  and o.PARENT_OBJECT_FK IS NULL and an.ID is null;
  
  select count(*) from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" 
    and an.ID is null;
  
 delete e from ACLENTRY e join ACLOBJECTIDENTITY o on e.OBJECTIDENTITY_FK=o.ID left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" 
   and an.ID is null;
  
 delete o from ACLOBJECTIDENTITY o left outer join ANALYSIS an on o.OBJECT_ID=an.ID 
 where o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" 
  and an.ID is null;
  
  
  ---------------
  -- -for subsets.
  select * from ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op 
 where o.OBJECT_ID=an.ID  and an.EXPERIMENT_ANALYZED_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" 
 and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl" 
 and o.PARENT_OBJECT_FK IS NULL limit 1 \G
 
 update ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op set o.PARENT_OBJECT_FK=op.ID
where o.OBJECT_ID=an.ID  and an.EXPERIMENT_ANALYZED_FK=op.OBJECT_ID 
 and o.OBJECT_CLASS="ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" 
 and op.OBJECT_CLASS="ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl" 
 and o.PARENT_OBJECT_FK IS NULL;
 
 
-- removing columns that are no longer needed.

-- already ran these.
alter table HUMAN_PROBE_CO_EXPRESSION modify column PVALUE double;
alter table MOUSE_PROBE_CO_EXPRESSION modify column PVALUE double;
alter table RAT_PROBE_CO_EXPRESSION modify column PVALUE double;
alter table OTHER_PROBE_CO_EXPRESSION modify column PVALUE double;
alter table USER_PROBE_CO_EXPRESSION modify column PVALUE double;


-- only after the model is migrated.
alter table GENE2GO_ASSOCIATION drop column SOURCE_ANALYSIS_FK;
alter table PAZAR_ASSOCIATION drop column SOURCE_ANALYSIS_FK;
alter table GENE2_GENE_PROTEIN_ASSOCIATION drop column SOURCE_ANALYSIS_FK;

alter table HUMAN_GENE_CO_EXPRESSION drop column PVALUE;
alter table HUMAN_PROBE_CO_EXPRESSION drop column PVALUE;
alter table MOUSE_GENE_CO_EXPRESSION drop column PVALUE;
alter table MOUSE_PROBE_CO_EXPRESSION drop column PVALUE;
alter table RAT_GENE_CO_EXPRESSION drop column PVALUE;
alter table RAT_PROBE_CO_EXPRESSION drop column PVALUE;
alter table OTHER_GENE_CO_EXPRESSION drop column PVALUE;
alter table OTHER_PROBE_CO_EXPRESSION drop column PVALUE;
alter table USER_PROBE_CO_EXPRESSION drop column PVALUE;
-- cruft
drop table LITERATURE_ASSOCIATION;
drop table GENE_HOMOLOGY;

alter table PROTOCOL drop column TYPE_FK;
alter table PROTOCOL drop column U_R_I;





