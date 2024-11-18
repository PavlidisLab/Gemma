/* this code is generated, see generate-dwr-client.sh for details */
var GeneController = {};
GeneController._path = '/dwr';
GeneController.getProducts = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'getProducts', p0, callback);
}
GeneController.findGOTerms = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'findGOTerms', p0, callback);
}
GeneController.loadAllenBrainImages = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'loadAllenBrainImages', p0, callback);
}
GeneController.loadGeneDetails = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'loadGeneDetails', p0, callback);
}
GeneController.getGeneABALink = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'getGeneABALink', p0, callback);
}
GeneController.loadGeneEvidence = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'loadGeneEvidence', p0, p1, p2, p3, p4, callback);
}
window.GeneController = GeneController
