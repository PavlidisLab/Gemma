/* this code is generated, see generate-dwr-client.sh for details */
var SignupController = {};
SignupController._path = '/dwr';
SignupController.loginCheck = function(callback) {
  dwr.engine._execute(SignupController._path, 'SignupController', 'loginCheck', callback);
}
window.SignupController = SignupController;
export default SignupController;
