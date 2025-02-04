/* this code is generated, see generate-dwr-client.py for details */
var ExpressionExperimentController = {};
ExpressionExperimentController._path = '/dwr';
ExpressionExperimentController.getAnnotation = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getAnnotation', p0, callback);
}
ExpressionExperimentController.find = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'find', p0, p1, callback);
}
ExpressionExperimentController.browseByTaxon = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browseByTaxon', p0, p1, callback);
}
ExpressionExperimentController.browseSpecificIds = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browseSpecificIds', p0, p1, callback);
}
ExpressionExperimentController.canCurrentUserEditExperiment = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'canCurrentUserEditExperiment', p0, callback);
}
ExpressionExperimentController.clearFromCaches = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'clearFromCaches', p0, callback);
}
ExpressionExperimentController.deleteById = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'deleteById', p0, callback);
}
ExpressionExperimentController.getDesignMatrixRows = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getDesignMatrixRows', p0, callback);
}
ExpressionExperimentController.loadCountsForDataSummaryTable = function(callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadCountsForDataSummaryTable', callback);
}
ExpressionExperimentController.loadExpressionExperimentDetails = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadExpressionExperimentDetails', p0, callback);
}
ExpressionExperimentController.recalculateBatchConfound = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'recalculateBatchConfound', p0, callback);
}
ExpressionExperimentController.recalculateBatchEffect = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'recalculateBatchEffect', p0, callback);
}
ExpressionExperimentController.runGeeq = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'runGeeq', p0, p1, callback);
}
ExpressionExperimentController.loadExpressionExperiments = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadExpressionExperiments', p0, callback);
}
ExpressionExperimentController.loadExperimentsForPlatform = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadExperimentsForPlatform', p0, callback);
}
ExpressionExperimentController.loadDetailedExpressionExperiments = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadDetailedExpressionExperiments', p0, callback);
}
ExpressionExperimentController.loadExpressionExperimentsWithQcIssues = function(callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadExpressionExperimentsWithQcIssues', callback);
}
ExpressionExperimentController.loadQuantitationTypes = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadQuantitationTypes', p0, callback);
}
ExpressionExperimentController.loadStatusSummaries = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'loadStatusSummaries', p0, p1, p2, p3, p4, callback);
}
ExpressionExperimentController.removePrimaryPublication = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'removePrimaryPublication', p0, callback);
}
ExpressionExperimentController.unmatchAllBioAssays = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'unmatchAllBioAssays', p0, callback);
}
ExpressionExperimentController.updateBasics = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updateBasics', p0, callback);
}
ExpressionExperimentController.updatePubMed = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'updatePubMed', p0, p1, callback);
}
ExpressionExperimentController.browse = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'browse', p0, callback);
}
ExpressionExperimentController.getExperimentalFactors = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getExperimentalFactors', p0, callback);
}
ExpressionExperimentController.getFactorValues = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getFactorValues', p0, callback);
}
ExpressionExperimentController.searchExpressionExperiments = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'searchExpressionExperiments', p0, callback);
}
ExpressionExperimentController.searchExperimentsAndExperimentGroups = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'searchExperimentsAndExperimentGroups', p0, p1, callback);
}
ExpressionExperimentController.getAllTaxonExperimentGroup = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getAllTaxonExperimentGroup', p0, callback);
}
ExpressionExperimentController.getDescription = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentController._path, 'ExpressionExperimentController', 'getDescription', p0, callback);
}
window.ExpressionExperimentController = ExpressionExperimentController;
export default ExpressionExperimentController;
