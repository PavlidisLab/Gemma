/* this code is generated, see generate-dwr-client.py for details */
var AnnotationController = {};
AnnotationController._path = '/dwr';
AnnotationController.reinitializeOntologyIndices = function(callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'reinitializeOntologyIndices', callback);
}
AnnotationController.getCategoryTerms = function(callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'getCategoryTerms', callback);
}
AnnotationController.getRelationTerms = function(callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'getRelationTerms', callback);
}
AnnotationController.findTerm = function(p0, p1, callback) {
  dwr.engine._execute(AnnotationController._path, 'AnnotationController', 'findTerm', p0, p1, callback);
}
window.AnnotationController = AnnotationController;
export default AnnotationController;
