/* this code is generated, see generate-dwr-client.py for details */
var ExpressionExperimentSetController = {};
ExpressionExperimentSetController._path = '/dwr';
ExpressionExperimentSetController.remove = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'remove', p0, callback);
}
ExpressionExperimentSetController.update = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'update', p0, callback);
}
ExpressionExperimentSetController.load = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'load', p0, callback);
}
ExpressionExperimentSetController.create = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'create', p0, callback);
}
ExpressionExperimentSetController.loadAllSessionGroups = function(callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadAllSessionGroups', callback);
}
ExpressionExperimentSetController.loadAllUserAndSessionGroups = function(callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadAllUserAndSessionGroups', callback);
}
ExpressionExperimentSetController.loadByName = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadByName', p0, callback);
}
ExpressionExperimentSetController.removeUserAndSessionGroups = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'removeUserAndSessionGroups', p0, callback);
}
ExpressionExperimentSetController.updateNameDesc = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateNameDesc', p0, callback);
}
ExpressionExperimentSetController.updateSessionGroups = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateSessionGroups', p0, callback);
}
ExpressionExperimentSetController.updateUserAndSessionGroups = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateUserAndSessionGroups', p0, callback);
}
ExpressionExperimentSetController.addSessionGroups = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'addSessionGroups', p0, p1, callback);
}
ExpressionExperimentSetController.addSessionGroup = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'addSessionGroup', p0, p1, callback);
}
ExpressionExperimentSetController.addUserAndSessionGroups = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'addUserAndSessionGroups', p0, callback);
}
ExpressionExperimentSetController.canCurrentUserEditGroup = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'canCurrentUserEditGroup', p0, callback);
}
ExpressionExperimentSetController.getExperimentIdsInSet = function(p0, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'getExperimentIdsInSet', p0, callback);
}
ExpressionExperimentSetController.getExperimentsInSet = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'getExperimentsInSet', p0, p1, callback);
}
ExpressionExperimentSetController.updateMembers = function(p0, p1, callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'updateMembers', p0, p1, callback);
}
ExpressionExperimentSetController.loadAll = function(callback) {
  dwr.engine._execute(ExpressionExperimentSetController._path, 'ExpressionExperimentSetController', 'loadAll', callback);
}
window.ExpressionExperimentSetController = ExpressionExperimentSetController;
export default ExpressionExperimentSetController;
