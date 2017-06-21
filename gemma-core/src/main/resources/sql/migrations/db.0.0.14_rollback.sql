-- Prepared rollback to db.0.0.13, in case something goes very wrong with the release

-- roll back ACLOBJECTIDENTITY - skipping User and Person, since those were fixes that should have happened in db.0.0.13.sql
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetImpl' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisImpl' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.ExpressionExperimentSet';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.auditAndSecurity.UserGroupImpl' WHERE OBJECT_CLASS='ubic.gemma.model.common.auditAndSecurity.UserGroup';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.bioAssay.BioAssayImpl' WHERE OBJECT_CLASS='ubic.gemma.model.expression.bioAssay.BioAssay';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.biomaterial.BioMaterialImpl' WHERE OBJECT_CLASS='ubic.gemma.model.expression.biomaterial.BioMaterial';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.ExperimentalDesignImpl' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.ExperimentalDesign';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.ExperimentalFactorImpl' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.ExperimentalFactor';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetImpl' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.experiment.FactorValueImpl' WHERE OBJECT_CLASS='ubic.gemma.model.expression.experiment.FactorValue';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.genome.gene.GeneSetImpl' WHERE OBJECT_CLASS='ubic.gemma.model.genome.gene.GeneSet';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.expression.bioAssayData.MeanVarianceRelationImpl' WHERE OBJECT_CLASS='ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.protocol.ProtocolImpl' WHERE OBJECT_CLASS='ubic.gemma.model.common.protocol.Protocol';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.description.LocalFileImpl' WHERE OBJECT_CLASS='ubic.gemma.model.common.description.LocalFile';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.common.auditAndSecurity.JobInfoImpl' WHERE OBJECT_CLASS='ubic.gemma.model.common.auditAndSecurity.JobInfo';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.LiteratureEvidenceImpl' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.LiteratureEvidence';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.GenericExperimentImpl' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.GenericExperiment';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.GenericEvidenceImpl' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.GenericEvidence';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl' WHERE OBJECT_CLASS='ubic.gemma.model.association.phenotype.ExperimentalEvidence';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis';
UPDATE ACLOBJECTIDENTITY SET `OBJECT_CLASS` = 'ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisImpl' WHERE OBJECT_CLASS='ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis';

-- ANALYSIS class rename
UPDATE ANALYSIS SET class = 'CoexpressionAnalysisImpl' WHERE class = 'CoexpressionAnalysis';
UPDATE ANALYSIS SET class = 'DifferentialExpressionAnalysisImpl' WHERE class = 'DifferentialExpressionAnalysis';
UPDATE ANALYSIS SET class = 'GeneDifferentialExpressionMetaAnalysisImpl' WHERE class = 'GeneDifferentialExpressionMetaAnalysis';
UPDATE ANALYSIS SET class = 'PrincipalComponentAnalysisImpl' WHERE class = 'PrincipalComponentAnalysis';
UPDATE ANALYSIS SET class = 'SampleCoexpressionAnalysisImpl' WHERE class = 'SampleCoexpressionAnalysis';
-- ANALYSIS_RESULT_SET class rename
UPDATE ANALYSIS_RESULT_SET SET class = 'ExpressionAnalysisResultSet' WHERE class = 'ExpressionAnalysisResultSetImpl';

-- rename classes in AUDIT_EVENT_TYPE back to their Impl versions
UPDATE AUDIT_EVENT_TYPE SET class = 'AlignmentBasedGeneMappingEventImpl' WHERE class = 'AlignmentBasedGeneMappingEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'AnnotationBasedGeneMappingEventImpl' WHERE class = 'AnnotationBasedGeneMappingEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'AnnotationEventImpl' WHERE class = 'AnnotationEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignAnalysisEventImpl' WHERE class = 'ArrayDesignAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignAnnotationFileEventImpl' WHERE class = 'ArrayDesignAnnotationFileEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignGeneMappingEventImpl' WHERE class = 'ArrayDesignGeneMappingEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignMergeEventImpl' WHERE class = 'ArrayDesignMergeEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignProbeRenamingEventImpl' WHERE class = 'ArrayDesignProbeRenamingEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignRepeatAnalysisEventImpl' WHERE class = 'ArrayDesignRepeatAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignSequenceAnalysisEventImpl' WHERE class = 'ArrayDesignSequenceAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignSequenceRemoveEventImpl' WHERE class = 'ArrayDesignSequenceRemoveEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignSequenceUpdateEventImpl' WHERE class = 'ArrayDesignSequenceUpdateEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ArrayDesignSubsumeCheckEventImpl' WHERE class = 'ArrayDesignSubsumeCheckEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'AuditEventTypeImpl' WHERE class = 'AuditEventType';
UPDATE AUDIT_EVENT_TYPE SET class = 'AutomatedAnnotationEventImpl' WHERE class = 'AutomatedAnnotationEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'BatchCorrectionEventImpl' WHERE class = 'BatchCorrectionEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'BatchInformationFetchingEventImpl' WHERE class = 'BatchInformationFetchingEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'BioMaterialMappingUpdateImpl' WHERE class = 'BioMaterialMappingUpdate';
UPDATE AUDIT_EVENT_TYPE SET class = 'CommentedEventImpl' WHERE class = 'CommentedEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'DataAddedEventImpl' WHERE class = 'DataAddedEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'DataReplacedEventImpl' WHERE class = 'DataReplacedEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'DifferentialExpressionAnalysisEventImpl' WHERE class = 'DifferentialExpressionAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ExpressionExperimentAnalysisEventImpl' WHERE class = 'ExpressionExperimentAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ExpressionExperimentPlatformSwitchEventImpl' WHERE class = 'ExpressionExperimentPlatformSwitchEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ExpressionExperimentVectorMergeEventImpl' WHERE class = 'ExpressionExperimentVectorMergeEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'FailedBatchInformationFetchingEventImpl' WHERE class = 'FailedBatchInformationFetchingEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'FailedBatchInformationMissingEventImpl' WHERE class = 'FailedBatchInformationMissingEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'LinkAnalysisEventImpl' WHERE class = 'LinkAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ManualAnnotationEventImpl' WHERE class = 'ManualAnnotationEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'MissingValueAnalysisEventImpl' WHERE class = 'MissingValueAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'OutlierAnalysisEventImpl' WHERE class = 'OutlierAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'OutlierFoundAnalysisEventImpl' WHERE class = 'OutlierFoundAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'OutliersNotFoundAnalysisEventImpl' WHERE class = 'OutliersNotFoundAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'PCAAnalysisEventImpl' WHERE class = 'PCAAnalysisEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'ProcessedVectorComputationEventImpl' WHERE class = 'ProcessedVectorComputationEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'RankComputationEventImpl' WHERE class = 'RankComputationEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'SampleRemovalEventImpl' WHERE class = 'SampleRemovalEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'SampleRemovalReversionEventImpl' WHERE class = 'SampleRemovalReversionEvent';
UPDATE AUDIT_EVENT_TYPE SET class = 'TooSmallDatasetLinkAnalysisEventImpl' WHERE class = 'TooSmallDatasetLinkAnalysisEvent';

-- BIB_REF_ANNOTATION class rename
UPDATE BIB_REF_ANNOTATION SET class = 'MedicalSubjectHeadingImpl' WHERE class = 'MedicalSubjectHeading';
UPDATE BIB_REF_ANNOTATION SET class = 'KeywordImpl' WHERE class = 'Keyword';

-- BIO_SEQUENCE2_GENE_PRODUCT class rename
UPDATE BIO_SEQUENCE2_GENE_PRODUCT SET class = 'BlatAssociationImpl' WHERE class = 'BlatAssociation';
UPDATE BIO_SEQUENCE2_GENE_PRODUCT SET class = 'AnnotationAssociationImpl' WHERE class = 'AnnotationAssociation';

-- CHARACTERISTIC class rename
UPDATE CHARACTERISTIC SET class = 'VocabCharacteristicImpl' WHERE class = 'VocabCharacteristic';
UPDATE CHARACTERISTIC SET class = 'CharacteristicImpl' WHERE class = 'Characteristic';

-- CHARTEMP class rename
UPDATE CHARTEMP SET class = 'VocabCharacteristicImpl' WHERE class = 'VocabCharacteristic';
UPDATE CHARTEMP SET class = 'CharacteristicImpl' WHERE class = 'Characteristic';

-- CHROMOSOME_FEATURE class rename
UPDATE CHROMOSOME_FEATURE SET class = 'GeneImpl' WHERE class = 'Gene';

-- INVESTIGATION class rename
UPDATE INVESTIGATION SET class = 'ExpressionExperimentSubSet' WHERE class = 'ExpressionExperimentSubSetImpl ';
UPDATE INVESTIGATION SET class = 'GenericExperimentImpl' WHERE class = 'GenericExperiment';

-- PHENOTYPE_ASSOCIATION class rename
UPDATE PHENOTYPE_ASSOCIATION SET class = 'ExperimentalEvidenceImpl' WHERE class = 'ExperimentalEvidence';
UPDATE PHENOTYPE_ASSOCIATION SET class = 'LiteratureEvidenceImpl' WHERE class = 'LiteratureEvidence';
UPDATE PHENOTYPE_ASSOCIATION SET class = 'GenericEvidenceImpl' WHERE class = 'GenericEvidence';