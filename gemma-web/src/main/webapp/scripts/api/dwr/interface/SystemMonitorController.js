/* this code is generated, see generate-dwr-client.sh for details */
var SystemMonitorController = {};
SystemMonitorController._path = '/dwr';
SystemMonitorController.clearCache = function(p0, callback) {
  dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'clearCache', p0, callback);
}
SystemMonitorController.enableStatistics = function(callback) {
  dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'enableStatistics', callback);
}
SystemMonitorController.clearAllCaches = function(callback) {
  dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'clearAllCaches', callback);
}
SystemMonitorController.disableStatistics = function(callback) {
  dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'disableStatistics', callback);
}
SystemMonitorController.getCacheStatus = function(callback) {
  dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'getCacheStatus', callback);
}
SystemMonitorController.getHibernateStatus = function(callback) {
  dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'getHibernateStatus', callback);
}
SystemMonitorController.resetHibernateStatus = function(callback) {
  dwr.engine._execute(SystemMonitorController._path, 'SystemMonitorController', 'resetHibernateStatus', callback);
}
window.SystemMonitorController = SystemMonitorController
