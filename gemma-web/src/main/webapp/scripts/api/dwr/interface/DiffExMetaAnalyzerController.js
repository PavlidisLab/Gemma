/* this code is generated, see generate-dwr-client.py for details */
var DiffExMetaAnalyzerController = {};
DiffExMetaAnalyzerController._path = '/dwr';
DiffExMetaAnalyzerController.analyzeResultSets = function(p0, callback) {
  dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'analyzeResultSets', p0, callback);
}
DiffExMetaAnalyzerController.removeMetaAnalysis = function(p0, callback) {
  dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'removeMetaAnalysis', p0, callback);
}
DiffExMetaAnalyzerController.saveResultSets = function(p0, p1, p2, callback) {
  dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'saveResultSets', p0, p1, p2, callback);
}
DiffExMetaAnalyzerController.findDetailMetaAnalysisById = function(p0, callback) {
  dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'findDetailMetaAnalysisById', p0, callback);
}
DiffExMetaAnalyzerController.loadAllMetaAnalyses = function(callback) {
  dwr.engine._execute(DiffExMetaAnalyzerController._path, 'DiffExMetaAnalyzerController', 'loadAllMetaAnalyses', callback);
}
window.DiffExMetaAnalyzerController = DiffExMetaAnalyzerController;
export default DiffExMetaAnalyzerController;
