if (typeof AnnotationController === 'undefined') {
    var AnnotationController = {};
}

AnnotationController.autoTag = function(p0, callback) {
};
AnnotationController.findTerm = function(p0, p1, p2, callback) {
};
AnnotationController.createExperimentTag = function(p0, p1, callback) {
};
AnnotationController.removeExperimentTag = function(p0, p1, callback) {
};
AnnotationController.createBioMaterialTag = function(p0, p1, callback) {
};
AnnotationController.removeBioMaterialTag = function(p0, p1, callback) {
};
AnnotationController.reinitializeOntologyIndices = function(callback) {
};
AnnotationController.validateTags = function(p0, callback) {
};

if (typeof ArrayDesignController === 'undefined') {
    var ArrayDesignController = {};
}
ArrayDesignController.remove = function(p0, callback) {
};
ArrayDesignController.getArrayDesigns = function(p0, p1, p2, callback) {
};
ArrayDesignController.loadArrayDesignsForShowAll = function(p0, callback) {
};
ArrayDesignController.addAlternateName = function(p0, p1, callback) {
};
ArrayDesignController.getCsSummaries = function(p0, callback) {
};
ArrayDesignController.getReportHtml = function(p0, callback) {
};
ArrayDesignController.updateReport = function(p0, callback) {
};
ArrayDesignController.getSummaryForArrayDesign = function(p0, callback) {
};
ArrayDesignController.loadArrayDesignsSummary = function(callback) {
};
ArrayDesignController.updateReportById = function(p0, callback) {
};
if (typeof ArrayDesignRepeatScanController === 'undefined') {
    var ArrayDesignRepeatScanController = {};
}
ArrayDesignRepeatScanController.run = function(p0, callback) {
};

if (typeof AuditController === 'undefined') {
    var AuditController = {};
}
AuditController.addAuditEvent = function(p0, p1, p2, p3, callback) {
}; 
AuditController.getEvents = function(p0, callback) {
};

if (typeof BatchInfoFetchController === 'undefined') {
    var BatchInfoFetchController = {};
}
BatchInfoFetchController.run = function(p0, callback) {
};

if (typeof BibliographicReferenceController === 'undefined') {
    var BibliographicReferenceController = {};
}
BibliographicReferenceController.update = function(p0, callback) {
};
BibliographicReferenceController.browse = function(p0, callback) {
};
BibliographicReferenceController.load = function(p0, callback) {
};
BibliographicReferenceController.search = function(p0, callback) {
};
BibliographicReferenceController.loadFromPubmedID = function(p0, callback) {
};

if (typeof BioAssayController === 'undefined') {
    var BioAssayController = {};
}
BioAssayController.markOutlier = function(p0, callback) {
};
BioAssayController.unmarkOutlier = function(p0, callback) {
};
BioAssayController.getBioAssays = function(p0, callback) {
};

if (typeof BioMaterialController === 'undefined') {
    var BioMaterialController = {};
}
BioMaterialController.getAnnotation = function(p0, callback) {
};
BioMaterialController.getFactorValues = function(p0, callback) {
};
BioMaterialController.getBioMaterials = function(p0, callback) {
};
BioMaterialController.addFactorValueTo = function(p0, p1, callback) {
};

if (typeof CharacteristicBrowserController === 'undefined') {
    var CharacteristicBrowserController = {};
}
CharacteristicBrowserController.findCharacteristics = function(p0, callback) {
};
CharacteristicBrowserController.findCharacteristicsCustom = function(p0, p1, p2, p3, p4, p5, p6, callback) {
};
CharacteristicBrowserController.removeCharacteristics = function(p0, callback) {
};
CharacteristicBrowserController.updateCharacteristics = function(p0, callback) {
};
CharacteristicBrowserController.browse = function(p0, callback) {
};
CharacteristicBrowserController.count = function(callback) {
};

if (typeof CompositeSequenceController === 'undefined') {
    var CompositeSequenceController = {};
}
CompositeSequenceController.search = function(p0, p1, callback) {
};
CompositeSequenceController.getCsSummaries = function(p0, callback) {
};
CompositeSequenceController.getGeneMappingSummary = function(p0, callback) {
};

if (typeof IndexService === 'undefined') {
    var IndexService = {};
}
IndexService.index = function(p0, callback) {
};

if (typeof DEDVController === 'undefined') {
    var DEDVController = {};
}

DEDVController.getDEDV = function(p0, p1, callback) {
};
DEDVController.getDEDVForCoexpressionVisualization = function(p0, p1, p2, callback) {
};
DEDVController.getDEDVForDiffExVisualization = function(p0, p1, p2, p3, callback) {
};
DEDVController.getDEDVForVisualization = function(p0, p1, callback) {
};
DEDVController.getVectorData = function(p0, callback) {
};
DEDVController.getDEDVForDiffExVisualizationByThreshold = function(p0, p1,  callback) {
};
DEDVController.getDEDVForPcaVisualization = function(p0, p1, p2, callback) {
};
DEDVController.getDEDVForDiffExVisualizationByExperiment = function(p0, p1, p2, p3, callback) {
};

if (typeof DifferentialExpressionAnalysisController === 'undefined') {
    var DifferentialExpressionAnalysisController = {};
}
DifferentialExpressionAnalysisController.run = function(p0, callback) {
};
DifferentialExpressionAnalysisController.determineAnalysisType = function(p0, callback) {
};
DifferentialExpressionAnalysisController.runCustom = function(p0, p1, p2, p3, callback) {
};
DifferentialExpressionAnalysisController.remove = function(p0, p1, callback) {
};
DifferentialExpressionAnalysisController.redo = function(p0, p1, callback) {
};
DifferentialExpressionAnalysisController.refreshStats = function(p0, p1, callback) {
};

if (typeof DifferentialExpressionSearchController === 'undefined') {
    var DifferentialExpressionSearchController = {};
}
DifferentialExpressionSearchController.getDifferentialExpression = function(p0, p1, p2, callback) {
};
DifferentialExpressionSearchController.getDifferentialExpressionWithoutBatch = function(p0, p1, p2, callback) {
};
DifferentialExpressionSearchController.getDiffExpressionForGenes = function(p0, callback) {
};
DifferentialExpressionSearchController.getFactors = function(p0, callback) {
};
DifferentialExpressionSearchController.scheduleDiffExpSearchTask = function(p0, p1, p2, p3, p4, callback) {
};

if (typeof DiffExMetaAnalyzerController === 'undefined') {
    var DiffExMetaAnalyzerController = {};
}
DiffExMetaAnalyzerController.analyzeResultSets = function(p0, callback) {
};
DiffExMetaAnalyzerController.findDetailMetaAnalysisById = function(p0, callback) {
};
DiffExMetaAnalyzerController.loadAllMetaAnalyses = function(callback) {
};
DiffExMetaAnalyzerController.removeMetaAnalysis = function(p0, callback) {
};
DiffExMetaAnalyzerController.saveResultSets = function(p0, p1, p2, callback) {
};

if (typeof ExperimentalDesignController === 'undefined') {
    var ExperimentalDesignController = {};
}
ExperimentalDesignController.updateBioMaterials = function(p0, callback) {
};
ExperimentalDesignController.getFactorValues = function(p0, callback) {
};
ExperimentalDesignController.getExperimentalFactors = function(p0, callback) {
};
ExperimentalDesignController.getBioMaterials = function(p0, callback) {
};
ExperimentalDesignController.createDesignFromFile = function(p0, p1, callback) {
};
ExperimentalDesignController.createExperimentalFactor = function(p0, p1, callback) {
};
ExperimentalDesignController.createFactorValue = function(p0, callback) {
};
ExperimentalDesignController.createFactorValueCharacteristic = function(p0, p1, callback) {
};
ExperimentalDesignController.deleteExperimentalFactors = function(pO, p1, callback) {
};
ExperimentalDesignController.deleteFactorValueCharacteristics = function(p0, callback) {
};
ExperimentalDesignController.deleteFactorValues = function(p0, p1, callback) {
};
ExperimentalDesignController.getFactorValuesWithCharacteristics = function(p0, callback) {
};
ExperimentalDesignController.updateExperimentalFactors = function(p0, callback) {
};
ExperimentalDesignController.updateFactorValueCharacteristics = function(p0, callback) {
};

if (typeof ExpressionDataFileUploadController === 'undefined') {
    var ExpressionDataFileUploadController = {};
}
ExpressionDataFileUploadController.load = function(p0, callback) {
};
ExpressionDataFileUploadController.validate = function(p0, callback) {
};

if (typeof ExpressionExperimentController === 'undefined') {
    var ExpressionExperimentController = {};
}
ExpressionExperimentController.getAnnotation = function(p0, callback) {
};
ExpressionExperimentController.find = function(p0, p1, callback) {
};
ExpressionExperimentController.searchExpressionExperiments = function(p0, callback) {
};
ExpressionExperimentController.getAllTaxonExperimentGroup = function(p0, callback) {
};
ExpressionExperimentController.searchExperimentsAndExperimentGroups = function(p0,p1, callback) {
};
ExpressionExperimentController.searchExperimentsAndExperimentGroupsGetIds = function(p0, p1, callback) {
};
ExpressionExperimentController.getDescription = function(p0, callback) {
};
ExpressionExperimentController.getFactorValues = function(p0, callback) {
};
ExpressionExperimentController.getExperimentalFactors = function(p0, callback) {
};
ExpressionExperimentController.updateReport = function(p0, callback) {
};
ExpressionExperimentController.updatePubMed = function(p0, p1, callback) {
};
ExpressionExperimentController.deleteById = function(p0, callback) {
};
ExpressionExperimentController.getDesignMatrixRows = function(p0, callback) {
};
ExpressionExperimentController.loadExpressionExperimentDetails = function(p0, callback) {
};
ExpressionExperimentController.loadQuantitationTypes = function(p0, callback) {
};
ExpressionExperimentController.loadExpressionExperiments = function(p0, callback) {
};
ExpressionExperimentController.loadStatusSummaries = function(p0, p1, p2, p3, p4, callback) {
};
ExpressionExperimentController.removePrimaryPublication = function(p0, callback) {
};
ExpressionExperimentController.updateAllReports = function(callback) {
};
ExpressionExperimentController.updateBasics = function(p0, callback) {
};
ExpressionExperimentController.clearFromCaches = function(p0, callback ) {
};
ExpressionExperimentController.unmatchAllBioAssays = function(p0, callback) {
};
ExpressionExperimentController.canCurrentUserEditExperiment = function(p0, callback) {
};
ExpressionExperimentController.browse = function(p0, callback) {
};
ExpressionExperimentController.browseSpecificIds = function(p0, p1, callback) {
};
ExpressionExperimentController.browseByTaxon = function(p0, p1, callback) {
};
ExpressionExperimentController.loadCountsForDataSummaryTable = function(callback) {
};
ExpressionExperimentController.loadExpressionExperimentsWithQcIssues = function(callback) {
};

if (typeof ExpressionExperimentDataFetchController === 'undefined') {
    var ExpressionExperimentDataFetchController = {};
}
ExpressionExperimentDataFetchController.getDataFile = function(p0, callback) {
};
ExpressionExperimentDataFetchController.getDiffExpressionDataFile = function(p0, callback) {
};
ExpressionExperimentDataFetchController.getCoExpressionDataFile = function(p0, callback) {
};

if (typeof ExpressionExperimentLoadController === 'undefined') {
    var ExpressionExperimentLoadController = {};
}
ExpressionExperimentLoadController.load = function(p0, callback) {
};

if (typeof ExpressionExperimentSetController === 'undefined') {
    var ExpressionExperimentSetController = {};
}
ExpressionExperimentSetController.remove = function(p0, callback) {
};
ExpressionExperimentSetController.create = function(p0, callback) {
};
ExpressionExperimentSetController.update = function(p0, callback) {
};
ExpressionExperimentSetController.updateNameDesc = function(p0, callback) {
};
ExpressionExperimentSetController.updateMembers = function(p0, p1, callback) {
};
ExpressionExperimentSetController.loadAll = function(callback) {
};
ExpressionExperimentSetController.load = function(p0, callback) {
};
ExpressionExperimentSetController.loadByName = function(p0, callback) {
};
ExpressionExperimentSetController.removeUserAndSessionGroups = function(p0, callback) {
};
ExpressionExperimentSetController.addUserAndSessionGroups = function(p0, callback) {
};
ExpressionExperimentSetController.addSessionGroups = function(p0, p1, callback) {
};
ExpressionExperimentSetController.updateUserAndSessionGroups = function(p0, callback) {
};
ExpressionExperimentSetController.loadAllUserAndSessionGroups = function(callback) {
};
ExpressionExperimentSetController.loadAllSessionGroups = function(callback) {
};
ExpressionExperimentSetController.getExperimentsInSet = function(p0, callback) {
};
ExpressionExperimentSetController.getExperimentsInSetBySessionId = function(p0, callback) {
};
ExpressionExperimentSetController.getExperimentIdsInSet = function(p0, callback) {
};
ExpressionExperimentSetController.canCurrentUserEditGroup = function(p0, callback) {
};

if (typeof ExpressionExperimentReportGenerationController === 'undefined') {
    var ExpressionExperimentReportGenerationController = {};
}

ExpressionExperimentReportGenerationController.run = function(p0, callback) {
};
ExpressionExperimentReportGenerationController.runAll = function(callback) {
};

if (typeof ExtCoexpressionSearchController === 'undefined') {
    var ExtCoexpressionSearchController = {};
}
ExtCoexpressionSearchController.doSearch = function(p0, callback) {
};
ExtCoexpressionSearchController.findExpressionExperiments = function(p0, p1, callback) {
};
ExtCoexpressionSearchController.doSearchQuick2Complete = function(p0, p1, callback) {
};
ExtCoexpressionSearchController.doSearchQuick2 = function(p0, callback) {
};
ExtCoexpressionSearchController.doSearchQuick = function(p0, callback) {
};
ExtCoexpressionSearchController.doBackgroundCoexSearch = function(p0, callback) {
};

if (typeof FileUploadController === 'undefined') {
    var FileUploadController = {};
}
FileUploadController.upload = function(p0, callback) {
};
FileUploadController.getUploadStatus = function(callback) {
};

if (typeof GeneController === 'undefined') {
    var GeneController = {};
}
GeneController.findGOTerms = function(p0, callback) {
};
GeneController.loadGeneDetails = function(p0, callback) {
};
GeneController.loadGeneEvidence = function(p0, p1, p2, p3, callback) {
};
GeneController.loadAllenBrainImages = function(p0, callback) {
};

if (typeof GenePickerController === 'undefined') {
    var GenePickerController = {};
}
GenePickerController.getGenes = function(p0, callback) {
};
GenePickerController.getGenesByGOId = function(p0, p1, callback) {
};
GenePickerController.searchGenes = function(p0, p1, callback) {
};
GenePickerController.searchGenesAndGeneGroups= function(p0, p1, callback) {
};
GenePickerController.searchGenesAndGeneGroupsGetIds= function(p0, p1, callback) {
};
GenePickerController.searchGenesWithNCBIId= function(p0, p1, callback) {
};
GenePickerController.getTaxa = function(callback) {
};
GenePickerController.getTaxaSpecies = function(callback) {
};
GenePickerController.getTaxaWithGenes = function(callback) {
};
GenePickerController.getTaxaWithDatasets = function(callback) {
};
GenePickerController.getTaxaWithArrays = function(callback) {
};
GenePickerController.getTaxaWithEvidence = function(callback) {
};
GenePickerController.searchMultipleGenes = function(p0, p1, callback) {
};
GenePickerController.searchMultipleGenesGetMap = function(p0, p1, callback) {
};
GenePickerController.getGeneSetByGOId = function(p0, p1, callback) {
};

if (typeof GeoRecordBrowserController === 'undefined') {
    var GeoRecordBrowserController = {};
}
GeoRecordBrowserController.browse = function(p0, p1, p2, callback) {
};
GeoRecordBrowserController.getDetails = function(p0, callback) {
};
GeoRecordBrowserController.toggleUsability = function(p0, callback) {
};

if (typeof SecurityController === 'undefined') {
    var SecurityController = {};
}
SecurityController.createGroup = function(p0, callback) {
};
SecurityController.deleteGroup = function(p0, callback) {
};
SecurityController.getAvailableGroups = function(callback) {
};
SecurityController.getAvailableSids = function(callback) {
};
SecurityController.getUsersData = function(p0, p1, callback) {
};
SecurityController.getSecurityInfo = function(p0, callback) {
};
SecurityController.addUserToGroup = function(p0, p1, callback) {
};
SecurityController.removeUsersFromGroup = function(p0, p1, callback) {
};
SecurityController.makeGroupReadable = function(p0, p1, callback) {
};
SecurityController.makeGroupWriteable = function(p0, p1, callback) {
};
SecurityController.makePrivate = function(p0, callback) {
};
SecurityController.makePublic = function(p0, callback) {
};
SecurityController.removeGroupWriteable = function(p0, p1, callback) {
};
SecurityController.removeGroupReadable = function(p0, p1, callback) {
};
SecurityController.updatePermissions = function(p0, callback) {
};
SecurityController.updatePermission = function(p0, callback) {
};
SecurityController.getGroupMembers = function(p0, callback) {
};
SecurityController.getAvailablePrincipalSids = function(callback) {
};
SecurityController.getAuthenticatedUserNames = function(callback) {
};
SecurityController.getAuthenticatedUserCount = function(callback) {
};

if (typeof GeneSetController === 'undefined') {
    var GeneSetController = {};
}
GeneSetController.getGenesInGroup = function(p0, callback) {
};
GeneSetController.load = function(p0, callback) {
};
GeneSetController.getGeneSetsByGOId = function(p0, p1, callback) {
};
GeneSetController.update = function(p0, callback) {
};
GeneSetController.updateNameDesc = function(p0, callback) {
};
GeneSetController.updateMembers = function(p0, p1, callback) {
};
GeneSetController.updateSessionGroups = function(p0, callback) {
};
GeneSetController.updateUserAndSessionGroups = function(p0, callback) {
};
GeneSetController.create = function(p0, callback) {
};
GeneSetController.addSessionGroups = function(p0, p1, callback) {
};
GeneSetController.addUserAndSessionGroups = function(p0, callback) {
};
GeneSetController.remove = function(p0, callback) {
};
GeneSetController.removeSessionGroups = function(p0, callback) {
};
GeneSetController.removeUserAndSessionGroups = function(p0, callback) {
};
GeneSetController.getUsersGeneGroups = function(p0, p1, callback) {
};
GeneSetController.getUserSessionGeneGroups = function(p0, p1, callback) {
};
GeneSetController.getUserAndSessionGeneGroups = function(p0, p1, callback) {
};
GeneSetController.findGeneSetsByGene = function(p0, callback) {
};
GeneSetController.findGeneSetsByName = function(p0, p1, callback) {
};
GeneSetController.canCurrentUserEditGroup = function(p0, callback) {
};

if (typeof SystemMonitorController === 'undefined') {
    var SystemMonitorController = {};
}
SystemMonitorController.getHibernateStatus = function(callback) {
};
SystemMonitorController.getSpaceStatus = function(callback) {
};
SystemMonitorController.getCacheStatus = function(callback) {
};
SystemMonitorController.clearCache = function(p0, callback) {
};
SystemMonitorController.clearAllCaches = function(callback) {
};
SystemMonitorController.enableStatistics = function(callback) {
};
SystemMonitorController.disableStatistics = function(callback) {
};
SystemMonitorController.resetHibernateStatus = function(callback) {
};

if (typeof LinkAnalysisController === 'undefined') {
    var LinkAnalysisController = {};
}
LinkAnalysisController.run = function(p0, callback) {
};

if (typeof OntologyService === 'undefined') {
    var OntologyService = {};
}
OntologyService.getCategoryTerms = function(p0, callback) {
};

if (typeof ProcessedExpressionDataVectorCreateController === 'undefined') {
    var ProcessedExpressionDataVectorCreateController = {};
}
ProcessedExpressionDataVectorCreateController.run = function(p0, callback) {
};

if (typeof ProgressStatusService === 'undefined') {
    var ProgressStatusService = {};
}
ProgressStatusService.getProgressStatus = function(p0, callback) {
};
ProgressStatusService.getSubmittedTask = function(p0, callback) {
};
ProgressStatusService.cancelJob = function(p0, callback) {
};
ProgressStatusService.addEmailAlert = function(p0, callback) {
};
ProgressStatusService.getSubmittedTasks = function(callback) {
};

if (typeof TwoChannelMissingValueController === 'undefined') {
    var TwoChannelMissingValueController = {};
}
TwoChannelMissingValueController.run = function(p0, callback) {
};
TwoChannelMissingValueController.run = function(p0, callback) {
};

if (typeof SvdController === 'undefined') {
    var SvdController = {};
}
SvdController.run = function(p0, callback) {
};

if (typeof SearchService === 'undefined') {
    var SearchService = {};
}
SearchService.ajaxSearch = function(p0, callback) {
};

if (typeof TaskCompletionController === 'undefined') {
    var TaskCompletionController = {};
}
TaskCompletionController.checkResult = function(p0, callback) {
};

if (typeof TestTaskController === 'undefined') {
    var TestTaskController = {};
}
TestTaskController.run = function(p0, p1, p2, p3, callback) {
};

if (typeof FeedReader === 'undefined') {
    var FeedReader = {};
}
FeedReader.getLatestNews = function(callback) {
};

if (typeof UserListController === 'undefined') {
    var UserListController = {};
}
UserListController.getUsers = function(callback) {
};
UserListController.saveUser = function(p0, callback) {
};

if (typeof LinkOutController === 'undefined') {
    var LinkOutController = {};
}
LinkOutController.getAllenBrainAtlasLink = function(p0, callback) {
};

if (typeof PhenotypeController === 'undefined') {
    var PhenotypeController = {};
}
PhenotypeController.findEvidenceByFilters = function(p0, p1, p2, callback) {
};
PhenotypeController.findCandidateGenes = function(p0, p1, p2, callback) {
};
PhenotypeController.findExperimentMgedCategory = function(callback) {
};
PhenotypeController.calculateExternalDatabasesStatistics = function(callback) {
};
PhenotypeController.findExperimentOntologyValue = function(p0, p1, p2, callback) {
};
PhenotypeController.findEvidenceOwners = function(callback) {
};
PhenotypeController.loadAllPhenotypesByTree = function(p0, p1, callback) {
};
PhenotypeController.searchOntologyForPhenotypes = function(p0, p1, callback) {
};
PhenotypeController.findBibliographicReference = function(p0, p1, callback) {
};
PhenotypeController.processPhenotypeAssociationForm = function(p0, callback) {
};
PhenotypeController.validatePhenotypeAssociationForm = function(p0, callback) {
};
PhenotypeController.removePhenotypeAssociation = function(p0, callback) {
};
PhenotypeController.makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis = function(p0, p1, p2, callback) {
};
PhenotypeController.removeAllEvidenceFromMetaAnalysis = function(p0, callback) {
};

if (typeof JavascriptLogger === 'undefined') {
    var JavascriptLogger = {};
}
JavascriptLogger.writeToLog = function(p0, p1, p2, p3, p4, callback) {
};
JavascriptLogger.writeToDebugLog = function(p0, p1, p2, p3, p4, callback) {
};
JavascriptLogger.writeToInfoLog = function(p0, p1, p2, p3, p4, callback) {
};
JavascriptLogger.writeToWarnLog = function(p0, p1, p2, p3, p4, callback) {
};
JavascriptLogger.writeToErrorLog = function(p0, p1, p2, p3, p4, callback) {
};
JavascriptLogger.writeToFatalLog = function(p0, p1, p2, p3, p4, callback) {
};

if (typeof SignupController === 'undefined') {
    var SignupController = {};
}
SignupController.loginCheck = function(callback) {
};
