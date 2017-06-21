-- ! execute before updating to version after db.0.0.14 !

-- Change by Paul, moved to this file from .12_postponed, since the changes in that file were already executed.
ALTER TABLE ANALYSIS_RESULT_SET DROP COLUMN QVALUE_THRESHOLD_FOR_STORAGE;

-- Unused table
DROP TABLE IF EXISTS COEXPRESSION_PROBE;

-- Non-existing instances
-- TODO make sure these are not referenced anywhere
DELETE FROM ANALYSIS WHERE class='ProbeCoexpressionAnalysisImpl';
DELETE FROM ANALYSIS WHERE class='GeneCoexpressionAnalysisImpl';

-- not sure about this one
-- update ACLOBJECTIDENTITY  set OBJECT_CLASS='ubic.gemma.model.association.phenotype.PhenotypeAssociation' where OBJECT_CLASS='ubic.gemma.model.association.phenotype.ExternalDatabaseEvidenceImpl';

-- TODO add commands to remove the ProbeCoexp. and GeneCoexp. analyses from ACLOBJECTIDENTITY - note foreign key refs (probably from ACLENTRY)