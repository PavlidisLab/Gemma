
/**
 * dwrServices.js
 * 
 * Manual bundling of DWR interfaces into one file. This is necessary because DWR 2 does not support any bundling; and
 * JAWR does not currently support DWR+Spring.
 * 
 * $Id$
 */

if (dwr == null)
	var dwr = {};
if (dwr.engine == null)
	dwr.engine = {};
if (DWREngine == null)
	var DWREngine = dwr.engine;
// ====================================================================================
if (AnnotationController == null)
	var AnnotationController = {};
AnnotationController._path = '/Gemma/dwr';
AnnotationController.autoTag = function(p0, callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'autoTag', p0, callback);
}
AnnotationController.findTerm = function(p0, p1, p2, callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'findTerm', p0, p1, p2, callback);
}
AnnotationController.createExperimentTag = function(p0, p1, callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'createExperimentTag', p0, p1, callback);
}
AnnotationController.removeExperimentTag = function(p0, p1, callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'removeExperimentTag', p0, p1, callback);
}
AnnotationController.createBioMaterialTag = function(p0, p1, callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'createBioMaterialTag', p0, p1, callback);
}
AnnotationController.removeBioMaterialTag = function(p0, p1, callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'removeBioMaterialTag', p0, p1, callback);
}
AnnotationController.reinitializeOntologyIndices = function(callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'reinitializeOntologyIndices', callback);
}
AnnotationController.validateTags = function(p0, callback) {
	dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'validateTags', p0, callback);
}

// ====================================================================================
if (ArrayDesignController == null)
	var ArrayDesignController = {};
ArrayDesignController._path = '/Gemma/dwr';
ArrayDesignController.remove = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'remove', p0, callback);
}
ArrayDesignController.getArrayDesigns = function(p0, p1, p2, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getArrayDesigns', p0, p1, p2, callback);
}
ArrayDesignController.loadArrayDesignsForShowAll = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'loadArrayDesignsForShowAll', p0, callback);
}
ArrayDesignController.addAlternateName = function(p0, p1, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'addAlternateName', p0, p1, callback);
}
ArrayDesignController.getCsSummaries = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getCsSummaries', p0, callback);
}
ArrayDesignController.getReportHtml = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getReportHtml', p0, callback);
}
ArrayDesignController.updateReport = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'updateReport', p0, callback);
}
ArrayDesignController.getSummaryForArrayDesign = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getSummaryForArrayDesign', p0, callback);
}
ArrayDesignController.loadArrayDesignsSummary = function(callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'loadArrayDesignsSummary', callback);
}
ArrayDesignController.updateReportById = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'updateReportById', p0, callback);
}
// ====================================================================================
if (ArrayDesignRepeatScanController == null)
	var ArrayDesignRepeatScanController = {};
ArrayDesignRepeatScanController._path = '/Gemma/dwr';
ArrayDesignRepeatScanController.run = function(p0, callback) {
	dwr.engine._execute(ArrayDesignRepeatScanController._path, 'ArrayDesignRepeatScanController', 'run', p0, callback);
}
ArrayDesignRepeatScanController.run = function(p0, callback) {
	dwr.engine._execute(ArrayDesignRepeatScanController._path, 'ArrayDesignRepeatScanController', 'run', p0, callback);
}
// ====================================================================================
if (AuditController == null)
	var AuditController = {};
AuditController._path = '/Gemma/dwr';
AuditController.addAuditEvent = function(p0, p1, p2, p3, callback) {
	dwr.engine._execute(AuditController._path, 'AuditController', 'addAuditEvent', p0, p1, p2, p3, callback);
}
AuditController.getAuditable = function(p0, callback) {
	dwr.engine._execute(AuditController._path, 'AuditController', 'getAuditable', p0, callback);
}
AuditController.getEvents = function(p0, callback) {
	dwr.engine._execute(AuditController._path, 'AuditController', 'getEvents', p0, callback);
}

// ====================================================================================
if (BatchInfoFetchController == null)
	var BatchInfoFetchController = {};
BatchInfoFetchController._path = '/Gemma/dwr';
BatchInfoFetchController.run = function(p0, callback) {
	dwr.engine
			._execute(BatchInfoFetchController._path, 'BatchInfoFetchController', 'run', p0, callback);
}
// ====================================================================================
if (BibliographicReferenceController == null)
	var BibliographicReferenceController = {};
BibliographicReferenceController._path = '/Gemma/dwr';
BibliographicReferenceController.update = function(p0, callback) {
	dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'update', p0,
			callback);
}
BibliographicReferenceController.browse = function(p0, callback) {
	dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'browse', p0,
			callback);
}
BibliographicReferenceController.load = function(p0, callback) {
	dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'load', p0,
			callback);
}
BibliographicReferenceController.search = function(p0, callback) {
	dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'search', p0,
			callback);
}
BibliographicReferenceController.loadFromPubmedID = function(p0, callback) {
		dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'loadFromPubmedID', p0,
				callback);
}
// ====================================================================================
if (BioAssayController == null)
	var BioAssayController = {};
BioAssayController._path = '/Gemma/dwr';
BioAssayController.markOutlier = function(p0, callback) {
	dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'markOutlier', p0, callback);
}
BioAssayController.getBioAssays = function(p0, callback) {
	dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'getBioAssays', p0, callback);
}

// ====================================================================================
if (BioMaterialController == null)
	var BioMaterialController = {};
BioMaterialController._path = '/Gemma/dwr';
BioMaterialController.getAnnotation = function(p0, callback) {
	dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getAnnotation', p0, callback);
}
BioMaterialController.getFactorValues = function(p0, callback) {
	dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getFactorValues', p0, callback);
}
BioMaterialController.getBioMaterials = function(p0, callback) {
	dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getBioMaterials', p0, callback);
}
BioMaterialController.addFactorValueTo = function(p0, p1, callback) {
	dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'addFactorValueTo', p0, p1, callback);
}
// ====================================================================================
if (CharacteristicBrowserController == null)
	var CharacteristicBrowserController = {};
CharacteristicBrowserController._path = '/Gemma/dwr';
CharacteristicBrowserController.findCharacteristics = function(p0, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
			'findCharacteristics', p0, callback);
}
CharacteristicBrowserController.findCharacteristicsCustom = function(p0, p1, p2, p3, p4, p5, p6, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
			'findCharacteristicsCustom', p0, p1, p2, p3, p4, p5, p6, callback);
}
CharacteristicBrowserController.removeCharacteristics = function(p0, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
			'removeCharacteristics', p0, callback);
}
CharacteristicBrowserController.updateCharacteristics = function(p0, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
			'updateCharacteristics', p0, callback);
}
CharacteristicBrowserController.browse = function(p0, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'browse', p0,
			callback);
}
CharacteristicBrowserController.count = function(callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'count', callback);
}
// ====================================================================================
if (CompositeSequenceController == null)
	var CompositeSequenceController = {};
CompositeSequenceController._path = '/Gemma/dwr';
CompositeSequenceController.search = function(p0, p1, callback) {
	dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'search', p0, p1, callback);
}
CompositeSequenceController.getCsSummaries = function(p0, callback) {
	dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getCsSummaries', p0,
			callback);
}
CompositeSequenceController.getGeneMappingSummary = function(p0, callback) {
	dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getGeneMappingSummary', p0,
			callback);
}
// ====================================================================================
if (IndexService == null)
	var IndexService = {};
IndexService._path = '/Gemma/dwr';
IndexService.index = function(p0, callback) {
	dwr.engine._execute(IndexService._path, 'IndexService', 'index', p0, callback);
}
// ====================================================================================
if (DEDVController == null)
	var DEDVController = {};
DEDVController._path = '/Gemma/dwr';
DEDVController.getDEDV = function(p0, p1, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDV', p0, p1, callback);
}
DEDVController.getDEDVForCoexpressionVisualization = function(p0, p1, p2, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForCoexpressionVisualization', p0, p1, p2,
			callback);
}
DEDVController.getDEDVForDiffExVisualization = function(p0, p1, p2, p3, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualization', p0, p1, p2, p3,
			callback);
}
DEDVController.getDEDVForVisualization = function(p0, p1, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForVisualization', p0, p1, callback);
}
DEDVController.getVectorData = function(p0, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getVectorData', p0, callback);
}
DEDVController.getDEDVForDiffExVisualizationByThreshold = function(p0, p1,  callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualizationByThreshold', p0, p1, 
			callback);
}
DEDVController.getDEDVForPcaVisualization = function(p0, p1, p2, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForPcaVisualization', p0, p1, p2,
			callback);
}
DEDVController.getDEDVForDiffExVisualizationByExperiment = function(p0, p1, p2, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualizationByExperiment', p0, p1,
			p2, callback);
}

// ====================================================================================
if (DifferentialExpressionAnalysisController == null)
	var DifferentialExpressionAnalysisController = {};
DifferentialExpressionAnalysisController._path = '/Gemma/dwr';
DifferentialExpressionAnalysisController.run = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'run', p0, callback);
};
DifferentialExpressionAnalysisController.determineAnalysisType = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'determineAnalysisType', p0, callback);
};
DifferentialExpressionAnalysisController.runCustom = function(p0, p1, p2, p3, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'runCustom', p0, p1, p2, p3, callback);
};
DifferentialExpressionAnalysisController.run = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'run', p0, callback);
};
DifferentialExpressionAnalysisController.remove = function(p0, p1, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'remove', p0, p1, callback);
};
DifferentialExpressionAnalysisController.redo = function(p0, p1, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'redo', p0, p1, callback);
};
DifferentialExpressionAnalysisController.refreshStats = function(p0, p1, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'refreshStats', p0, p1, callback);
};
// ====================================================================================
if (DifferentialExpressionSearchController == null)
	var DifferentialExpressionSearchController = {};
DifferentialExpressionSearchController._path = '/Gemma/dwr';
DifferentialExpressionSearchController.getDifferentialExpression = function(p0, p1, p2, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDifferentialExpression', p0, p1, p2, callback);
}
DifferentialExpressionSearchController.getDifferentialExpressionWithoutBatch = function(p0, p1, p2, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDifferentialExpressionWithoutBatch', p0, p1, p2, callback);
}
DifferentialExpressionSearchController.getDiffExpressionForGenes = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDiffExpressionForGenes', p0, callback);
}
DifferentialExpressionSearchController.getDifferentialExpressionForFactors = function(p0, p1, p2, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDifferentialExpressionForFactors', p0, p1, p2, callback);
}
DifferentialExpressionSearchController.getFactors = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getFactors', p0, callback);
}
DifferentialExpressionSearchController.differentialExpressionAnalysisVisualizationSearch = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'differentialExpressionAnalysisVisualizationSearch', p0, p1, p2, p3, p4, callback);
}
DifferentialExpressionSearchController.geneConditionSearch = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'geneConditionSearch', p0, p1, p2, p3, p4, callback); // FIXME not used?
}

DifferentialExpressionSearchController.scheduleDiffExpSearchTask = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'scheduleDiffExpSearchTask', p0, p1, p2, p3, p4, callback);
}

DifferentialExpressionSearchController.getDiffExpSearchResult = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDiffExpSearchResult', p0, callback);
}

DifferentialExpressionSearchController.getDiffExpSearchTaskProgress = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDiffExpSearchTaskProgress', p0, callback);
}
// ====================================================================================
if (DiffExMetaAnalyzerController == null)
	var DiffExMetaAnalyzerController = {};
DiffExMetaAnalyzerController._path = '/Gemma/dwr';
DiffExMetaAnalyzerController.analyzeResultSets = function(p0, callback) {
	dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController',
			'analyzeResultSets', p0, callback);
};
DiffExMetaAnalyzerController.findDetailMetaAnalysisById = function(p0, callback) {
	dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController',
			'findDetailMetaAnalysisById', p0, callback);
};
DiffExMetaAnalyzerController.findMyMetaAnalyses = function(callback) {
	dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController',
			'findMyMetaAnalyses', callback);
};
DiffExMetaAnalyzerController.removeMetaAnalysis = function(p0, callback) {
	dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController',
			'removeMetaAnalysis', p0, callback);
};
DiffExMetaAnalyzerController.saveResultSets = function(p0, p1, p2, callback) {
	dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController',
			'saveResultSets', p0, p1, p2, callback);
};

// ====================================================================================
if (ExperimentalDesignController == null)
	var ExperimentalDesignController = {};
ExperimentalDesignController._path = '/Gemma/dwr';
ExperimentalDesignController.updateBioMaterials = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'updateBioMaterials', p0,
			callback);
}
ExperimentalDesignController.getFactorValues = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'getFactorValues', p0,
			callback);
}
ExperimentalDesignController.getExperimentalFactors = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'getExperimentalFactors',
			p0, callback);
}
ExperimentalDesignController.getBioMaterials = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'getBioMaterials', p0,
			callback);
}
ExperimentalDesignController.createDesignFromFile = function(p0, p1, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'createDesignFromFile', p0,
			p1, callback);
}
ExperimentalDesignController.createExperimentalFactor = function(p0, p1, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'createExperimentalFactor',
			p0, p1, callback);
}
ExperimentalDesignController.createFactorValue = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'createFactorValue', p0,
			callback);
}
ExperimentalDesignController.createFactorValueCharacteristic = function(p0, p1, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
			'createFactorValueCharacteristic', p0, p1, callback);
}
ExperimentalDesignController.deleteExperimentalFactors = function(pO, p1, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
			'deleteExperimentalFactors', pO, p1, callback);
}
ExperimentalDesignController.deleteFactorValueCharacteristics = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
			'deleteFactorValueCharacteristics', p0, callback);
}
ExperimentalDesignController.deleteFactorValues = function(p0, p1, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'deleteFactorValues', p0,
			p1, callback);
}
ExperimentalDesignController.getFactorValuesWithCharacteristics = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
			'getFactorValuesWithCharacteristics', p0, callback);
}
ExperimentalDesignController.updateExperimentalFactors = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
			'updateExperimentalFactors', p0, callback);
}
ExperimentalDesignController.updateFactorValueCharacteristics = function(p0, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
			'updateFactorValueCharacteristics', p0, callback);
}
// ====================================================================================
if (ExpressionDataFileUploadController == null)
	var ExpressionDataFileUploadController = {};
ExpressionDataFileUploadController._path = '/Gemma/dwr';
ExpressionDataFileUploadController.load = function(p0, callback) {
	dwr.engine._execute(ExpressionDataFileUploadController._path, 'ExpressionDataFileUploadController', 'load', p0,
			callback);
}
ExpressionDataFileUploadController.validate = function(p0, callback) {
	dwr.engine._execute(ExpressionDataFileUploadController._path, 'ExpressionDataFileUploadController', 'validate', p0,
			callback);
}
// ====================================================================================
if (ExpressionExperimentController == null)
	var ExpressionExperimentController = {};
ExpressionExperimentController._path = '/Gemma/dwr';
ExpressionExperimentController.getAnnotation = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getAnnotation', p0,
			callback);
}
ExpressionExperimentController.find = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'find', p0, p1,
			callback);
}
ExpressionExperimentController.searchExpressionExperiments = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'searchExpressionExperiments', p0, callback);
}
ExpressionExperimentController.getAllTaxonExperimentGroup = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getAllTaxonExperimentGroup', p0, callback);
}
ExpressionExperimentController.searchExperimentsAndExperimentGroups = function(p0,p1, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'searchExperimentsAndExperimentGroups', p0, p1, callback);
}
ExpressionExperimentController.searchExperimentsAndExperimentGroupsGetIds = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'searchExperimentsAndExperimentGroupsGetIds', p0, p1, callback);
}
ExpressionExperimentController.getDescription = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getDescription', p0,
			callback);
}
ExpressionExperimentController.getFactorValues = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getFactorValues', p0,
			callback);
}
ExpressionExperimentController.getExperimentalFactors = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'getExperimentalFactors', p0, callback);
}
ExpressionExperimentController.updateReport = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateReport', p0,
			callback);
}
ExpressionExperimentController.updatePubMed = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updatePubMed', p0, p1,
			callback);
}
ExpressionExperimentController.deleteById = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'deleteById', p0,
			callback);
}
ExpressionExperimentController.getDesignMatrixRows = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getDesignMatrixRows',
			p0, callback);
};
ExpressionExperimentController.loadExpressionExperimentDetails = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'loadExpressionExperimentDetails', p0, callback);
};
ExpressionExperimentController.loadQuantitationTypes = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'loadQuantitationTypes', p0, callback);
};
ExpressionExperimentController.loadExpressionExperiments = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'loadExpressionExperiments', p0, callback);
};
ExpressionExperimentController.loadStatusSummaries = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadStatusSummaries',
			p0, p1, p2, p3, p4, callback);
};
ExpressionExperimentController.removePrimaryPublication = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'removePrimaryPublication', p0, callback);
};
ExpressionExperimentController.updateAllReports = function(callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateAllReports',
			callback);
};
ExpressionExperimentController.updateBasics = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateBasics', p0,
			callback);
};
ExpressionExperimentController.clearFromCaches = function(p0, callback ) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
            'clearFromCaches', p0, callback);
};
// ExpressionExperimentController.updateBioMaterialMapping = function(callback) {
// dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
// 'updateBioMaterialMapping', callback);
// }
ExpressionExperimentController.unmatchAllBioAssays = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'unmatchAllBioAssays',
			p0, callback);
};
ExpressionExperimentController.canCurrentUserEditExperiment = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'canCurrentUserEditExperiment',
			p0, callback);
};
ExpressionExperimentController.browse = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browse', p0,
			callback);
};
ExpressionExperimentController.browseSpecificIds = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browseSpecificIds', p0, p1,
			callback);
};
ExpressionExperimentController.browseByTaxon = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browseByTaxon', p0, p1,
			callback);
};
ExpressionExperimentController.loadCountsForDataSummaryTable = function(callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadCountsForDataSummaryTable', 
			callback);
}
ExpressionExperimentController.loadExpressionExperimentsWithQcIssues = function(callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadExpressionExperimentsWithQcIssues', 
			callback);
};

// ====================================================================================
if (ExpressionExperimentDataFetchController == null)
	var ExpressionExperimentDataFetchController = {};
ExpressionExperimentDataFetchController._path = '/Gemma/dwr';
ExpressionExperimentDataFetchController.getDataFile = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
			'getDataFile', p0, callback);
};
ExpressionExperimentDataFetchController.getDiffExpressionDataFile = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
			'getDiffExpressionDataFile', p0, callback);
};
ExpressionExperimentDataFetchController.getCoExpressionDataFile = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
			'getCoExpressionDataFile', p0, callback);
}
// ====================================================================================
if (ExpressionExperimentLoadController == null)
	var ExpressionExperimentLoadController = {};
ExpressionExperimentLoadController._path = '/Gemma/dwr';
ExpressionExperimentLoadController.load = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentLoadController._path, 'ExpressionExperimentLoadController', 'load', p0,
			callback);
}

// ====================================================================================
if (ExpressionExperimentSetController == null)
	var ExpressionExperimentSetController = {};
ExpressionExperimentSetController._path = '/Gemma/dwr';
ExpressionExperimentSetController.remove = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'remove', p0,
			callback);
}
ExpressionExperimentSetController.create = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'create', p0,
			callback);
}
ExpressionExperimentSetController.update = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'update', p0,
			callback);
}
ExpressionExperimentSetController.updateNameDesc = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateNameDesc', p0,
			callback);
}
ExpressionExperimentSetController.updateMembers = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateMembers', p0, p1,
			callback);
}
ExpressionExperimentSetController.loadAll = function(callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadAll',
			callback);
}
ExpressionExperimentSetController.load = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'load', p0,
			callback);
}
ExpressionExperimentSetController.loadByName = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadByName', p0,
			callback);
}
ExpressionExperimentSetController.removeUserAndSessionGroups = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'removeUserAndSessionGroups', p0,
			callback);
}
ExpressionExperimentSetController.addUserAndSessionGroups = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'addUserAndSessionGroups', p0,
			callback);
}
ExpressionExperimentSetController.addSessionGroups = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'addSessionGroups', p0, p1,
			callback);
}
ExpressionExperimentSetController.updateUserAndSessionGroups = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateUserAndSessionGroups', p0,
			callback);
}
ExpressionExperimentSetController.loadAllUserAndSessionGroups = function(callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadAllUserAndSessionGroups',
			callback);
}
ExpressionExperimentSetController.loadAllSessionGroups = function(callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadAllSessionGroups',
			callback);
}
ExpressionExperimentSetController.getExperimentsInSet = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
			'getExperimentsInSet', p0, callback);
}
ExpressionExperimentSetController.getExperimentsInSetBySessionId = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
			'getExperimentsInSetBySessionId', p0, callback);
}
ExpressionExperimentSetController.getExperimentIdsInSet = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
			'getExperimentIdsInSet', p0, callback);
}
ExpressionExperimentSetController.canCurrentUserEditGroup = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
			'canCurrentUserEditGroup', p0, callback);
}

// ====================================================================================

if (ExpressionExperimentReportGenerationController == null)
	var ExpressionExperimentReportGenerationController = {};
ExpressionExperimentReportGenerationController._path = '/Gemma/dwr';

ExpressionExperimentReportGenerationController.run = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentReportGenerationController._path,
			'ExpressionExperimentReportGenerationController', 'run', p0, callback);
}
ExpressionExperimentReportGenerationController.runAll = function(callback) {
	dwr.engine._execute(ExpressionExperimentReportGenerationController._path,
			'ExpressionExperimentReportGenerationController', 'runAll', callback);
}
// ====================================================================================
if (ExtCoexpressionSearchController == null)
	var ExtCoexpressionSearchController = {};
ExtCoexpressionSearchController._path = '/Gemma/dwr';
ExtCoexpressionSearchController.doSearch = function(p0, callback) {
	dwr.engine._execute(ExtCoexpressionSearchController._path, 'ExtCoexpressionSearchController', 'doSearch', p0,
			callback);
}
ExtCoexpressionSearchController.findExpressionExperiments = function(p0, p1, callback) {
	dwr.engine._execute(ExtCoexpressionSearchController._path, 'ExtCoexpressionSearchController',
			'findExpressionExperiments', p0, p1, callback);
}
ExtCoexpressionSearchController.doSearchQuick2Complete = function(p0, p1, callback) {
	
	dwr.engine._execute(ExtCoexpressionSearchController._path, 'ExtCoexpressionSearchController', 'doSearchQuick2', p0, p1,
			callback);
	
}
ExtCoexpressionSearchController.doSearchQuick2 = function(p0, callback) {
	
	dwr.engine._execute(ExtCoexpressionSearchController._path, 'ExtCoexpressionSearchController', 'doSearchQuick2', p0,
			callback);
	
}
ExtCoexpressionSearchController.doSearchQuick = function(p0, callback) {
	dwr.engine._execute(ExtCoexpressionSearchController._path, 'ExtCoexpressionSearchController', 'doSearchQuick', p0,
			callback);
}

ExtCoexpressionSearchController.doBackgroundCoexSearch = function(p0, callback) {
	dwr.engine._execute(ExtCoexpressionSearchController._path, 'ExtCoexpressionSearchController', 'doBackgroundCoexSearch', p0,
			callback);
}
// ====================================================================================
if (FileUploadController == null)
	var FileUploadController = {};
FileUploadController._path = '/Gemma/dwr';
FileUploadController.upload = function(p0, callback) {
	dwr.engine._execute(FileUploadController._path, 'FileUploadController', 'upload', p0, callback);
}
FileUploadController.getUploadStatus = function(callback) {
	dwr.engine._execute(FileUploadController._path, 'FileUploadController', 'getUploadStatus', callback);
}
// ====================================================================================
if (GeneController == null)
	var GeneController = {};
GeneController._path = '/Gemma/dwr';
GeneController.getProducts = function(p0, callback) {
	dwr.engine._execute(GeneController._path, 'GeneController', 'getProducts', p0, callback);
}
GeneController.findGOTerms = function(p0, callback) {
	dwr.engine._execute(GeneController._path, 'GeneController', 'findGOTerms', p0, callback);
}
GeneController.loadGeneDetails = function(p0, callback) {
	dwr.engine._execute(GeneController._path, 'GeneController', 'loadGeneDetails', p0, callback);
}
GeneController.loadGeneEvidence = function(p0, p1, p2, p3, callback) {
	dwr.engine._execute(GeneController._path, 'GeneController', 'loadGeneEvidence', p0, p1, p2, p3, callback);
}
GeneController.loadAllenBrainImages = function(p0, callback) {
	dwr.engine._execute(GeneController._path, 'GeneController', 'loadAllenBrainImages', p0, callback);
}
// ====================================================================================
if (GenePickerController == null)
	var GenePickerController = {};
GenePickerController._path = '/Gemma/dwr';
GenePickerController.getGenes = function(p0, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGenes', p0, callback);
}
GenePickerController.getGenesByGOId = function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGenesByGOId', p0, p1, callback);
}
GenePickerController.searchGenes = function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenes', p0, p1, callback);
}
GenePickerController.searchGenesAndGeneGroups= function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesAndGeneGroups', p0, p1, callback);
}
GenePickerController.searchGenesAndGeneGroupsGetIds= function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesAndGeneGroupsGetIds', p0, p1, callback);
}
GenePickerController.searchGenesWithNCBIId= function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesWithNCBIId', p0, p1, callback);
}
GenePickerController.getTaxa = function(callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxa', callback);
}
GenePickerController.getTaxaSpecies = function(callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaSpecies', callback);
}
GenePickerController.getTaxaWithGenes = function(callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithGenes', callback);
}
GenePickerController.getTaxaWithDatasets = function(callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithDatasets', callback);
}
GenePickerController.getTaxaWithArrays = function(callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithArrays', callback);
}
GenePickerController.getTaxaWithEvidence = function(callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithEvidence', callback);
}
GenePickerController.searchMultipleGenes = function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchMultipleGenes', p0, p1, callback);
}
GenePickerController.searchMultipleGenesGetMap = function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchMultipleGenesGetMap', p0, p1, callback);
}
GenePickerController.getGeneSetByGOId = function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGeneSetByGOId', p0, p1, callback);
}
// ====================================================================================
if (GeoRecordBrowserController == null)
	var GeoRecordBrowserController = {};
GeoRecordBrowserController._path = '/Gemma/dwr';
GeoRecordBrowserController.browse = function(p0, p1, callback) {
	dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'browse', p0, p1, callback);
}
GeoRecordBrowserController.getDetails = function(p0, callback) {
	dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'getDetails', p0, callback);
}
GeoRecordBrowserController.toggleUsability = function(p0, callback) {
	dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'toggleUsability', p0, callback);
}
// ====================================================================================
if (SecurityController == null)
	var SecurityController = {};
SecurityController._path = '/Gemma/dwr';
SecurityController.createGroup = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'createGroup', p0, callback);
}
SecurityController.deleteGroup = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'deleteGroup', p0, callback);
}
SecurityController.getAvailableGroups = function(callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailableGroups', callback);
}
SecurityController.getAvailableSids = function(callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailableSids', callback);
}
SecurityController.getUsersData = function(p0, p1, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getUsersData', p0, p1, callback);
}
SecurityController.getSecurityInfo = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getSecurityInfo', p0, callback);
}
SecurityController.addUserToGroup = function(p0, p1, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'addUserToGroup', p0, p1, callback);
}
SecurityController.removeUsersFromGroup = function(p0, p1, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeUsersFromGroup', p0, p1, callback);
}
SecurityController.makeGroupReadable = function(p0, p1, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'makeGroupReadable', p0, p1, callback);
}
SecurityController.makeGroupWriteable = function(p0, p1, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'makeGroupWriteable', p0, p1, callback);
}
SecurityController.makePrivate = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'makePrivate', p0, callback);
}
SecurityController.makePublic = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'makePublic', p0, callback);
}
SecurityController.removeGroupWriteable = function(p0, p1, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeGroupWriteable', p0, p1, callback);
}
SecurityController.removeGroupReadable = function(p0, p1, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeGroupReadable', p0, p1, callback);
}
SecurityController.updatePermissions = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'updatePermissions', p0, callback);
}
SecurityController.updatePermission = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'updatePermission', p0, callback);
}
SecurityController.getGroupMembers = function(p0, callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getGroupMembers', p0, callback);
}
SecurityController.getAvailablePrincipalSids = function(callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailablePrincipalSids', callback);
}
SecurityController.getAuthenticatedUserNames = function(callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAuthenticatedUserNames', callback);
}
SecurityController.getAuthenticatedUserCount = function(callback) {
	dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAuthenticatedUserCount', callback);
}
// ==============================================================================
if (GeneSetController == null)
	var GeneSetController = {};
GeneSetController._path = '/Gemma/dwr';
GeneSetController.getGenesInGroup = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getGenesInGroup', p0, callback);
}
GeneSetController.load = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'load', p0, callback);
}
GeneSetController.getGeneSetsByGOId = function(p0, p1, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getGeneSetsByGO', p0, p1, callback);
}
GeneSetController.update = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'update', p0, callback);
}
GeneSetController.updateNameDesc = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateNameDesc', p0, callback);
}
GeneSetController.updateMembers = function(p0, p1, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateMembers', p0, p1, callback);
}
GeneSetController.updateSessionGroups = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateSessionGroups', p0, callback);
}
GeneSetController.updateUserAndSessionGroups = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateUserAndSessionGroups', p0, callback);
}
GeneSetController.create = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'create', p0, callback);
}
GeneSetController.addSessionGroups = function(p0, p1, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addSessionGroups', p0, p1, callback);
}
GeneSetController.addUserAndSessionGroups = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addUserAndSessionGroups', p0, callback);
}
GeneSetController.remove = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'remove', p0, callback);
}
GeneSetController.removeSessionGroups = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'removeSessionGroups', p0, callback);
}
GeneSetController.removeUserAndSessionGroups = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'removeUserAndSessionGroups', p0, callback);
}
GeneSetController.getUsersGeneGroups = function(p0, p1, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUsersGeneGroups', p0, p1, callback);
}
GeneSetController.getUserSessionGeneGroups = function(p0, p1, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUserSessionGeneGroups', p0, p1, callback);
}
GeneSetController.getUserAndSessionGeneGroups = function(p0, p1, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUserAndSessionGeneGroups', p0, p1, callback);
}
GeneSetController.findGeneSetsByGene = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'findGeneSetsByGene', p0, callback);
}
GeneSetController.findGeneSetsByName = function(p0, p1, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'findGeneSetsByName', p0, p1, callback);
}
GeneSetController.canCurrentUserEditGroup = function(p0, callback) {
	dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'canCurrentUserEditGroup', p0, callback);
}
// ====================================================================================
if (SystemMonitorController == null)
	var SystemMonitorController = {};
SystemMonitorController._path = '/Gemma/dwr';
SystemMonitorController.getHibernateStatus = function(callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'getHibernateStatus', callback);
}
SystemMonitorController.getSpaceStatus = function(callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'getSpaceStatus', callback);
}
SystemMonitorController.getCacheStatus = function(callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'getCacheStatus', callback);
}
SystemMonitorController.clearCache = function(p0, callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'clearCache', p0, callback);
}
SystemMonitorController.clearAllCaches = function(callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'clearAllCaches', callback);
}
SystemMonitorController.enableStatistics = function(callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'enableStatistics', callback);
}
SystemMonitorController.disableStatistics = function(callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'disableStatistics', callback);
}
SystemMonitorController.resetHibernateStatus = function(callback) {
	dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'resetHibernateStatus', callback);
}
// ====================================================================================
if (LinkAnalysisController == null)
	var LinkAnalysisController = {};
LinkAnalysisController._path = '/Gemma/dwr';
LinkAnalysisController.run = function(p0, callback) {
	dwr.engine._execute(LinkAnalysisController._path, 'LinkAnalysisController', 'run', p0, callback);
}

// ====================================================================================
if (MgedOntologyService == null)
	var MgedOntologyService = {};
MgedOntologyService._path = '/Gemma/dwr';
MgedOntologyService.getMgedTermsByKey = function(p0, callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'getMgedTermsByKey', p0, callback);
}
MgedOntologyService.getBioMaterialTreeNodeTerms = function(callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'getBioMaterialTreeNodeTerms', callback);
}
MgedOntologyService.getBioMaterialTerms = function(callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'getBioMaterialTerms', callback);
}
MgedOntologyService.getUsefulMgedTerms = function(callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'getUsefulMgedTerms', callback);
}
MgedOntologyService.loadNewOntology = function(p0, p1, callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'loadNewOntology', p0, p1, callback);
}
MgedOntologyService.getTerm = function(p0, callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'getTerm', p0, callback);
}
MgedOntologyService.getTermIndividuals = function(p0, callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'getTermIndividuals', p0, callback);
}
MgedOntologyService.isOntologyLoaded = function(callback) {
	dwr.engine._execute(MgedOntologyService._path, 'MgedOntologyService', 'isOntologyLoaded', callback);
}
// ====================================================================================
if (ProcessedExpressionDataVectorCreateController == null)
	var ProcessedExpressionDataVectorCreateController = {};
ProcessedExpressionDataVectorCreateController._path = '/Gemma/dwr';
ProcessedExpressionDataVectorCreateController.run = function(p0, callback) {
	dwr.engine._execute(ProcessedExpressionDataVectorCreateController._path,
			'ProcessedExpressionDataVectorCreateController', 'run', p0, callback);
}
// ====================================================================================
if (ProgressStatusService == null)
	var ProgressStatusService = {};
ProgressStatusService._path = '/Gemma/dwr';
ProgressStatusService.getProgressStatus = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getProgressStatus', p0, callback);
}
ProgressStatusService.cancelJob = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'cancelJob', p0, callback);
}
ProgressStatusService.addEmailAlert = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'addEmailAlert', p0, callback);
}
ProgressStatusService.getSubmittedTasks = function(callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getSubmittedTasks', callback);
}
ProgressStatusService.getFailedTasks = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getFailedTasks', callback);
}
ProgressStatusService.getCancelledTasks = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getCancelledTasks', callback);
}
ProgressStatusService.getFinishedTasks = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getFinishedTasks', callback);
}
// ====================================================================================

if (TwoChannelMissingValueController == null)
	var TwoChannelMissingValueController = {};
TwoChannelMissingValueController._path = '/Gemma/dwr';
TwoChannelMissingValueController.run = function(p0, callback) {
	dwr.engine
			._execute(TwoChannelMissingValueController._path, 'TwoChannelMissingValueController', 'run', p0, callback);
}
TwoChannelMissingValueController.run = function(p0, callback) {
	dwr.engine
			._execute(TwoChannelMissingValueController._path, 'TwoChannelMissingValueController', 'run', p0, callback);
}
// ====================================================================================
if (SvdController == null)
	var SvdController = {};
SvdController._path = '/Gemma/dwr';
SvdController.run = function(p0, callback) {
	dwr.engine
			._execute(SvdController._path, 'SvdController', 'run', p0, callback);
}

// ====================================================================================
if (SearchService == null)
	var SearchService = {};
SearchService._path = '/Gemma/dwr';
SearchService.search = function(p0, callback) {
	dwr.engine._execute(SearchService._path, 'SearchService', 'search', p0, callback);
}
// ====================================================================================
if (TaskCompletionController == null)
	var TaskCompletionController = {};
TaskCompletionController._path = '/Gemma/dwr';
TaskCompletionController.checkResult = function(p0, callback) {
	dwr.engine._execute(TaskCompletionController._path, 'TaskCompletionController', 'checkResult', p0, callback);
}
// ====================================================================================
if (TestTaskController == null)
	var TestTaskController = {};
TestTaskController._path = '/Gemma/dwr';
TestTaskController.run = function(p0, p1, p2, p3, callback) {
	dwr.engine._execute(TestTaskController._path, 'TestTaskController', 'run', p0, p1, p2, p3, callback);
}
// ====================================================================================
if (FeedReader == null)
	var FeedReader = {};
FeedReader._path = '/Gemma/dwr';
FeedReader.getLatestNews = function(callback) {
	dwr.engine._execute(FeedReader._path, 'FeedReader', 'getLatestNews', callback);
}
// ====================================================================================
if (UserListController == null)
	var UserListController = {};
UserListController._path = '/Gemma/dwr';
UserListController.getUsers = function(callback) {
	dwr.engine._execute(UserListController._path, 'UserListController', 'getUsers', callback);
}
UserListController.saveUser = function(p0, callback) {
	dwr.engine._execute(UserListController._path, 'UserListController', 'saveUser', p0, callback);
}
// =====================================================================================
if (LinkOutController == null)
	var LinkOutController = {};
LinkOutController._path = '/Gemma/dwr';
LinkOutController.getAllenBrainAtlasLink = function(p0, callback) {
	dwr.engine._execute(LinkOutController._path, 'LinkOutController', 'getAllenBrainAtlasLink', p0, callback);
}
//=====================================================================================
if (PhenotypeController == null)
	var PhenotypeController = {};
PhenotypeController._path = '/Gemma/dwr';
PhenotypeController.findEvidenceByFilters = function(p0, p1, p2, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findEvidenceByFilters', p0, p1, p2, callback);
}
PhenotypeController.findCandidateGenes = function(p0, p1, p2, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findCandidateGenes', p0, p1, p2, callback);
}
PhenotypeController.findExperimentMgedCategory = function(callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findExperimentMgedCategory', callback);
}
PhenotypeController.calculateExternalDatabasesStatistics = function(callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'calculateExternalDatabasesStatistics', callback);
}
PhenotypeController.findExperimentOntologyValue = function(p0, p1, p2, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findExperimentOntologyValue', p0, p1, p2, callback);
}
PhenotypeController.findEvidenceOwners = function(callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findEvidenceOwners', callback);
}
PhenotypeController.loadAllPhenotypesByTree = function(p0, p1, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'loadAllPhenotypesByTree', p0, p1, callback);
}
PhenotypeController.searchOntologyForPhenotypes = function(p0, p1, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'searchOntologyForPhenotypes', p0, p1, callback);
}
PhenotypeController.findBibliographicReference = function(p0, p1, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findBibliographicReference', p0, p1, callback);
}
PhenotypeController.processPhenotypeAssociationForm = function(p0, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'processPhenotypeAssociationForm', p0, callback);
}
PhenotypeController.validatePhenotypeAssociationForm = function(p0, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'validatePhenotypeAssociationForm', p0, callback);
}
PhenotypeController.removePhenotypeAssociation = function(p0, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'removePhenotypeAssociation', p0, callback);
}
PhenotypeController.makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis = function(p0, p1, p2, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis', p0, p1, p2, callback);
}
PhenotypeController.removeAllEvidenceFromMetaAnalysis = function(p0, callback) {
	dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'removeAllEvidenceFromMetaAnalysis', p0, callback);
}
//=====================================================================================
if (JavascriptLogger == null)
	var JavascriptLogger = {};
JavascriptLogger._path = '/Gemma/dwr';
JavascriptLogger.writeToLog = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToDebugLog = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToDebugLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToInfoLog = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToInfoLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToWarnLog = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToWarnLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToErrorLog = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToErrorLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToFatalLog = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToFatalLog', p0, p1, p2, p3, p4, callback);
}
//====================================================================================
if (SignupController == null)
	var SignupController = {};
SignupController._path = '/Gemma/dwr';
SignupController.loginCheck = function(callback) {
	dwr.engine._execute(SignupController._path, 'SignupController', 'loginCheck', callback);
}

// ====================================================================================
