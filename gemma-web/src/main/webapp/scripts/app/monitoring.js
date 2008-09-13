/**
 * Monitor hibernate
 */
Ext.onReady(function() {
	window.setInterval(task, 5000);
});

function task() {
	HibernateMonitorController.getHibernateStatus(handleSuccess);
	HibernateMonitorController.getCacheStatus(handleCacheData);
}

function handleSuccess(data) {
	Ext.DomHelper.overwrite("hibernateStats", data);
}

function handleCacheData(data) {
	Ext.DomHelper.overwrite("cacheStats", data);
}
