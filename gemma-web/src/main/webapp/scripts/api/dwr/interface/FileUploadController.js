/* this code is generated, see generate-dwr-client.sh for details */
var FileUploadController = {};
FileUploadController._path = '/dwr';
FileUploadController.upload = function(p0, callback) {
  dwr.engine._execute(FileUploadController._path, 'FileUploadController', 'upload', p0, callback);
}
FileUploadController.getUploadStatus = function(callback) {
  dwr.engine._execute(FileUploadController._path, 'FileUploadController', 'getUploadStatus', callback);
}
window.FileUploadController = FileUploadController
