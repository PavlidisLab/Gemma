/**
 * Monitor hibernate
 */
Ext.onReady(function() {
			// fire once immediately.
			task();
			window.setInterval(task, 10000);
		});

function task() {
	HibernateMonitorController.getHibernateStatus(handleSuccess);
	HibernateMonitorController.getCacheStatus(handleCacheData);
	HibernateMonitorController.getSpaceStatus(handleSpaceStatus);
}

function handleSuccess(data) {
	Ext.DomHelper.overwrite("hibernateStats", data);
}

function handleCacheData(data) {
	Ext.DomHelper.overwrite("cacheStats", data);
}

function handleSpaceStatus(data) {
	Ext.DomHelper.overwrite("spaceStats", data);
}
