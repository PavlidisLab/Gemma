/* this code is generated, see generate-dwr-client.py for details */
var GeoRecordBrowserController = {};
GeoRecordBrowserController._path = '/dwr';
GeoRecordBrowserController.browse = function(p0, p1, p2, callback) {
  dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'browse', p0, p1, p2, callback);
}
GeoRecordBrowserController.getDetails = function(p0, callback) {
  dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'getDetails', p0, callback);
}
GeoRecordBrowserController.toggleUsability = function(p0, callback) {
  dwr.engine._execute(GeoRecordBrowserController._path, 'GeoRecordBrowserController', 'toggleUsability', p0, callback);
}
window.GeoRecordBrowserController = GeoRecordBrowserController;
export default GeoRecordBrowserController;
