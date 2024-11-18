/* this code is generated, see generate-dwr-client.sh for details */
var CoexpressionSearchController = {};
CoexpressionSearchController._path = '/dwr';
CoexpressionSearchController.doBackgroundCoexSearch = function(p0, callback) {
  dwr.engine._execute(CoexpressionSearchController._path, 'CoexpressionSearchController', 'doBackgroundCoexSearch', p0, callback);
}
CoexpressionSearchController.doSearch = function(p0, callback) {
  dwr.engine._execute(CoexpressionSearchController._path, 'CoexpressionSearchController', 'doSearch', p0, callback);
}
CoexpressionSearchController.doSearchQuickComplete = function(p0, p1, callback) {
  dwr.engine._execute(CoexpressionSearchController._path, 'CoexpressionSearchController', 'doSearchQuickComplete', p0, p1, callback);
}
window.CoexpressionSearchController = CoexpressionSearchController
