/* this code is generated, see generate-dwr-client.sh for details */
var AnnotationController = {};
AnnotationController._path = '/dwr';
AnnotationController.createExperimentTag = function(p0, p1, callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'createExperimentTag', p0, p1, callback);
}
AnnotationController.reinitializeOntologyIndices = function(callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'reinitializeOntologyIndices', callback);
}
AnnotationController.removeExperimentTag = function(p0, p1, callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'removeExperimentTag', p0, p1, callback);
}
AnnotationController.findTerm = function(p0, p1, callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'findTerm', p0, p1, callback);
}
AnnotationController.getCategoryTerms = function(callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'getCategoryTerms', callback);
}
AnnotationController.getRelationTerms = function(callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'getRelationTerms', callback);
}
window.AnnotationController = AnnotationController;
export default AnnotationController;
