/**
 * Monitor hibernate, caches, etc.
 */
Ext.onReady(function() {

			var pauseBx = new Ext.form.Checkbox({
						id : 'cache-pause-stats-checkbox',
						renderTo : 'cache-pause-stats-checkbox-div',
						boxLabel : 'Pause updates'
					});

			// fire once immediately.
			task();
			window.setInterval(task, 10000);
         
			function task() {
				SystemMonitorController.getHibernateStatus(handleSuccess);

				if (!pauseBx.getValue()) {
					SystemMonitorController.getCacheStatus(handleCacheData);
				}

				SystemMonitorController.getSpaceStatus(handleSpaceStatus);
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
});
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

			function resetHibernateStats() {
				Ext.Msg.show({
							title : 'Are you sure?',
							msg : 'Reset Hibernate stats?',
							buttons : Ext.Msg.YESNO,
							fn : processResetHibernateStats,
							animEl : 'hibstat-reset-button',
							icon : Ext.MessageBox.QUESTION,
							cacheName : name
						});
			}

			function processFlushCacheResult(btn, text, opt) {
				if (btn == 'yes') {
					SystemMonitorController.flushCache(opt.cacheName);
				}
			}

			function processFlushAllCachesResult(btn, text, opt) {
				if (btn == 'yes') {
					SystemMonitorController.flushAllCaches();
				}
			}

			function processResetHibernateStats(btn, text, opt) {
				if (btn == 'yes') {
					SystemMonitorController.resetHibernateStatus();
					SystemMonitorController.getHibernateStatus(handleSuccess);
				}
			}

