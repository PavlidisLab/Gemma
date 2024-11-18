/* this code is generated, see generate-dwr-client.sh for details */
var UserListController = {};
UserListController._path = '/dwr';
UserListController.getUsers = function(callback) {
  dwr.engine._execute(UserListController._path, 'UserListController', 'getUsers', callback);
}
UserListController.saveUser = function(p0, callback) {
  dwr.engine._execute(UserListController._path, 'UserListController', 'saveUser', p0, callback);
}
window.UserListController = UserListController
