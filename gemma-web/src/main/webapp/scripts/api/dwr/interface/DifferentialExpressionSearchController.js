/* this code is generated, see generate-dwr-client.sh for details */
var DifferentialExpressionSearchController = {};
DifferentialExpressionSearchController._path = '/dwr';
DifferentialExpressionSearchController.getDifferentialExpression = function(p0, p1, p2, callback) {
  dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController', 'getDifferentialExpression', p0, p1, p2, callback);
}
DifferentialExpressionSearchController.getDifferentialExpression = function(p0, p1, callback) {
  dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController', 'getDifferentialExpression', p0, p1, callback);
}
DifferentialExpressionSearchController.getFactors = function(p0, callback) {
  dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController', 'getFactors', p0, callback);
}
DifferentialExpressionSearchController.getDifferentialExpressionWithoutBatch = function(p0, p1, p2, callback) {
  dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController', 'getDifferentialExpressionWithoutBatch', p0, p1, p2, callback);
}
DifferentialExpressionSearchController.scheduleDiffExpSearchTask = function(p0, p1, p2, callback) {
  dwr.engine._execute(DifferentialExpressionSearchController._path, 'DifferentialExpressionSearchController', 'scheduleDiffExpSearchTask', p0, p1, p2, callback);
}
window.DifferentialExpressionSearchController = DifferentialExpressionSearchController
