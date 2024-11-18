/* this code is generated, see generate-dwr-client.sh for details */
var CompositeSequenceController = {};
CompositeSequenceController._path = '/dwr';
CompositeSequenceController.getGeneMappingSummary = function(p0, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getGeneMappingSummary', p0, callback);
}
CompositeSequenceController.getCsSummaries = function(p0, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getCsSummaries', p0, callback);
}
CompositeSequenceController.getGeneCsSummaries = function(p0, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'getGeneCsSummaries', p0, callback);
}
CompositeSequenceController.search = function(p0, p1, callback) {
  dwr.engine._execute(CompositeSequenceController._path, 'CompositeSequenceController', 'search', p0, p1, callback);
}
window.CompositeSequenceController = CompositeSequenceController
