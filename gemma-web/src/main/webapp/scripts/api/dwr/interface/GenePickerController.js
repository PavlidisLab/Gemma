/* this code is generated, see generate-dwr-client.py for details */
var GenePickerController = {};
GenePickerController._path = '/dwr';
GenePickerController.searchGenesWithNCBIId = function(p0, p1, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesWithNCBIId', p0, p1, callback);
}
GenePickerController.getGeneSetByGOId = function(p0, p1, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGeneSetByGOId', p0, p1, callback);
}
GenePickerController.getGenes = function(p0, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGenes', p0, callback);
}
GenePickerController.getTaxa = function(callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxa', callback);
}
GenePickerController.getGenesByGOId = function(p0, p1, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getGenesByGOId', p0, p1, callback);
}
GenePickerController.searchGenesAndGeneGroups = function(p0, p1, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenesAndGeneGroups', p0, p1, callback);
}
GenePickerController.searchGenes = function(p0, p1, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchGenes', p0, p1, callback);
}
GenePickerController.searchMultipleGenes = function(p0, p1, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchMultipleGenes', p0, p1, callback);
}
GenePickerController.searchMultipleGenesGetMap = function(p0, p1, callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'searchMultipleGenesGetMap', p0, p1, callback);
}
GenePickerController.getTaxaWithArrays = function(callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithArrays', callback);
}
GenePickerController.getTaxaWithGenes = function(callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithGenes', callback);
}
GenePickerController.getTaxaWithDatasets = function(callback) {
  dwr.engine._execute(GenePickerController._path, 'GenePickerController', 'getTaxaWithDatasets', callback);
}
window.GenePickerController = GenePickerController;
export default GenePickerController;
