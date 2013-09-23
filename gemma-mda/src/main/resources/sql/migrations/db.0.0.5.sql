-- fix lingering problems with acls. 

alter table ACLOBJECTIDENTITY add column OLD_OBJECT_CLASS varchar(255);
update ACLOBJECTIDENTITY set OLD_OBJECT_CLASS=OBJECT_CLASS;


update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.ExternalDatabaseEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.GenericEvidenceImpl';
update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl';

-- reverse
update ACLOBJECTIDENTITY SET OBJECT_CLASS = OLD_OBJECT_CLASS WHERE OLD_OBJECT_CLASS is not null  and  OBJECT_CLASS = "ubic.gemma.model.association.phenotype.PhenotypeAssociation";

-- later ...
alter table ACLOBJECTIDENTITY drop column OLD_OBJECT_CLASS ;

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
