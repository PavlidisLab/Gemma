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

function flushAllCaches() {
	Ext.Msg.show({
				title : 'Are you sure?',
				msg : 'Flush all caches?',
				buttons : Ext.Msg.YESNO,
				fn : processFlushAllCachesResult,
				animEl : 'cacheStats',
				icon : Ext.MessageBox.QUESTION
			});
}

function flushCache(name) {
	Ext.Msg.show({
				title : 'Are you sure?',
				msg : 'Flush ' + name + ' cache?',
				buttons : Ext.Msg.YESNO,
				fn : processFlushCacheResult,
				animEl : 'cacheStats',
				icon : Ext.MessageBox.QUESTION,
				cacheName : name
			});
}

function processFlushCacheResult(btn, text, opt) {
	if (btn == 'yes') {
		HibernateMonitorController.flushCache(opt.cacheName);
	}
}

function processFlushAllCachesResult(btn, text, opt) {
	if (btn == 'yes') {
		HibernateMonitorController.flushAllCaches();
	}
}