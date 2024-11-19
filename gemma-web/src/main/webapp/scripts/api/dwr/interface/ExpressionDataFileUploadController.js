/* this code is generated, see generate-dwr-client.sh for details */
var ExpressionDataFileUploadController = {};
ExpressionDataFileUploadController._path = '/dwr';
ExpressionDataFileUploadController.validate = function(p0, callback) {
  dwr.engine._execute(ExpressionDataFileUploadController._path, 'ExpressionDataFileUploadController', 'validate', p0, callback);
}
ExpressionDataFileUploadController.load = function(p0, callback) {
  dwr.engine._execute(ExpressionDataFileUploadController._path, 'ExpressionDataFileUploadController', 'load', p0, callback);
}
window.ExpressionDataFileUploadController = ExpressionDataFileUploadController;
export default ExpressionDataFileUploadController;
