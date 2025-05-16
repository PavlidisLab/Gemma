/* this code is generated, see generate-dwr-client.py for details */
var ExpressionExperimentDataFetchController = {};
ExpressionExperimentDataFetchController._path = '/dwr';
ExpressionExperimentDataFetchController.getMetadataFiles = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController', 'getMetadataFiles', p0, callback);
}
ExpressionExperimentDataFetchController.getCoExpressionDataFile = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController', 'getCoExpressionDataFile', p0, callback);
}
ExpressionExperimentDataFetchController.getDiffExpressionDataFile = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController', 'getDiffExpressionDataFile', p0, callback);
}
ExpressionExperimentDataFetchController.getDataFile = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentDataFetchController._path, 'ExpressionExperimentDataFetchController', 'getDataFile', p0, callback);
}
window.ExpressionExperimentDataFetchController = ExpressionExperimentDataFetchController;
export default ExpressionExperimentDataFetchController;
