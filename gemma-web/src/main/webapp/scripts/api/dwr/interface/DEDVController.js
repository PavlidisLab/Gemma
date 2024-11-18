/* this code is generated, see generate-dwr-client.sh for details */
var DEDVController = {};
DEDVController._path = '/dwr';
DEDVController.getDEDVForCoexpressionVisualization = function(p0, p1, p2, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForCoexpressionVisualization', p0, p1, p2, callback);
}
DEDVController.getDEDV = function(p0, p1, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDV', p0, p1, callback);
}
DEDVController.getDEDVForDiffExVisualization = function(p0, p1, p2, p3, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualization', p0, p1, p2, p3, callback);
}
DEDVController.getDEDVForDiffExVisualizationByExperiment = function(p0, p1, p2, p3, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualizationByExperiment', p0, p1, p2, p3, callback);
}
DEDVController.getDEDVForDiffExVisualizationByThreshold = function(p0, p1, p2, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForDiffExVisualizationByThreshold', p0, p1, p2, callback);
}
DEDVController.getDEDVForPcaVisualization = function(p0, p1, p2, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForPcaVisualization', p0, p1, p2, callback);
}
DEDVController.getDEDVForVisualization = function(p0, p1, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForVisualization', p0, p1, callback);
}
DEDVController.getDEDVForVisualizationByProbe = function(p0, p1, callback) {
  dwr.engine._execute(DEDVController._path, 'DEDVController', 'getDEDVForVisualizationByProbe', p0, p1, callback);
}
window.DEDVController = DEDVController
