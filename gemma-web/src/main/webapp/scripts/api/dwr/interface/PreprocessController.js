/* this code is generated, see generate-dwr-client.sh for details */
var PreprocessController = {};
PreprocessController._path = '/dwr';
PreprocessController.run = function(p0, callback) {
  dwr.engine._execute(PreprocessController._path, 'PreprocessController', 'run', p0, callback);
}
PreprocessController.diagnostics = function(p0, callback) {
  dwr.engine._execute(PreprocessController._path, 'PreprocessController', 'diagnostics', p0, callback);
}
window.PreprocessController = PreprocessController;
export default PreprocessController;
