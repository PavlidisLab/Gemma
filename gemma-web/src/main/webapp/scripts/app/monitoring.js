/**
 * Monitor hibernate
 */
Ext.onReady(function() {
	var results = HibernateMonitorController.getHibernateStatus(handleSuccess);
});

function handleSuccess(data) {
	Ext.DomHelper.overwrite("hibernateStats", data);
}
