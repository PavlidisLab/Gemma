-- fix lingering problems with acls. 


ALTER TABLE ACLOBJECTIDENTITY
  ADD COLUMN OLD_OBJECT_CLASS VARCHAR(255);
UPDATE ACLOBJECTIDENTITY
SET OLD_OBJECT_CLASS = OBJECT_CLASS;
UPDATE ACLOBJECTIDENTITY
SET OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.PhenotypeAssociation'
WHERE OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl';
UPDATE ACLOBJECTIDENTITY
SET OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.PhenotypeAssociation'
WHERE OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl';
UPDATE ACLOBJECTIDENTITY
SET OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.PhenotypeAssociation'
WHERE OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.ExternalDatabaseEvidenceImpl';
UPDATE ACLOBJECTIDENTITY
SET OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.PhenotypeAssociation'
WHERE OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.GenericEvidenceImpl';
UPDATE ACLOBJECTIDENTITY
SET OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.PhenotypeAssociation'
WHERE OBJECT_CLASS = 'ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl';

-- reverse
-- update ACLOBJECTIDENTITY SET OBJECT_CLASS = OLD_OBJECT_CLASS WHERE OLD_OBJECT_CLASS is not null  and  OBJECT_CLASS = "ubic.gemma.model.association.phenotype.PhenotypeAssociation";

-- later ...
ALTER TABLE ACLOBJECTIDENTITY
  DROP COLUMN OLD_OBJECT_CLASS;

-- fill in parent for expressionexperimentsubset

SELECT DISTINCT *
FROM ACLOBJECTIDENTITY o, INVESTIGATION subset, ACLOBJECTIDENTITY op
WHERE o.OBJECT_ID = subset.ID AND subset.SOURCE_EXPERIMENT_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl" AND
      op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
      AND o.PARENT_OBJECT_FK IS NULL
LIMIT 1;

UPDATE ACLOBJECTIDENTITY o, INVESTIGATION subset, ACLOBJECTIDENTITY op
SET o.PARENT_OBJECT_FK = op.ID
WHERE o.OBJECT_ID = subset.ID AND subset.SOURCE_EXPERIMENT_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl" AND
      op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
      AND o.PARENT_OBJECT_FK IS NULL;

-- fill in parent for analyses


SELECT count(*)
FROM ACLOBJECTIDENTITY
WHERE
  OBJECT_CLASS = 'ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl' AND PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLOBJECTIDENTITY
WHERE OBJECT_CLASS = 'ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl' AND
      PARENT_OBJECT_FK IS NOT NULL;

SELECT count(*)
FROM ACLOBJECTIDENTITY
WHERE OBJECT_CLASS = 'ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl' AND
      PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLOBJECTIDENTITY
WHERE OBJECT_CLASS = 'ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl' AND
      PARENT_OBJECT_FK IS NOT NULL;

SELECT count(*)
FROM ACLOBJECTIDENTITY
WHERE OBJECT_CLASS = 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl' AND
      PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLOBJECTIDENTITY
WHERE OBJECT_CLASS = 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl' AND
      PARENT_OBJECT_FK IS NOT NULL;

-- coexpression

SELECT *
FROM ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op
WHERE o.OBJECT_ID = an.ID AND an.EXPERIMENT_ANALYZED_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
      AND op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
      AND o.PARENT_OBJECT_FK IS NULL
LIMIT 1;
-- none.

-- orphans:
SELECT count(*)
FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

DELETE e FROM ACLENTRY e
  JOIN ACLOBJECTIDENTITY o ON e.OBJECTIDENTITY_FK = o.ID
  LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

DELETE o FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

-- -----------------------------

SELECT *
FROM ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op
WHERE o.OBJECT_ID = an.ID AND an.EXPERIMENT_ANALYZED_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"
      AND op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
      AND o.PARENT_OBJECT_FK IS NULL
LIMIT 1;

-- these are messed up because they never leave the dao - so they don't actually need security.
SELECT count(*)
FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

SELECT count(*)
FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

DELETE e FROM ACLENTRY e
  JOIN ACLOBJECTIDENTITY o ON e.OBJECTIDENTITY_FK = o.ID
  LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

DELETE o FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

-- -----------------------------------

SELECT *
FROM ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op
WHERE o.OBJECT_ID = an.ID AND an.EXPERIMENT_ANALYZED_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
      AND op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
      AND o.PARENT_OBJECT_FK IS NULL
LIMIT 1;
-- none

-- orphans:
SELECT count(*)
FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

SELECT count(*)
FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
      AND an.ID IS NULL;

DELETE e FROM ACLENTRY e
  JOIN ACLOBJECTIDENTITY o ON e.OBJECTIDENTITY_FK = o.ID
  LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

DELETE o FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

-- ---------------

SELECT *
FROM ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op
WHERE o.OBJECT_ID = an.ID AND an.EXPERIMENT_ANALYZED_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
      AND op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
      AND o.PARENT_OBJECT_FK IS NULL
LIMIT 1;
-- none

-- orphans:
SELECT count(*)
FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
      AND o.PARENT_OBJECT_FK IS NULL AND an.ID IS NULL;

SELECT count(*)
FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
      AND an.ID IS NULL;

DELETE e FROM ACLENTRY e
  JOIN ACLOBJECTIDENTITY o ON e.OBJECTIDENTITY_FK = o.ID
  LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
      AND an.ID IS NULL;

DELETE o FROM ACLOBJECTIDENTITY o LEFT OUTER JOIN ANALYSIS an ON o.OBJECT_ID = an.ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
      AND an.ID IS NULL;

-- ---------------
-- -for subsets.
SELECT *
FROM ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op
WHERE o.OBJECT_ID = an.ID AND an.EXPERIMENT_ANALYZED_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
      AND op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl"
      AND o.PARENT_OBJECT_FK IS NULL
LIMIT 1;

UPDATE ACLOBJECTIDENTITY o, ANALYSIS an, ACLOBJECTIDENTITY op
SET o.PARENT_OBJECT_FK = op.ID
WHERE o.OBJECT_ID = an.ID AND an.EXPERIMENT_ANALYZED_FK = op.OBJECT_ID
      AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
      AND op.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl"
      AND o.PARENT_OBJECT_FK IS NULL;

-- removing columns that are no longer needed.

-- already ran these.
ALTER TABLE HUMAN_PROBE_CO_EXPRESSION
  MODIFY COLUMN PVALUE DOUBLE;
ALTER TABLE MOUSE_PROBE_CO_EXPRESSION
  MODIFY COLUMN PVALUE DOUBLE;
ALTER TABLE RAT_PROBE_CO_EXPRESSION
  MODIFY COLUMN PVALUE DOUBLE;
ALTER TABLE OTHER_PROBE_CO_EXPRESSION
  MODIFY COLUMN PVALUE DOUBLE;
ALTER TABLE USER_PROBE_CO_EXPRESSION
  MODIFY COLUMN PVALUE DOUBLE;

-- only after the model is migrated.
ALTER TABLE GENE2GO_ASSOCIATION
  DROP COLUMN SOURCE_ANALYSIS_FK;
ALTER TABLE PAZAR_ASSOCIATION
  DROP COLUMN SOURCE_ANALYSIS_FK;
ALTER TABLE GENE2_GENE_PROTEIN_ASSOCIATION
  DROP COLUMN SOURCE_ANALYSIS_FK;

ALTER TABLE HUMAN_GENE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE HUMAN_PROBE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE MOUSE_GENE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE MOUSE_PROBE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE RAT_GENE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE RAT_PROBE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE OTHER_GENE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE OTHER_PROBE_CO_EXPRESSION
  DROP COLUMN PVALUE;
ALTER TABLE USER_PROBE_CO_EXPRESSION
  DROP COLUMN PVALUE;
-- cruft
DROP TABLE LITERATURE_ASSOCIATION;
DROP TABLE GENE_HOMOLOGY;

ALTER TABLE PROTOCOL
  DROP COLUMN TYPE_FK;
ALTER TABLE PROTOCOL
  DROP COLUMN U_R_I;





