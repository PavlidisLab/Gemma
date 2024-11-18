/* this code is generated, see generate-dwr-client.sh for details */
var ExpressionExperimentReportGenerationController = {};
ExpressionExperimentReportGenerationController._path = '/dwr';
ExpressionExperimentReportGenerationController.runAll = function(callback) {
  dwr.engine._execute(ExpressionExperimentReportGenerationController._path, 'ExpressionExperimentReportGenerationController', 'runAll', callback);
}
ExpressionExperimentReportGenerationController.run = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentReportGenerationController._path, 'ExpressionExperimentReportGenerationController', 'run', p0, callback);
}
window.ExpressionExperimentReportGenerationController = ExpressionExperimentReportGenerationController
