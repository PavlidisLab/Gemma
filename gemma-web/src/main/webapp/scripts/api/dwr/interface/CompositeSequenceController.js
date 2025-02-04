/* this code is generated, see generate-dwr-client.py for details */
var CompositeSequenceController = {};
CompositeSequenceController._path = '/dwr';
CompositeSequenceController.search = function(p0, p1, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'search', p0, p1, callback);
}
CompositeSequenceController.getCsSummaries = function(p0, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getCsSummaries', p0, callback);
}
CompositeSequenceController.getGeneCsSummaries = function(p0, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getGeneCsSummaries', p0, callback);
}
CompositeSequenceController.getGeneMappingSummary = function(p0, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getGeneMappingSummary', p0, callback);
}
window.CompositeSequenceController = CompositeSequenceController;
export default CompositeSequenceController;
