/* this code is generated, see generate-dwr-client.py for details */
var BioAssayController = {};
BioAssayController._path = '/dwr';
BioAssayController.markOutlier = function(p0, callback) {
  dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'markOutlier', p0, callback);
}
BioAssayController.unmarkOutlier = function(p0, callback) {
  dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'unmarkOutlier', p0, callback);
}
BioAssayController.getBioAssays = function(p0, callback) {
  dwr.engine._execute(BioAssayController._path, 'BioAssayController', 'getBioAssays', p0, callback);
}
window.BioAssayController = BioAssayController;
export default BioAssayController;
