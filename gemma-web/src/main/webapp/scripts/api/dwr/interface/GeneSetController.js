/* this code is generated, see generate-dwr-client.py for details */
var GeneSetController = {};
GeneSetController._path = '/dwr';
GeneSetController.remove = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'remove', p0, callback);
}
GeneSetController.update = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'update', p0, callback);
}
GeneSetController.load = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'load', p0, callback);
}
GeneSetController.create = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'create', p0, callback);
}
GeneSetController.removeSessionGroups = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'removeSessionGroups', p0, callback);
}
GeneSetController.removeUserAndSessionGroups = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'removeUserAndSessionGroups', p0, callback);
}
GeneSetController.updateNameDesc = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateNameDesc', p0, callback);
}
GeneSetController.updateSessionGroups = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateSessionGroups', p0, callback);
}
GeneSetController.updateUserAndSessionGroups = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateUserAndSessionGroups', p0, callback);
}
GeneSetController.addSessionGroups = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addSessionGroups', p0, p1, callback);
}
GeneSetController.addSessionGroup = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addSessionGroup', p0, p1, callback);
}
GeneSetController.addUserAndSessionGroups = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'addUserAndSessionGroups', p0, callback);
}
GeneSetController.canCurrentUserEditGroup = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'canCurrentUserEditGroup', p0, callback);
}
GeneSetController.getUserAndSessionGeneGroups = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUserAndSessionGeneGroups', p0, p1, callback);
}
GeneSetController.getUserSessionGeneGroups = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUserSessionGeneGroups', p0, p1, callback);
}
GeneSetController.updateSessionGroup = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateSessionGroup', p0, callback);
}
GeneSetController.findGeneSetsByName = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'findGeneSetsByName', p0, p1, callback);
}
GeneSetController.updateMembers = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'updateMembers', p0, p1, callback);
}
GeneSetController.findGeneSetsByGene = function(p0, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'findGeneSetsByGene', p0, callback);
}
GeneSetController.getUsersGeneGroups = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getUsersGeneGroups', p0, p1, callback);
}
GeneSetController.getGenesInGroup = function(p0, p1, callback) {
  dwr.engine._execute(GeneSetController._path, 'GeneSetController', 'getGenesInGroup', p0, p1, callback);
}
window.GeneSetController = GeneSetController;
export default GeneSetController;
