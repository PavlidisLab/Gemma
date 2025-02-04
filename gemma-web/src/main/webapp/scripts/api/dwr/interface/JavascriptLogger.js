/* this code is generated, see generate-dwr-client.py for details */
var JavascriptLogger = {};
JavascriptLogger._path = '/dwr';
JavascriptLogger.writeToFatalLog = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToFatalLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToDebugLog = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToDebugLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToErrorLog = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToErrorLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToLog = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToWarnLog = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToWarnLog', p0, p1, p2, p3, p4, callback);
}
JavascriptLogger.writeToInfoLog = function(p0, p1, p2, p3, p4, callback) {
  dwr.engine._execute(JavascriptLogger._path, 'JavascriptLogger', 'writeToInfoLog', p0, p1, p2, p3, p4, callback);
}
window.JavascriptLogger = JavascriptLogger;
export default JavascriptLogger;
