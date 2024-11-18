/* this code is generated, see generate-dwr-client.sh for details */
var ArrayDesignController = {};
ArrayDesignController._path = '/dwr';
ArrayDesignController.getDetails = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getDetails', p0, callback);
}
ArrayDesignController.getArrayDesigns = function(p0, p1, p2, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getArrayDesigns', p0, p1, p2, callback);
}
ArrayDesignController.getCsSummaries = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getCsSummaries', p0, callback);
}
ArrayDesignController.loadArrayDesignsSummary = function(callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'loadArrayDesignsSummary', callback);
}
ArrayDesignController.updateReport = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'updateReport', p0, callback);
}
ArrayDesignController.updateReportById = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'updateReportById', p0, callback);
}
ArrayDesignController.addAlternateName = function(p0, p1, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'addAlternateName', p0, p1, callback);
}
ArrayDesignController.getReportHtml = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getReportHtml', p0, callback);
}
ArrayDesignController.getSummaryForArrayDesign = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'getSummaryForArrayDesign', p0, callback);
}
ArrayDesignController.loadArrayDesignsForShowAll = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'loadArrayDesignsForShowAll', p0, callback);
}
ArrayDesignController.remove = function(p0, callback) {
  dwr.engine._execute(ArrayDesignController._path, 'ArrayDesignController', 'remove', p0, callback);
}
window.ArrayDesignController = ArrayDesignController
