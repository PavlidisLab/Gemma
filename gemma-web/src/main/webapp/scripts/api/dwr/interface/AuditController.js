/* this code is generated, see generate-dwr-client.sh for details */
var AuditController = {};
AuditController._path = '/dwr';
AuditController.getEvents = function(p0, callback) {
  dwr.engine._execute(AuditController._path, 'AuditController', 'getEvents', p0, callback);
}
AuditController.addAuditEvent = function(p0, p1, p2, p3, callback) {
  dwr.engine._execute(AuditController._path, 'AuditController', 'addAuditEvent', p0, p1, p2, p3, callback);
}
window.AuditController = AuditController
