/**
 * dwrServices.js
 *
 * Manual bundling of DWR interfaces into one file. This is necessary because DWR 2 does not support any bundling; and
 * JAWR does not currently support DWR+Spring.
 *
 * See analytics.jsp for definition of googleAnalyticsTrackPageviewIfConfigured().
 *
 */
if (typeof dwr === 'undefined') {
    var dwr = {};
}
if (typeof dwr.engine === 'undefined') {
    dwr.engine = {};
}
// ====================================================================================
if (typeof AnnotationController === 'undefined') {
    var AnnotationController = {};
}
AnnotationController._path = ctxBasePath + '/dwr';
AnnotationController.findTerm = function (p0, p1, callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'findTerm', p0, p1, callback);
};
AnnotationController.createExperimentTag = function (p0, p1, callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'createExperimentTag', p0, p1, callback);
};
AnnotationController.removeExperimentTag = function (p0, p1, callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'removeExperimentTag', p0, p1, callback);
};
AnnotationController.createBioMaterialTag = function (p0, p1, callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'createBioMaterialTag', p0, p1, callback);
};
AnnotationController.getCategoryTerms = function (callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'getCategoryTerms', callback);
};
AnnotationController.removeBioMaterialTag = function (p0, p1, callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'removeBioMaterialTag', p0, p1, callback);
};
AnnotationController.reinitializeOntologyIndices = function (callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'reinitializeOntologyIndices', callback);
};
AnnotationController.validateTags = function (p0, callback) {
    dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'validateTags', p0, callback);
};

// ====================================================================================
if (typeof ArrayDesignController === 'undefined') {
    var ArrayDesignController = {};
}
ArrayDesignController._path = ctxBasePath + '/dwr';
ArrayDesignController.remove = function (p0, callback) {
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'remove', p0, callback);
};
ArrayDesignController.getDetails = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ArrayDesignController/getDetails");
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getDetails', p0, callback);
};
ArrayDesignController.getArrayDesigns = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ArrayDesignController/getArrayDesigns");
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getArrayDesigns', p0, p1, p2, callback);
};
ArrayDesignController.loadArrayDesignsForShowAll = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ArrayDesignController/loadArrayDesignsForShowAll");
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'loadArrayDesignsForShowAll', p0,
        callback);
};
ArrayDesignController.addAlternateName = function (p0, p1, callback) {
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'addAlternateName', p0, p1, callback);
};
ArrayDesignController.getCsSummaries = function (p0, callback) {
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getCsSummaries', p0, callback);
};
ArrayDesignController.getReportHtml = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ArrayDesignController/getReportHtml");
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getReportHtml', p0, callback);
};
ArrayDesignController.updateReport = function (p0, callback) {
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'updateReport', p0, callback);
};
ArrayDesignController.getSummaryForArrayDesign = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ArrayDesignController/getSummaryForArrayDesign");
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getSummaryForArrayDesign', p0, callback);
};
ArrayDesignController.loadArrayDesignsSummary = function (callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ArrayDesignController/loadArrayDesignsSummary");
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'loadArrayDesignsSummary', callback);
};
ArrayDesignController.updateReportById = function (p0, callback) {
    dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'updateReportById', p0, callback);
};
// ====================================================================================
if (typeof ArrayDesignRepeatScanController === 'undefined') {
    var ArrayDesignRepeatScanController = {};
}
ArrayDesignRepeatScanController._path = ctxBasePath + '/dwr';
ArrayDesignRepeatScanController.run = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ArrayDesignRepeatScanController/run");
    dwr.engine._execute(ArrayDesignRepeatScanController._path, 'ArrayDesignRepeatScanController', 'run', p0, callback);
};
// ====================================================================================
if (typeof AuditController === 'undefined') {
    var AuditController = {};
}
AuditController._path = ctxBasePath + '/dwr';
AuditController.addAuditEvent = function (p0, p1, p2, p3, callback) {
    dwr.engine._execute(AuditController._path, 'AuditController', 'addAuditEvent', p0, p1, p2, p3, callback);
};
AuditController.getEvents = function (p0, callback) {
    dwr.engine._execute(AuditController._path, 'AuditController', 'getEvents', p0, callback);
};

// ====================================================================================
if (typeof BatchInfoFetchController === 'undefined') {
    var BatchInfoFetchController = {};
}
BatchInfoFetchController._path = ctxBasePath + '/dwr';
BatchInfoFetchController.run = function (p0, callback) {
    dwr.engine._execute(BatchInfoFetchController._path, 'BatchInfoFetchController', 'run', p0, callback);
};
// ====================================================================================
if (typeof BibliographicReferenceController === 'undefined') {
    var BibliographicReferenceController = {};
}
BibliographicReferenceController._path = ctxBasePath + '/dwr';
BibliographicReferenceController.update = function (p0, callback) {
    dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'update', p0,
        callback);
};
BibliographicReferenceController.browse = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/BibliographicReferenceController/browse");
    dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'browse', p0,
        callback);
};
BibliographicReferenceController.load = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/BibliographicReferenceController/load");
    dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'load', p0,
        callback);
};
BibliographicReferenceController.search = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/BibliographicReferenceController/search");
    dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'search', p0,
        callback);
};
BibliographicReferenceController.loadFromPubmedID = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/BibliographicReferenceController/loadFromPubmedID");
    dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'loadFromPubmedID',
        p0, callback);
};
// ====================================================================================
if (typeof BioAssayController === 'undefined') {
    var BioAssayController = {};
}
BioAssayController._path = ctxBasePath + '/dwr';
BioAssayController.markOutlier = function (p0, callback) {
    dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'markOutlier', p0, callback);
};
BioAssayController.unmarkOutlier = function (p0, callback) {
    dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'unmarkOutlier', p0, callback);
};
BioAssayController.getBioAssays = function (p0, callback) {
    dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'getBioAssays', p0, callback);
};
// BioAssayController.getIdentifiedOutliers = function( p0, callback ) {
// dwr.engine._execute( BioAssayController._path, 'BioAssayController', 'getIdentifiedOutliers', p0, callback );
// };

// ====================================================================================
if (typeof BioMaterialController === 'undefined') {
    var BioMaterialController = {};
}
BioMaterialController._path = ctxBasePath + '/dwr';
BioMaterialController.getAnnotation = function (p0, callback) {
    dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getAnnotation', p0, callback);
};
BioMaterialController.getFactorValues = function (p0, callback) {
    dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getFactorValues', p0, callback);
};
BioMaterialController.getBioMaterials = function (p0, callback) {
    dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getBioMaterials', p0, callback);
};
BioMaterialController.addFactorValueTo = function (p0, p1, callback) {
    dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'addFactorValueTo', p0, p1, callback);
};
// ====================================================================================
if (typeof CharacteristicBrowserController === 'undefined') {
    var CharacteristicBrowserController = {};
}
CharacteristicBrowserController._path = ctxBasePath + '/dwr';
CharacteristicBrowserController.findCharacteristics = function (p0, callback) {
    dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
        'findCharacteristics', p0, callback);
};
CharacteristicBrowserController.findCharacteristicsCustom = function (p0, p1, p2, p3, p4, p5, p6, p7, p8, callback) {
    dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
        'findCharacteristicsCustom', p0, p1, p2, p3, p4, p5, p6, p7, p8, callback);
};
CharacteristicBrowserController.removeCharacteristics = function (p0, callback) {
    dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
        'removeCharacteristics', p0, callback);
};
CharacteristicBrowserController.updateCharacteristics = function (p0, callback) {
    dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
        'updateCharacteristics', p0, callback);
};
CharacteristicBrowserController.browse = function (p0, callback) {
    dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'browse', p0,
        callback);
};
CharacteristicBrowserController.count = function (callback) {
    dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'count', callback);
};
// ====================================================================================
if (typeof CompositeSequenceController === 'undefined') {
    var CompositeSequenceController = {};
}
CompositeSequenceController._path = ctxBasePath + '/dwr';
CompositeSequenceController.search = function (p0, p1, callback) {
    dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'search', p0, p1, callback);
};
CompositeSequenceController.getCsSummaries = function (p0, callback) {
    dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getCsSummaries', p0,
        callback);
};
CompositeSequenceController.getGeneCsSummaries = function (p0, callback) {
    dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getGeneCsSummaries', p0,
        callback);
};
CompositeSequenceController.getGeneMappingSummary = function (p0, callback) {
    dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getGeneMappingSummary', p0,
        callback);
};
// ====================================================================================
if (typeof IndexService === 'undefined') {
    var IndexService = {};
}
IndexService._path = ctxBasePath + '/dwr';
IndexService.index = function (p0, callback) {
    dwr.engine._execute(IndexService._path, 'IndexService', 'index', p0, callback);
};
// ====================================================================================
if (typeof DEDVController === 'undefined') {
    var DEDVController = {};
}
DEDVController._path = ctxBasePath + '/dwr';
DEDVController.getDEDV = function (p0, p1, callback) {
    dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDV', p0, p1, callback);
};
DEDVController.getDEDVForCoexpressionVisualization = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DEDVController/getDEDVForCoexpressionVisualization");
    dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForCoexpressionVisualization', p0, p1, p2,
        callback);
};
DEDVController.getDEDVForDiffExVisualization = function (p0, p1, p2, p3, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DEDVController/getDEDVForDiffExVisualization");
    dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualization', p0, p1, p2, p3,
        callback);
};
DEDVController.getDEDVForVisualization = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DEDVController/getDEDVForVisualization");
    dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForVisualization', p0, p1, callback);
};
DEDVController.getDEDVForDiffExVisualizationByThreshold = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DEDVController/getDEDVForDiffExVisualizationByThreshold");
    dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualizationByThreshold', p0, p1,
        callback);
};
DEDVController.getDEDVForPcaVisualization = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DEDVController/getDEDVForPcaVisualization");
    dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForPcaVisualization', p0, p1, p2, callback);
};
DEDVController.getDEDVForDiffExVisualizationByExperiment = function (p0, p1, p2, p3, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DEDVController/getDEDVForDiffExVisualizationByExperiment");
    dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualizationByExperiment', p0, p1,
        p2, p3, callback);
};

// ====================================================================================
if (typeof DifferentialExpressionAnalysisController === 'undefined') {
    var DifferentialExpressionAnalysisController = {};
}
DifferentialExpressionAnalysisController._path = ctxBasePath + '/dwr';
DifferentialExpressionAnalysisController.run = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionAnalysisController/run");
    dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
        'run', p0, callback);
};
DifferentialExpressionAnalysisController.determineAnalysisType = function (p0, callback) {
    dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
        'determineAnalysisType', p0, callback);
};
DifferentialExpressionAnalysisController.runCustom = function (p0, p1, p2, p3, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionAnalysisController/runCutom");
    dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
        'runCustom', p0, p1, p2, p3, callback);
};
DifferentialExpressionAnalysisController.remove = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionAnalysisController/remove");
    dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
        'remove', p0, p1, callback);
};
DifferentialExpressionAnalysisController.redo = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionAnalysisController/redo");
    dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
        'redo', p0, p1, callback);
};
DifferentialExpressionAnalysisController.refreshStats = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionAnalysisController/refreshStats");
    dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
        'refreshStats', p0, p1, callback);
};
// ====================================================================================
if (typeof DifferentialExpressionSearchController === 'undefined') {
    var DifferentialExpressionSearchController = {};
}
DifferentialExpressionSearchController._path = ctxBasePath + '/dwr';
DifferentialExpressionSearchController.getDifferentialExpression = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionSearchController/getDifferentialExpression");
    dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
        'getDifferentialExpression', p0, p1, p2, callback);
};
DifferentialExpressionSearchController.getDifferentialExpressionWithoutBatch = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionSearchController/getDifferentialExpressionWithoutBatch");
    dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
        'getDifferentialExpressionWithoutBatch', p0, p1, p2, callback);
};
DifferentialExpressionSearchController.getFactors = function (p0, callback) {
    dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
        'getFactors', p0, callback);
};

DifferentialExpressionSearchController.scheduleDiffExpSearchTask = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DifferentialExpressionSearchController/scheduleDiffExpSearchTask");
    dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
        'scheduleDiffExpSearchTask', p0, p1, p2, callback);
};

// ====================================================================================
if (typeof DiffExMetaAnalyzerController === 'undefined') {
    var DiffExMetaAnalyzerController = {};
}
DiffExMetaAnalyzerController._path = ctxBasePath + '/dwr';
DiffExMetaAnalyzerController.analyzeResultSets = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DiffExMetaAnalyzerController/analyzeResultSets");
    dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'analyzeResultSets', p0,
        callback);
};
DiffExMetaAnalyzerController.findDetailMetaAnalysisById = function (p0, callback) {
    dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController',
        'findDetailMetaAnalysisById', p0, callback);
};
DiffExMetaAnalyzerController.loadAllMetaAnalyses = function (callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DiffExMetaAnalyzerController/loadAllMetaAnalyses");
    dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'loadAllMetaAnalyses',
        callback);
};
DiffExMetaAnalyzerController.removeMetaAnalysis = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DiffExMetaAnalyzerController/removeMetaAnalysis");
    dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'removeMetaAnalysis', p0,
        callback);
};
DiffExMetaAnalyzerController.saveResultSets = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/DiffExMetaAnalyzerController/saveResultSets");
    dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'saveResultSets', p0, p1,
        p2, callback);
};

// ====================================================================================
if (typeof ExperimentalDesignController === 'undefined') {
    var ExperimentalDesignController = {};
}
ExperimentalDesignController._path = ctxBasePath + '/dwr';
ExperimentalDesignController.updateBioMaterials = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/updateBioMaterials");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'updateBioMaterials', p0,
        callback);
};
ExperimentalDesignController.getFactorValues = function (p0, callback) {
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'getFactorValues', p0,
        callback);
};
ExperimentalDesignController.getExperimentalFactors = function (p0, callback) {
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'getExperimentalFactors',
        p0, callback);
};
ExperimentalDesignController.getBioMaterials = function (p0, callback) {
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'getBioMaterials', p0,
        callback);
};
ExperimentalDesignController.createDesignFromFile = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/createDesignFromFile");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'createDesignFromFile', p0,
        p1, callback);
};
ExperimentalDesignController.createExperimentalFactor = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/createExperimentalFactor");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'createExperimentalFactor',
        p0, p1, callback);
};
ExperimentalDesignController.createFactorValue = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/createFactorValue");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'createFactorValue', p0,
        callback);
};
ExperimentalDesignController.createFactorValueCharacteristic = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/createFactorValueCharacteristic");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
        'createFactorValueCharacteristic', p0, p1, callback);
};
ExperimentalDesignController.deleteExperimentalFactors = function (pO, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/deleteExperimentalFactors");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
        'deleteExperimentalFactors', pO, p1, callback);
};
ExperimentalDesignController.deleteFactorValueCharacteristics = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/deleteFactorValueCharacteristics");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
        'deleteFactorValueCharacteristics', p0, callback);
};
ExperimentalDesignController.deleteFactorValues = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/deleteFactorValues");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController', 'deleteFactorValues', p0,
        p1, callback);
};
ExperimentalDesignController.getFactorValuesWithCharacteristics = function (p0, callback) {
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
        'getFactorValuesWithCharacteristics', p0, callback);
};
ExperimentalDesignController.updateExperimentalFactors = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/updateExperimentalFactors");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
        'updateExperimentalFactors', p0, callback);
};
ExperimentalDesignController.updateFactorValueCharacteristics = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExperimentalDesignController/updateFactorValueCharacteristics");
    dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
        'updateFactorValueCharacteristics', p0, callback);
};
// ====================================================================================
if (typeof ExpressionDataFileUploadController === 'undefined') {
    var ExpressionDataFileUploadController = {};
}
ExpressionDataFileUploadController._path = ctxBasePath + '/dwr';
ExpressionDataFileUploadController.load = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionDataFileUploadController/load");
    dwr.engine._execute(ExpressionDataFileUploadController._path, 'ExpressionDataFileUploadController', 'load', p0,
        callback);
};
ExpressionDataFileUploadController.validate = function (p0, callback) {
    dwr.engine._execute(ExpressionDataFileUploadController._path, 'ExpressionDataFileUploadController', 'validate', p0,
        callback);
};
// ====================================================================================
if (typeof ExpressionExperimentController === 'undefined') {
    var ExpressionExperimentController = {};
}
ExpressionExperimentController._path = ctxBasePath + '/dwr';
ExpressionExperimentController.getAnnotation = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getAnnotation', p0,
        callback);
};
ExpressionExperimentController.find = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/find");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'find', p0, p1,
        callback);
};
ExpressionExperimentController.searchExpressionExperiments = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/searchExpressionExperiments");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'searchExpressionExperiments', p0, callback);
};
ExpressionExperimentController.getAllTaxonExperimentGroup = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'getAllTaxonExperimentGroup', p0, callback);
};
ExpressionExperimentController.searchExperimentsAndExperimentGroups = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/searchExperimentsAndExperimentGroups");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'searchExperimentsAndExperimentGroups', p0, p1, callback);
};
ExpressionExperimentController.getDescription = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getDescription', p0,
        callback);
};
ExpressionExperimentController.getFactorValues = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getFactorValues', p0,
        callback);
};
ExpressionExperimentController.getExperimentalFactors = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/getExperimentalFactors");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'getExperimentalFactors', p0, callback);
};
ExpressionExperimentController.updateReport = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/updateReport");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateReport', p0,
        callback);
};
ExpressionExperimentController.updatePubMed = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/updatePubMed");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updatePubMed', p0, p1,
        callback);
};
ExpressionExperimentController.deleteById = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/deleteById");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'deleteById', p0,
        callback);
};
ExpressionExperimentController.getDesignMatrixRows = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/getDesignMatrixRows");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getDesignMatrixRows',
        p0, callback);
};
ExpressionExperimentController.loadExpressionExperimentDetails = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/loadExpressionExperimentDetails?"
        + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'loadExpressionExperimentDetails', p0, callback);
};
ExpressionExperimentController.loadQuantitationTypes = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/loadQuantitationTypes?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'loadQuantitationTypes', p0, callback);
};
ExpressionExperimentController.loadExpressionExperiments = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/loadExpressionExperiments?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'loadExpressionExperiments', p0, callback);
};
ExpressionExperimentController.loadDetailedExpressionExperiments = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/loadDetailedExpressionExperiments?"
        + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'loadDetailedExpressionExperiments', p0, callback);
};

ExpressionExperimentController.loadExperimentsForPlatform = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/loadExperimentsForPlatform?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'loadExperimentsForPlatform', p0, callback);
};

ExpressionExperimentController.loadStatusSummaries = function (p0, p1, p2, p3, p4, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/loadStatusSummaries");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadStatusSummaries',
        p0, p1, p2, p3, p4, callback);
};
ExpressionExperimentController.removePrimaryPublication = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/removePrimaryPublication?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'removePrimaryPublication', p0, callback);
};
ExpressionExperimentController.updateAllReports = function (callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/updateAllReports");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateAllReports',
        callback);
};
ExpressionExperimentController.updateBasics = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/updateBasics");
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateBasics', p0,
        callback);
};
// ExpressionExperimentController.clearFromCaches = function(p0, callback ) {
// dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
// 'clearFromCaches', p0, callback);
// };
// ExpressionExperimentController.updateBioMaterialMapping = function(callback) {
// dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
// 'updateBioMaterialMapping', callback);
// }
ExpressionExperimentController.unmatchAllBioAssays = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/unmatchAllBioAssays?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'unmatchAllBioAssays',
        p0, callback);
};
ExpressionExperimentController.canCurrentUserEditExperiment = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'canCurrentUserEditExperiment', p0, callback);
};
ExpressionExperimentController.browse = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/browse?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browse', p0, callback);
};
ExpressionExperimentController.browseSpecificIds = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/browseSpecificIds?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browseSpecificIds',
        p0, p1, callback);
};
ExpressionExperimentController.browseByTaxon = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentController/browseByTaxon?" + p0);
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browseByTaxon', p0,
        p1, callback);
};
ExpressionExperimentController.loadCountsForDataSummaryTable = function (callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'loadCountsForDataSummaryTable', callback);
};
ExpressionExperimentController.loadExpressionExperimentsWithQcIssues = function (callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'loadExpressionExperimentsWithQcIssues', callback);
};
ExpressionExperimentController.recalculateBatchConfound = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'recalculateBatchConfound', p0, callback);
};
ExpressionExperimentController.recalculateBatchEffect = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'recalculateBatchEffect', p0, callback);
};
ExpressionExperimentController.runGeeq = function (p0, p1, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'runGeeq', p0, p1, callback);
};
ExpressionExperimentController.setGeeqManualSettings = function (p0, p1, callback) {
    dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
        'setGeeqManualSettings', p0, p1, callback);
};

// ====================================================================================
if (typeof ExpressionExperimentDataFetchController === 'undefined') {
    var ExpressionExperimentDataFetchController = {};
}
ExpressionExperimentDataFetchController._path = ctxBasePath + '/dwr';
ExpressionExperimentDataFetchController.getDataFile = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentDataFetchController/getDataFile?" + p0);
    dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
        'getDataFile', p0, callback);
};
ExpressionExperimentDataFetchController.getMetadataFiles = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
        'getMetadataFiles', p0, callback);
};
ExpressionExperimentDataFetchController.getDiffExpressionDataFile = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentDataFetchController/getDiffExpressionDataFile?"
        + p0);
    dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
        'getDiffExpressionDataFile', p0, callback);
};
ExpressionExperimentDataFetchController.getCoExpressionDataFile = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentDataFetchController/getCoExpressionDataFile?"
        + p0);
    dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
        'getCoExpressionDataFile', p0, callback);
};
// ====================================================================================
if (typeof ExpressionExperimentLoadController === 'undefined') {
    var ExpressionExperimentLoadController = {};
}
ExpressionExperimentLoadController._path = ctxBasePath + '/dwr';
ExpressionExperimentLoadController.load = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentLoadController._path, 'ExpressionExperimentLoadController', 'load', p0,
        callback);
};

// ====================================================================================
if (typeof ExpressionExperimentSetController === 'undefined') {
    var ExpressionExperimentSetController = {};
}
ExpressionExperimentSetController._path = ctxBasePath + '/dwr';
ExpressionExperimentSetController.remove = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/remove?" + p0);
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'remove', p0,
        callback);
};
ExpressionExperimentSetController.create = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/create?" + p0);
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'create', p0,
        callback);
};
ExpressionExperimentSetController.update = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/update?" + p0);
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'update', p0,
        callback);
};
ExpressionExperimentSetController.updateNameDesc = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/updateNameDesc?" + p0);
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateNameDesc',
        p0, callback);
};
ExpressionExperimentSetController.updateMembers = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/updateMembers?" + p0);
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateMembers',
        p0, p1, callback);
};
ExpressionExperimentSetController.loadAll = function (callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/loadAll");
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadAll',
        callback);
};
ExpressionExperimentSetController.load = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/load?" + p0);
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'load', p0,
        callback);
};
ExpressionExperimentSetController.loadByName = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/ExpressionExperimentSetController/loadByName?" + p0);
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadByName', p0,
        callback);
};
ExpressionExperimentSetController.removeUserAndSessionGroups = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'removeUserAndSessionGroups', p0, callback);
};
ExpressionExperimentSetController.addUserAndSessionGroups = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'addUserAndSessionGroups', p0, callback);
};
ExpressionExperimentSetController.addSessionGroups = function (p0, p1, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'addSessionGroups', p0, p1, callback);
};
ExpressionExperimentSetController.addSessionGroup = function (p0, p1, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'addSessionGroup', p0, p1, callback);
};
ExpressionExperimentSetController.updateUserAndSessionGroups = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'updateUserAndSessionGroups', p0, callback);
};
ExpressionExperimentSetController.updateSessionGroups = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'updateSessionGroups', p0, callback);
};
ExpressionExperimentSetController.loadAllUserAndSessionGroups = function (callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'loadAllUserAndSessionGroups', callback);
};
ExpressionExperimentSetController.loadAllSessionGroups = function (callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'loadAllSessionGroups', callback);
};
// ExpressionExperimentSetController.getExperimentsInSetBySessionId = function( p0, callback ) {
// dwr.engine._execute( ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
// 'getExperimentsInSetBySessionId', p0, callback );
// };
ExpressionExperimentSetController.getExperimentIdsInSet = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'getExperimentIdsInSet', p0, callback);
};
ExpressionExperimentSetController.getExperimentsInSet = function (p0, p1, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'getExperimentsInSet', p0, p1, callback);
};
ExpressionExperimentSetController.canCurrentUserEditGroup = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
        'canCurrentUserEditGroup', p0, callback);
};

// ====================================================================================

if (typeof ExpressionExperimentReportGenerationController === 'undefined') {
    var ExpressionExperimentReportGenerationController = {};
}
ExpressionExperimentReportGenerationController._path = ctxBasePath + '/dwr';

ExpressionExperimentReportGenerationController.run = function (p0, callback) {
    dwr.engine._execute(ExpressionExperimentReportGenerationController._path,
        'ExpressionExperimentReportGenerationController', 'run', p0, callback);
};
ExpressionExperimentReportGenerationController.runAll = function (callback) {
    dwr.engine._execute(ExpressionExperimentReportGenerationController._path,
        'ExpressionExperimentReportGenerationController', 'runAll', callback);
};
// ====================================================================================
if (typeof CoexpressionSearchController === 'undefined') {
    var CoexpressionSearchController = {};
}
CoexpressionSearchController._path = ctxBasePath + '/dwr';

CoexpressionSearchController.doSearch = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/CoexpressionSearchController/doSearch");
    dwr.engine._execute(CoexpressionSearchController._path, 'CoexpressionSearchController', 'doSearch', p0, callback);
};
CoexpressionSearchController.doSearchQuickComplete = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/CoexpressionSearchController/doSearchQuickComplete");
    dwr.engine._execute(CoexpressionSearchController._path, 'CoexpressionSearchController', 'doSearchQuickComplete',
        p0, p1, callback);

};

CoexpressionSearchController.doBackgroundCoexSearch = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/CoexpressionSearchController/doBackgroundCoexSearch");
    dwr.engine._execute(CoexpressionSearchController._path, 'CoexpressionSearchController', 'doBackgroundCoexSearch',
        p0, callback);
};
// ====================================================================================
if (typeof FileUploadController === 'undefined') {
    var FileUploadController = {};
}
FileUploadController._path = ctxBasePath + '/dwr';
FileUploadController.upload = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/FileUploadController/upload");
    dwr.engine._execute(FileUploadController._path, 'FileUploadController', 'upload', p0, callback);
};
FileUploadController.getUploadStatus = function (callback) {
    dwr.engine._execute(FileUploadController._path, 'FileUploadController', 'getUploadStatus', callback);
};
// ====================================================================================
if (typeof GeneController === 'undefined') {
    var GeneController = {};
}
GeneController._path = ctxBasePath + '/dwr';
GeneController.getProducts = function (p0, callback) {
    // Not used?
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneController/getProducts");
    dwr.engine._execute(GeneController._path, 'GeneController', 'getProducts', p0, callback);
};
GeneController.findGOTerms = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneController/findGOTerms");
    dwr.engine._execute(GeneController._path, 'GeneController', 'findGOTerms', p0, callback);
};
GeneController.loadGeneDetails = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneController/loadGeneDetails");
    dwr.engine._execute(GeneController._path, 'GeneController', 'loadGeneDetails', p0, callback);
};
GeneController.loadGeneEvidence = function (p0, p1, p2, p3, p4, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneController/loadGeneEvidence");
    dwr.engine._execute(GeneController._path, 'GeneController', 'loadGeneEvidence', p0, p1, p2, p3, p4, callback);
};
GeneController.loadAllenBrainImages = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneController/loadAllenBrainImages");
    dwr.engine._execute(GeneController._path, 'GeneController', 'loadAllenBrainImages', p0, callback);
};
GeneController.getGeneABALink = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneController/getGeneABALink");
    dwr.engine._execute(GeneController._path, 'GeneController', 'getGeneABALink', p0, callback);
};
// ====================================================================================
if (typeof GenePickerController === 'undefined') {
    var GenePickerController = {};
}
GenePickerController._path = ctxBasePath + '/dwr';
GenePickerController.getGenes = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GenePickerController/getGenes");
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGenes', p0, callback);
};
GenePickerController.getGenesByGOId = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GenePickerController/getGenesByGOId");
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGenesByGOId', p0, p1, callback);
};
GenePickerController.searchGenes = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GenePickerController/searchGenes");
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenes', p0, p1, callback);
};
GenePickerController.searchGenesAndGeneGroups = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GenePickerController/searchGenesAndGeneGroups");
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesAndGeneGroups', p0, p1,
        callback);
};
GenePickerController.searchGenesAndGeneGroupsGetIds = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GenePickerController/searchGenesAndGeneGroupsGetIds");
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesAndGeneGroupsGetIds', p0, p1,
        callback);
};
GenePickerController.searchGenesWithNCBIId = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GenePickerController/searchGenesWithNCBIId");
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesWithNCBIId', p0, p1, callback);
};
GenePickerController.getTaxa = function (callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxa', callback);
}; 
GenePickerController.getTaxaWithGenes = function (callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithGenes', callback);
};
GenePickerController.getTaxaWithDatasets = function (callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithDatasets', callback);
};
GenePickerController.getTaxaWithArrays = function (callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithArrays', callback);
};
GenePickerController.getTaxaWithEvidence = function (callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithEvidence', callback);
};
GenePickerController.searchMultipleGenes = function (p0, p1, callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchMultipleGenes', p0, p1, callback);
};
/**
 * @param {Array}
 * @param {Number}
 * @param {Function}
 */
GenePickerController.searchMultipleGenesGetMap = function (p0, p1, callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchMultipleGenesGetMap', p0, p1,
        callback);
};
GenePickerController.getGeneSetByGOId = function (p0, p1, callback) {
    dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGeneSetByGOId', p0, p1, callback);
};
// ====================================================================================
if (typeof GeoRecordBrowserController === 'undefined') {
    var GeoRecordBrowserController = {};
}
GeoRecordBrowserController._path = ctxBasePath + '/dwr';
GeoRecordBrowserController.browse = function (p0, p1, p2, callback) {
    dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'browse', p0, p1, p2, callback);
};
GeoRecordBrowserController.getDetails = function (p0, callback) {
    dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'getDetails', p0, callback);
};
GeoRecordBrowserController.toggleUsability = function (p0, callback) {
    dwr.engine
        ._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'toggleUsability', p0, callback);
};
// ====================================================================================
if (typeof SecurityController === 'undefined') {
    var SecurityController = {};
}
SecurityController._path = ctxBasePath + '/dwr';
SecurityController.createGroup = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/createGroup");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'createGroup', p0, callback);
};
SecurityController.deleteGroup = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/deleteGroup");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'deleteGroup', p0, callback);
};
SecurityController.getAvailableGroups = function (callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailableGroups', callback);
};
SecurityController.getAvailableSids = function (callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailableSids', callback);
};
SecurityController.getUsersData = function (p0, p1, callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getUsersData', p0, p1, callback);
};
SecurityController.getSecurityInfo = function (p0, callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getSecurityInfo', p0, callback);
};
SecurityController.addUserToGroup = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/addUserToGroup");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'addUserToGroup', p0, p1, callback);
};
SecurityController.removeUsersFromGroup = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/removeUsersFromGroup");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeUsersFromGroup', p0, p1, callback);
};
SecurityController.makeGroupReadable = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/makeGroupReadable");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'makeGroupReadable', p0, p1, callback);
};
SecurityController.makeGroupWriteable = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/makeGroupWriteable");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'makeGroupWriteable', p0, p1, callback);
};
SecurityController.makePrivate = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/makePrivate");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'makePrivate', p0, callback);
};
SecurityController.makePublic = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/makePublic");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'makePublic', p0, callback);
};
SecurityController.removeGroupWriteable = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/removeGroupWriteable");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeGroupWriteable', p0, p1, callback);
};
SecurityController.removeGroupReadable = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/removeGroupReadable");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeGroupReadable', p0, p1, callback);
};
SecurityController.updatePermissions = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/updatePermissions");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'updatePermissions', p0, callback);
};
SecurityController.updatePermission = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SecurityController/updatePermission");
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'updatePermission', p0, callback);
};
SecurityController.getGroupMembers = function (p0, callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getGroupMembers', p0, callback);
};
SecurityController.getAvailablePrincipalSids = function (callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailablePrincipalSids', callback);
};
SecurityController.getAuthenticatedUserNames = function (callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAuthenticatedUserNames', callback);
};
SecurityController.getAuthenticatedUserCount = function (callback) {
    dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAuthenticatedUserCount', callback);
};
// ==============================================================================
if (typeof GeneSetController === 'undefined') {
    var GeneSetController = {};
}
GeneSetController._path = ctxBasePath + '/dwr';
GeneSetController.getGenesInGroup = function (p0, p1, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getGenesInGroup', p0, p1, callback);
};
GeneSetController.getGenesIdsInGroup = function (p0, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getGeneIdsInGroup', p0, callback);
};
GeneSetController.load = function (p0, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'load', p0, callback);
};
GeneSetController.getGeneSetsByGOId = function (p0, p1, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getGeneSetsByGO', p0, p1, callback);
};
GeneSetController.update = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/update");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'update', p0, callback);
};
GeneSetController.updateNameDesc = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/updateNameDesc");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateNameDesc', p0, callback);
};
GeneSetController.updateMembers = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/updateMembers");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateMembers', p0, p1, callback);
};
GeneSetController.updateSessionGroups = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/updateSessionGroups");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateSessionGroups', p0, callback);
};
GeneSetController.updateSessionGroup = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/updateSessionGroup");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateSessionGroup', p0, callback);
};
GeneSetController.updateUserAndSessionGroups = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/updateUserAndSessionGroups");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateUserAndSessionGroups', p0, callback);
};
GeneSetController.create = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/create");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'create', p0, callback);
};
GeneSetController.addSessionGroups = function (p0, p1, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addSessionGroups', p0, p1, callback);
};
GeneSetController.addSessionGroup = function (p0, p1, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addSessionGroup', p0, p1, callback);
};
GeneSetController.addUserAndSessionGroups = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/addUserAndSessionGroups");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addUserAndSessionGroups', p0, callback);
};
GeneSetController.remove = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/remove");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'remove', p0, callback);
};
GeneSetController.removeSessionGroups = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/removeSessionGroups");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'removeSessionGroups', p0, callback);
};
GeneSetController.removeUserAndSessionGroups = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/removeUserAndSessionGroups");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'removeUserAndSessionGroups', p0, callback);
};
GeneSetController.getUsersGeneGroups = function (p0, p1, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUsersGeneGroups', p0, p1, callback);
};
GeneSetController.getUserSessionGeneGroups = function (p0, p1, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUserSessionGeneGroups', p0, p1, callback);
};
GeneSetController.getUserAndSessionGeneGroups = function (p0, p1, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUserAndSessionGeneGroups', p0, p1, callback);
};
GeneSetController.findGeneSetsByGene = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/findGeneSetsByGene");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'findGeneSetsByGene', p0, callback);
};
GeneSetController.findGeneSetsByName = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/GeneSetController/findGeneSetsByName");
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'findGeneSetsByName', p0, p1, callback);
};
GeneSetController.canCurrentUserEditGroup = function (p0, callback) {
    dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'canCurrentUserEditGroup', p0, callback);
};
// ====================================================================================
if (typeof SystemMonitorController === 'undefined') {
    var SystemMonitorController = {};
}
SystemMonitorController._path = ctxBasePath + '/dwr';
SystemMonitorController.getHibernateStatus = function (callback) {
    dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'getHibernateStatus', callback);
};
SystemMonitorController.getCacheStatus = function (callback) {
    dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'getCacheStatus', callback);
};
SystemMonitorController.clearCache = function (p0, callback) {
    dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'clearCache', p0, callback);
};
SystemMonitorController.clearAllCaches = function (callback) {
    dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'clearAllCaches', callback);
};
SystemMonitorController.enableStatistics = function (callback) {
    dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'enableStatistics', callback);
};
SystemMonitorController.disableStatistics = function (callback) {
    dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'disableStatistics', callback);
};
SystemMonitorController.resetHibernateStatus = function (callback) {
    dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'resetHibernateStatus', callback);
};

// ====================================================================================
if (typeof LinkAnalysisController === 'undefined') {
    var LinkAnalysisController = {};
}
LinkAnalysisController._path = ctxBasePath + '/dwr';
LinkAnalysisController.run = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/LinkAnalysisController/run");
    dwr.engine._execute(LinkAnalysisController._path, 'LinkAnalysisController', 'run', p0, callback);
};

// ====================================================================================
if (typeof PreprocessController === 'undefined') {
    var PreprocessController = {};
}
PreprocessController._path = ctxBasePath + '/dwr';
PreprocessController.run = function (p0, callback) {
    dwr.engine._execute(PreprocessController._path,
        'PreprocessController', 'run', p0, callback);
};
PreprocessController.diagnostics = function (p0, callback) {
    dwr.engine._execute(PreprocessController._path,
        'PreprocessController', 'diagnostics', p0, callback);
};
// ====================================================================================
if (typeof ProgressStatusService === 'undefined') {
    var ProgressStatusService = {};
}
ProgressStatusService._path = ctxBasePath + '/dwr';
ProgressStatusService.getProgressStatus = function (p0, callback) {
    dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getProgressStatus', p0, callback);
};
ProgressStatusService.getSubmittedTask = function (p0, callback) {
    dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getSubmittedTask', p0, callback);
};
ProgressStatusService.cancelJob = function (p0, callback) {
    dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'cancelJob', p0, callback);
};
ProgressStatusService.addEmailAlert = function (p0, callback) {
    dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'addEmailAlert', p0, callback);
};
ProgressStatusService.getSubmittedTasks = function (callback) {
    dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getSubmittedTasks', callback);
};
// ====================================================================================

if (typeof TwoChannelMissingValueController === 'undefined') {
    var TwoChannelMissingValueController = {};
}
TwoChannelMissingValueController._path = ctxBasePath + '/dwr';
TwoChannelMissingValueController.run = function (p0, callback) {
    dwr.engine
        ._execute(TwoChannelMissingValueController._path, 'TwoChannelMissingValueController', 'run', p0, callback);
};
TwoChannelMissingValueController.run = function (p0, callback) {
    dwr.engine
        ._execute(TwoChannelMissingValueController._path, 'TwoChannelMissingValueController', 'run', p0, callback);
};
// ====================================================================================
if (typeof SvdController === 'undefined') {
    var SvdController = {};
}
SvdController._path = ctxBasePath + '/dwr';
SvdController.run = function (p0, callback) {
    dwr.engine._execute(SvdController._path, 'SvdController', 'run', p0, callback);
};

// ====================================================================================
if (typeof SearchService === 'undefined') {
    var SearchService = {};
}
SearchService._path = ctxBasePath + '/dwr';
SearchService.ajaxSearch = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/SearchService/search");
    dwr.engine._execute(SearchService._path, 'SearchService', 'ajaxSearch', p0, callback);
};
// ====================================================================================
if (typeof TaskCompletionController === 'undefined') {
    var TaskCompletionController = {};
}
TaskCompletionController._path = ctxBasePath + '/dwr';
TaskCompletionController.checkResult = function (p0, callback) {
    dwr.engine._execute(TaskCompletionController._path, 'TaskCompletionController', 'checkResult', p0, callback);
};
// ====================================================================================
if (typeof FeedReader === 'undefined') {
    var FeedReader = {};
}
FeedReader._path = ctxBasePath + '/dwr';
FeedReader.getLatestNews = function (callback) {
    dwr.engine._execute(FeedReader._path, 'FeedReader', 'getLatestNews', callback);
};
// ====================================================================================
if (typeof UserListController === 'undefined') {
    var UserListController = {};
}
UserListController._path = ctxBasePath + '/dwr';
UserListController.getUsers = function (callback) {
    dwr.engine._execute(UserListController._path, 'UserListController', 'getUsers', callback);
};
UserListController.saveUser = function (p0, callback) {
    dwr.engine._execute(UserListController._path, 'UserListController', 'saveUser', p0, callback);
};
// =====================================================================================
if (typeof LinkOutController === 'undefined') {
    var LinkOutController = {};
}
LinkOutController._path = ctxBasePath + '/dwr';
LinkOutController.getAllenBrainAtlasLink = function (p0, callback) {
    dwr.engine._execute(LinkOutController._path, 'LinkOutController', 'getAllenBrainAtlasLink', p0, callback);
};
// =====================================================================================
if (typeof PhenotypeController === 'undefined') {
    var PhenotypeController = {};
}
PhenotypeController._path = ctxBasePath + '/dwr';
PhenotypeController.findEvidenceByFilters = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/PhenotypeController/findEvidenceByFilters");
    dwr.engine
        ._execute(PhenotypeController._path, 'PhenotypeController', 'findEvidenceByFilters', p0, p1, p2, callback);
};
PhenotypeController.findCandidateGenes = function (p0, p1, p2, p3, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/PhenotypeController/findCandidateGenes");
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findCandidateGenes', p0, p1, p2, p3,
        callback);
};
PhenotypeController.findExperimentCategory = function (callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findExperimentCategory', callback);
};
PhenotypeController.calculateExternalDatabasesStatistics = function (callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/PhenotypeController/calculateExternalDatabasesStatistics");
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'calculateExternalDatabasesStatistics',
        callback);
};
PhenotypeController.findExperimentOntologyValue = function (p0, p1, p2, callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findExperimentOntologyValue', p0, p1, p2,
        callback);
};
PhenotypeController.findEvidenceOwners = function (callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findEvidenceOwners', callback);
};
PhenotypeController.loadAllPhenotypesByTree = function (p0, p1, p2, callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'loadAllPhenotypesByTree', p0, p1, p2,
        callback);
};
PhenotypeController.searchOntologyForPhenotypes = function (p0, p1, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/PhenotypeController/searchOntologyForPhenotypes");
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'searchOntologyForPhenotypes', p0, p1,
        callback);
};
PhenotypeController.findBibliographicReference = function (p0, p1, callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findBibliographicReference', p0, p1,
        callback);
};
PhenotypeController.processPhenotypeAssociationForm = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/PhenotypeController/processPhenotypeAssociationForm");
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'processPhenotypeAssociationForm', p0,
        callback);
};
PhenotypeController.validatePhenotypeAssociationForm = function (p0, callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'validatePhenotypeAssociationForm', p0,
        callback);
};
PhenotypeController.removePhenotypeAssociation = function (p0, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/PhenotypeController/removePhenotypeAssociation");
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'removePhenotypeAssociation', p0, callback);
};
PhenotypeController.makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis = function (p0, p1, p2, callback) {
    googleAnalyticsTrackPageviewIfConfigured("/Gemma/PhenotypeController/makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis");
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController',
        'makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis', p0, p1, p2, callback);
};
PhenotypeController.removeAllEvidenceFromMetaAnalysis = function (p0, callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'removeAllEvidenceFromMetaAnalysis', p0,
        callback);
};
PhenotypeController.findExternalDatabaseName = function (callback) {
    dwr.engine._execute(PhenotypeController._path, 'PhenotypeController', 'findExternalDatabaseName', callback);
};
// =====================================================================================
if (typeof JavascriptLogger === 'undefined') {
    var JavascriptLogger = {};
}
JavascriptLogger._path = ctxBasePath + '/dwr';
JavascriptLogger.writeToLog = function (p0, p1, p2, p3, p4, callback) {
    dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToLog', p0, p1, p2, p3, p4, callback);
};
JavascriptLogger.writeToDebugLog = function (p0, p1, p2, p3, p4, callback) {
    dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToDebugLog', p0, p1, p2, p3, p4, callback);
};
JavascriptLogger.writeToInfoLog = function (p0, p1, p2, p3, p4, callback) {
    dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToInfoLog', p0, p1, p2, p3, p4, callback);
};
JavascriptLogger.writeToWarnLog = function (p0, p1, p2, p3, p4, callback) {
    dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToWarnLog', p0, p1, p2, p3, p4, callback);
};
JavascriptLogger.writeToErrorLog = function (p0, p1, p2, p3, p4, callback) {
    dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToErrorLog', p0, p1, p2, p3, p4, callback);
};
JavascriptLogger.writeToFatalLog = function (p0, p1, p2, p3, p4, callback) {
    dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToFatalLog', p0, p1, p2, p3, p4, callback);
};
// ====================================================================================
if (typeof SignupController === 'undefined') {
    var SignupController = {};
}
SignupController._path = ctxBasePath + '/dwr';
SignupController.loginCheck = function (callback) {
    dwr.engine._execute(SignupController._path, 'SignupController', 'loginCheck', callback);
};

// ====================================================================================
