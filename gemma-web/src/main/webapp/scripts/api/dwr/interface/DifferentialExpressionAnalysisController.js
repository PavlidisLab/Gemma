/* this code is generated, see generate-dwr-client.py for details */
var DifferentialExpressionAnalysisController = {};
DifferentialExpressionAnalysisController._path = '/dwr';
DifferentialExpressionAnalysisController.run = function(p0, callback) {
  dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController', 'run', p0, callback);
}
DifferentialExpressionAnalysisController.remove = function(p0, p1, callback) {
  dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController', 'remove', p0, p1, callback);
}
DifferentialExpressionAnalysisController.redo = function(p0, p1, callback) {
  dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController', 'redo', p0, p1, callback);
}
DifferentialExpressionAnalysisController.refreshStats = function(p0, p1, callback) {
  dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController', 'refreshStats', p0, p1, callback);
}
DifferentialExpressionAnalysisController.runCustom = function(p0, p1, p2, p3, callback) {
  dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController', 'runCustom', p0, p1, p2, p3, callback);
}
DifferentialExpressionAnalysisController.determineAnalysisType = function(p0, callback) {
  dwr.engine._execute(DifferentialExpressionAnalysisController._path, 'DifferentialExpressionAnalysisController', 'determineAnalysisType', p0, callback);
}
window.DifferentialExpressionAnalysisController = DifferentialExpressionAnalysisController;
export default DifferentialExpressionAnalysisController;
