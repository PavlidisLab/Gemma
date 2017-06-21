-- Updates for Gemma 1.12

-- ACLOBJECTIDENTITY class rename - contains few overdue changes (e.g. User)
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.auditAndSecurity.User' WHERE OBJECT_CLASS='ubic.gemma.model.common.auditAndSecurity.UserImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.auditAndSecurity.Person' WHERE OBJECT_CLASS='ubic.gemma.model.common.auditAndSecurity.PersonImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.ExpressionExperimentSet' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.auditAndSecurity.UserGroup' WHERE OBJECT_CLASS='ubic.gemma.model.common.auditAndSecurity.UserGroupImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.bioAssay.BioAssay' WHERE OBJECT_CLASS='ubic.gemma.model.expression.bioAssay.BioAssayImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.biomaterial.BioMaterial' WHERE OBJECT_CLASS='ubic.gemma.model.expression.biomaterial.BioMaterialImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.ExperimentalDesign' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.ExperimentalDesignImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.ExperimentalFactor' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.ExperimentalFactorImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.FactorValue' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.FactorValueImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.genome.gene.GeneSet' WHERE OBJECT_CLASS='ubic.gemma.model.genome.gene.GeneSetImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation' WHERE OBJECT_CLASS='ubic.gemma.model.expression.bioAssayData.MeanVarianceRelationImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.protocol.Protocol' WHERE OBJECT_CLASS='ubic.gemma.model.common.protocol.ProtocolImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.description.LocalFile' WHERE OBJECT_CLASS='ubic.gemma.model.common.description.LocalFileImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.auditAndSecurity.JobInfo' WHERE OBJECT_CLASS='ubic.gemma.model.common.auditAndSecurity.JobInfoImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.LiteratureEvidence' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.GenericExperiment' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.GenericExperimentImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.GenericEvidence' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.GenericEvidenceImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.ExperimentalEvidence' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisImpl';

-- ANALYSIS class rename
UPDATE ANALYSIS SET class = 'CoexpressionAnalysis' WHERE class = 'CoexpressionAnalysisImpl';
UPDATE ANALYSIS SET class = 'DifferentialExpressionAnalysis' WHERE class = 'DifferentialExpressionAnalysisImpl';
UPDATE ANALYSIS SET class = 'GeneDifferentialExpressionMetaAnalysis' WHERE class = 'GeneDifferentialExpressionMetaAnalysisImpl';
UPDATE ANALYSIS SET class = 'PrincipalComponentAnalysis' WHERE class = 'PrincipalComponentAnalysisImpl';
UPDATE ANALYSIS SET class = 'SampleCoexpressionAnalysis' WHERE class = 'SampleCoexpressionAnalysisImpl';
-- ANALYSIS_RESULT_SET class rename
UPDATE ANALYSIS_RESULT_SET SET class = 'ExpressionAnalysisResultSet' WHERE class = 'ExpressionAnalysisResultSetImpl';

-- rename all classes in AUDIT_EVENT_TYPE that end with Impl (There is too many of them and I removed all the *Impl types)
UPDATE AUDIT_EVENT_TYPE SET class = LEFT(class, CHAR_LENGTH(class)-4) WHERE RIGHT(class, 4) = 'Impl' ;

-- Manual regex replace using list of short class names of the changed event types:
-- ([a-zA-Z]*Event)\n([a-zA-Z]*EventImpl)
-- UPDATE AUDIT_EVENT_TYPE SET class = '$1' WHERE class = '$2';

-- BIB_REF_ANNOTATION class rename
UPDATE BIB_REF_ANNOTATION SET class = 'MedicalSubjectHeading' WHERE class = 'MedicalSubjectHeadingImpl';
UPDATE BIB_REF_ANNOTATION SET class = 'Keyword' WHERE class = 'KeywordImpl';

-- BIO_SEQUENCE2_GENE_PRODUCT class rename
UPDATE BIO_SEQUENCE2_GENE_PRODUCT SET class = 'BlatAssociation' WHERE class = 'BlatAssociationImpl';
UPDATE BIO_SEQUENCE2_GENE_PRODUCT SET class = 'AnnotationAssociation' WHERE class = 'AnnotationAssociationImpl';

-- CHARACTERISTIC class rename
UPDATE CHARACTERISTIC SET class = 'VocabCharacteristic' WHERE class = 'VocabCharacteristicImpl';
UPDATE CHARACTERISTIC SET class = 'Characteristic' WHERE class = 'CharacteristicImpl';

-- CHARTEMP class rename
UPDATE CHARTEMP SET class = 'VocabCharacteristic' WHERE class = 'VocabCharacteristicImpl';
UPDATE CHARTEMP SET class = 'Characteristic' WHERE class = 'CharacteristicImpl';

-- CHROMOSOME_FEATURE class rename
UPDATE CHROMOSOME_FEATURE SET class = 'Gene' WHERE class = 'GeneImpl';

-- INVESTIGATION class rename
UPDATE INVESTIGATION SET class = 'ExpressionExperimentSubSet' WHERE class = 'ExpressionExperimentSubSetImpl ';
UPDATE INVESTIGATION SET class = 'GenericExperiment' WHERE class = 'GenericExperimentImpl';

-- PHENOTYPE_ASSOCIATION class rename
UPDATE PHENOTYPE_ASSOCIATION SET class = 'ExperimentalEvidence' WHERE class = 'ExperimentalEvidenceImpl';
UPDATE PHENOTYPE_ASSOCIATION SET class = 'LiteratureEvidence' WHERE class = 'LiteratureEvidenceImpl';
UPDATE PHENOTYPE_ASSOCIATION SET class = 'GenericEvidence' WHERE class = 'GenericEvidenceImpl';


