/* this code is generated, see generate-dwr-client.sh for details */
var BioMaterialController = {};
BioMaterialController._path = '/dwr';
BioMaterialController.getAnnotation = function(p0, callback) {
  dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getAnnotation', p0, callback);
}
BioMaterialController.addFactorValueTo = function(p0, p1, callback) {
  dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'addFactorValueTo', p0, p1, callback);
}
BioMaterialController.getFactorValues = function(p0, callback) {
  dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getFactorValues', p0, callback);
}
BioMaterialController.getBioMaterials = function(p0, callback) {
  dwr.engine._execute(BioMaterialController._path, 'BioMaterialController', 'getBioMaterials', p0, callback);
}
window.BioMaterialController = BioMaterialController;
export default BioMaterialController;
