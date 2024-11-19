/* this code is generated, see generate-dwr-client.sh for details */
var TaskCompletionController = {};
TaskCompletionController._path = '/dwr';
TaskCompletionController.checkResult = function(p0, callback) {
  dwr.engine._execute(TaskCompletionController._path, 'TaskCompletionController', 'checkResult', p0, callback);
}
window.TaskCompletionController = TaskCompletionController;
export default TaskCompletionController;
