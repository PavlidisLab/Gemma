/* this code is generated, see generate-dwr-client.py for details */
var ExpressionExperimentReportGenerationController = {};
ExpressionExperimentReportGenerationController._path = '/dwr';
ExpressionExperimentReportGenerationController.run = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentReportGenerationController._path, 'ExpressionExperimentReportGenerationController', 'run', p0, callback);
}
ExpressionExperimentReportGenerationController.runAll = function(callback) {
  dwr.engine._execute(ExpressionExperimentReportGenerationController._path, 'ExpressionExperimentReportGenerationController', 'runAll', callback);
}
window.ExpressionExperimentReportGenerationController = ExpressionExperimentReportGenerationController;
export default ExpressionExperimentReportGenerationController;
