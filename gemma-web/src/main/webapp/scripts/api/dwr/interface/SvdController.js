/* this code is generated, see generate-dwr-client.sh for details */
var SvdController = {};
SvdController._path = '/dwr';
SvdController.run = function(p0, callback) {
  dwr.engine._execute(SvdController._path, 'SvdController', 'run', p0, callback);
}
window.SvdController = SvdController
