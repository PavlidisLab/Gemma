/* this code is generated, see generate-dwr-client.py for details */
var ExpressionExperimentLoadController = {};
ExpressionExperimentLoadController._path = '/dwr';
ExpressionExperimentLoadController.load = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentLoadController._path, 'ExpressionExperimentLoadController', 'load', p0, callback);
}
window.ExpressionExperimentLoadController = ExpressionExperimentLoadController;
export default ExpressionExperimentLoadController;
