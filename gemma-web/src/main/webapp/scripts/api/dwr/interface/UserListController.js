/* this code is generated, see generate-dwr-client.sh for details */
var UserListController = {};
UserListController._path = '/dwr';
UserListController.saveUser = function(p0, callback) {
  dwr.engine._execute(UserListController._path, 'UserListController', 'saveUser', p0, callback);
}
UserListController.getUsers = function(callback) {
  dwr.engine._execute(UserListController._path, 'UserListController', 'getUsers', callback);
}
window.UserListController = UserListController;
export default UserListController;
