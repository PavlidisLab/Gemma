/* this code is generated, see generate-dwr-client.sh for details */
var ProgressStatusService = {};
ProgressStatusService._path = '/dwr';
ProgressStatusService.cancelJob = function(p0, callback) {
  dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'cancelJob', p0, callback);
}
ProgressStatusService.getProgressStatus = function(p0, callback) {
  dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getProgressStatus', p0, callback);
}
ProgressStatusService.getSubmittedTask = function(p0, callback) {
  dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getSubmittedTask', p0, callback);
}
ProgressStatusService.getSubmittedTasks = function(callback) {
  dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'getSubmittedTasks', callback);
}
ProgressStatusService.addEmailAlert = function(p0, callback) {
  dwr.engine._execute(ProgressStatusService._path, 'ProgressStatusService', 'addEmailAlert', p0, callback);
}
window.ProgressStatusService = ProgressStatusService
