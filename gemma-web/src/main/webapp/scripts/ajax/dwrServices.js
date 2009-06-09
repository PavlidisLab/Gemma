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
if (ArrayDesignController == null)
	var ArrayDesignController = {};
ArrayDesignController._path = '/Gemma/dwr';
ArrayDesignController.remove = function(p0, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'remove', p0, callback);
}
ArrayDesignController.getArrayDesigns = function(p0, p1, p2, callback) {
	dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getArrayDesigns', p0, p1, p2, callback);
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
if (BibliographicReferenceController == null)
	var BibliographicReferenceController = {};
BibliographicReferenceController._path = '/Gemma/dwr';
BibliographicReferenceController.update = function(p0, callback) {
	dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'update', p0,
			callback);
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
CharacteristicBrowserController.findCharacteristics = function(p0, p1, p2, p3, p4, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
			'findCharacteristics', p0, p1, p2, p3, p4, callback);
}
CharacteristicBrowserController.removeCharacteristics = function(p0, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
			'removeCharacteristics', p0, callback);
}
CharacteristicBrowserController.updateCharacteristics = function(p0, callback) {
	dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController',
			'updateCharacteristics', p0, callback);
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
CompositeSequenceController.getBlatMappingSummary = function(p0, callback) {
	dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getBlatMappingSummary', p0,
			callback);
}
// ====================================================================================
if (CustomCompassIndexController == null)
	var CustomCompassIndexController = {};
CustomCompassIndexController._path = '/Gemma/dwr';
CustomCompassIndexController.run = function(p0, callback) {
	dwr.engine._execute(CustomCompassIndexController._path, 'CustomCompassIndexController', 'run', p0, callback);
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
DEDVController.getDEDVForDiffExVisualizationByThreshold = function(p0, p1,p2, callback) {
	dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualizationByThreshold', p0, p1,p2,callback);
}

// ====================================================================================
if (DifferentialExpressionAnalysisController == null)
	var DifferentialExpressionAnalysisController = {};
DifferentialExpressionAnalysisController._path = '/Gemma/dwr';
DifferentialExpressionAnalysisController.run = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'run', p0, callback);
}
DifferentialExpressionAnalysisController.determineAnalysisType = function(p0, p1, p2, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'determineAnalysisType', p0, p1, p2, callback);
}
DifferentialExpressionAnalysisController.determineAnalysisType = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'determineAnalysisType', p0, callback);
}
DifferentialExpressionAnalysisController.runCustom = function(p0, p1, p2, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'runCustom', p0, p1, p2, callback);
}
DifferentialExpressionAnalysisController.run = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController',
			'run', p0, callback);
}
// ====================================================================================
if (DifferentialExpressionSearchController == null)
	var DifferentialExpressionSearchController = {};
DifferentialExpressionSearchController._path = '/Gemma/dwr';
DifferentialExpressionSearchController.getDifferentialExpression = function(p0, p1, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDifferentialExpression', p0, p1, callback);
}
DifferentialExpressionSearchController.getDiffExpressionForGenes = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDiffExpressionForGenes', p0, callback);
}
DifferentialExpressionSearchController.getDifferentialExpressionForFactors = function(p0, p1, p2, p3, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getDifferentialExpressionForFactors', p0, p1, p2, p3, callback);
}
DifferentialExpressionSearchController.getFactors = function(p0, callback) {
	dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController',
			'getFactors', p0, callback);
}
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
ExperimentalDesignController.deleteExperimentalFactors = function(p0, p1, callback) {
	dwr.engine._execute(ExperimentalDesignController._path, 'ExperimentalDesignController',
			'deleteExperimentalFactors', p0, p1, callback);
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
}
ExpressionExperimentController.loadExpressionExperimentDetails = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'loadExpressionExperimentDetails', p0, callback);
}
ExpressionExperimentController.loadExpressionExperiments = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'loadExpressionExperiments', p0, callback);
}
ExpressionExperimentController.loadStatusSummaries = function(p0, p1, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadStatusSummaries',
			p0, p1, callback);
}
ExpressionExperimentController.removePrimaryPublication = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'removePrimaryPublication', p0, callback);
}
ExpressionExperimentController.updateAllReports = function(callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateAllReports',
			callback);
}
ExpressionExperimentController.updateBasics = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateBasics', p0,
			callback);
}
ExpressionExperimentController.updateBioMaterialMapping = function(callback) {
	dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController',
			'updateBioMaterialMapping', callback);
}
// ====================================================================================
if (ExpressionExperimentDataFetchController == null)
	var ExpressionExperimentDataFetchController = {};
ExpressionExperimentDataFetchController._path = '/Gemma/dwr';
ExpressionExperimentDataFetchController.getDataFile = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
			'getDataFile', p0, callback);
}
ExpressionExperimentDataFetchController.getDiffExpressionDataFile = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
			'getDiffExpressionDataFile', p0, callback);
}
ExpressionExperimentDataFetchController.getCoExpressionDataFile = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController',
			'getCoExpressionDataFile', p0, callback);
}
// ====================================================================================
if (ExpressionExperimentLoadController == null)
	var ExpressionExperimentLoadController = {};
ExpressionExperimentLoadController._path = '/Gemma/dwr';
ExpressionExperimentLoadController.run = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentLoadController._path, 'ExpressionExperimentLoadController', 'run', p0,
			callback);
}
ExpressionExperimentLoadController.run = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentLoadController._path, 'ExpressionExperimentLoadController', 'run', p0,
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
ExpressionExperimentSetController.getAvailableExpressionExperimentSets = function(callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
			'getAvailableExpressionExperimentSets', callback);
}
ExpressionExperimentSetController.getExperimentsInSet = function(p0, callback) {
	dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController',
			'getExperimentsInSet', p0, callback);
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
// ====================================================================================
if (FileUploadController == null)
	var FileUploadController = {};
FileUploadController._path = '/Gemma/dwr';
FileUploadController.upload = function(p0, callback) {
	dwr.engine._execute(FileUploadController._path, 'FileUploadController', 'upload', p0, callback);
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
// ====================================================================================
if (GenePickerController == null)
	var GenePickerController = {};
GenePickerController._path = '/Gemma/dwr';
GenePickerController.getGenes = function(p0, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGenes', p0, callback);
}
GenePickerController.searchGenes = function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenes', p0, p1, callback);
}
GenePickerController.getTaxa = function(callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxa', callback);
}
GenePickerController.searchMultipleGenes = function(p0, p1, callback) {
	dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchMultipleGenes', p0, p1, callback);
}
// ====================================================================================
if (GeoBrowserService == null)
	var GeoBrowserService = {};
GeoBrowserService._path = '/Gemma/dwr';
GeoBrowserService.getDetails = function(p0, callback) {
	dwr.engine._execute(GeoBrowserService._path, 'GeoBrowserService', 'getDetails', p0, callback);
}
// ====================================================================================
if (HibernateMonitorController == null)
	var HibernateMonitorController = {};
HibernateMonitorController._path = '/Gemma/dwr';
HibernateMonitorController.getHibernateStatus = function(callback) {
	dwr.engine._execute(HibernateMonitorController._path, 'HibernateMonitorController', 'getHibernateStatus', callback);
}
HibernateMonitorController.getSpaceStatus = function(callback) {
	dwr.engine._execute(HibernateMonitorController._path, 'HibernateMonitorController', 'getSpaceStatus', callback);
}
HibernateMonitorController.getCacheStatus = function(callback) {
	dwr.engine._execute(HibernateMonitorController._path, 'HibernateMonitorController', 'getCacheStatus', callback);
}
HibernateMonitorController.flushCache = function(p0, callback) {
	dwr.engine._execute(HibernateMonitorController._path, 'HibernateMonitorController', 'flushCache', p0, callback);
}
HibernateMonitorController.flushAllCaches = function(callback) {
	dwr.engine._execute(HibernateMonitorController._path, 'HibernateMonitorController', 'flushAllCaches', callback);
}
// ====================================================================================
if (LinkAnalysisController == null)
	var LinkAnalysisController = {};
LinkAnalysisController._path = '/Gemma/dwr';
LinkAnalysisController.run = function(p0, callback) {
	dwr.engine._execute(LinkAnalysisController._path, 'LinkAnalysisController', 'run', p0, callback);
}
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
if (OntologyService == null)
	var OntologyService = {};
OntologyService._path = '/Gemma/dwr';
OntologyService.findExactTerm = function(p0, p1, callback) {
	dwr.engine._execute(OntologyService._path, 'OntologyService', 'findExactTerm', p0, p1, callback);
}
OntologyService.saveBioMaterialStatement = function(p0, p1, callback) {
	dwr.engine._execute(OntologyService._path, 'OntologyService', 'saveBioMaterialStatement', p0, p1, callback);
}
OntologyService.saveExpressionExperimentStatement = function(p0, p1, callback) {
	dwr.engine
			._execute(OntologyService._path, 'OntologyService', 'saveExpressionExperimentStatement', p0, p1, callback);
}
OntologyService.saveExpressionExperimentStatement = function(p0, p1, callback) {
	dwr.engine._execute(OntologyService._path, 'OntologyService', 'saveExpressionExperimentStatementById', p0, p1,
			callback);
}
OntologyService.saveExpressionExperimentStatement = function(p0, p1, callback) {
	dwr.engine._execute(OntologyService._path, 'OntologyService', 'saveExpressionExperimentsStatement', p0, p1,
			callback);
}
OntologyService.removeExpressionExperimentStatement = function(p0, p1, callback) {
	dwr.engine._execute(OntologyService._path, 'OntologyService', 'removeExpressionExperimentStatement', p0, p1,
			callback);
}
OntologyService.removeBioMaterialStatement = function(p0, p1, callback) {
	dwr.engine._execute(OntologyService._path, 'OntologyService', 'removeBioMaterialStatement', p0, p1, callback);
}
// ====================================================================================
if (ProcessedExpressionDataVectorCreateController == null)
	var ProcessedExpressionDataVectorCreateController = {};
ProcessedExpressionDataVectorCreateController._path = '/Gemma/dwr';
ProcessedExpressionDataVectorCreateController.run = function(p0, callback) {
	dwr.engine._execute(ProcessedExpressionDataVectorCreateController._path,
			'ProcessedExpressionDataVectorCreateController', 'run', p0, callback);
}
ProcessedExpressionDataVectorCreateController.run = function(p0, callback) {
	dwr.engine._execute(ProcessedExpressionDataVectorCreateController._path,
			'ProcessedExpressionDataVectorCreateController', 'run', p0, callback);
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
if (ProgressStatusService == null)
	var ProgressStatusService = {};
ProgressStatusService._path = '/Gemma/dwr';
ProgressStatusService.getProgressStatus = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getProgressStatus', p0, callback);
}
ProgressStatusService.cancelJob = function(p0, callback) {
	dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'cancelJob', p0, callback);
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