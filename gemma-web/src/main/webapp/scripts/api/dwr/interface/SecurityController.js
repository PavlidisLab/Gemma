/* this code is generated, see generate-dwr-client.sh for details */
var SecurityController = {};
SecurityController._path = '/dwr';
SecurityController.createGroup = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'createGroup', p0, callback);
}
SecurityController.removeGroupReadable = function(p0, p1, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeGroupReadable', p0, p1, callback);
}
SecurityController.removeGroupWriteable = function(p0, p1, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeGroupWriteable', p0, p1, callback);
}
SecurityController.removeUsersFromGroup = function(p0, p1, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'removeUsersFromGroup', p0, p1, callback);
}
SecurityController.updatePermission = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'updatePermission', p0, callback);
}
SecurityController.updatePermissions = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'updatePermissions', p0, callback);
}
SecurityController.getAvailableGroups = function(callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailableGroups', callback);
}
SecurityController.getAvailablePrincipalSids = function(callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailablePrincipalSids', callback);
}
SecurityController.getSecurityInfo = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getSecurityInfo', p0, callback);
}
SecurityController.getUsersData = function(p0, p1, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getUsersData', p0, p1, callback);
}
SecurityController.makeGroupReadable = function(p0, p1, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'makeGroupReadable', p0, p1, callback);
}
SecurityController.makeGroupWriteable = function(p0, p1, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'makeGroupWriteable', p0, p1, callback);
}
SecurityController.getGroupMembers = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getGroupMembers', p0, callback);
}
SecurityController.deleteGroup = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'deleteGroup', p0, callback);
}
SecurityController.addUserToGroup = function(p0, p1, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'addUserToGroup', p0, p1, callback);
}
SecurityController.makePrivate = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'makePrivate', p0, callback);
}
SecurityController.makePublic = function(p0, callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'makePublic', p0, callback);
}
SecurityController.getAuthenticatedUserCount = function(callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAuthenticatedUserCount', callback);
}
SecurityController.getAuthenticatedUserNames = function(callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAuthenticatedUserNames', callback);
}
SecurityController.getAvailableSids = function(callback) {
  dwr.engine._execute(SecurityController._path, 'SecurityController', 'getAvailableSids', callback);
}
window.SecurityController = SecurityController;
export default SecurityController;
