/* this code is generated, see generate-dwr-client.sh for details */
var GeneController = {};
GeneController._path = '/dwr';
GeneController.loadGeneDetails = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'loadGeneDetails', p0, callback);
}
GeneController.getProducts = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'getProducts', p0, callback);
}
GeneController.findGOTerms = function(p0, callback) {
  dwr.engine._execute(GeneController._path, 'GeneController', 'findGOTerms', p0, callback);
}
window.GeneController = GeneController;
export default GeneController;
