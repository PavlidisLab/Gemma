/**
 * Monitor hibernate, caches, etc.
 * 
 * @version $Id$
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

function clearAllCaches() {
    Ext.Msg.show({
                title : 'Are you sure?',
                msg : 'Clear all caches?',
                buttons : Ext.Msg.YESNO,
                fn : processClearAllCachesResult,
                animEl : 'cacheStats',
                icon : Ext.MessageBox.QUESTION
            });
}

function clearCache(name) {
    Ext.Msg.show({
                title : 'Are you sure?',
                msg : 'Clear ' + name + ' cache?',
                buttons : Ext.Msg.YESNO,
                fn : processClearCacheResult,
                animEl : 'cacheStats',
                icon : Ext.MessageBox.QUESTION,
                cacheName : name
            });
}

function enableStatistics() {
    Ext.Msg.show({
                title : 'Are you sure?',
                msg : 'Enable stats collection',
                buttons : Ext.Msg.YESNO,
                fn : processEnableStats,
                animEl : 'cacheStats',
                icon : Ext.MessageBox.QUESTION
            });
}

function disableStatistics() {
    Ext.Msg.show({
                title : 'Are you sure?',
                msg : 'Disable stats collection',
                buttons : Ext.Msg.YESNO,
                fn : processDisableStats,
                animEl : 'cacheStats',
                icon : Ext.MessageBox.QUESTION
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

function processClearCacheResult(btn, text, opt) {
    if (btn == 'yes') {
        SystemMonitorController.clearCache(opt.cacheName);
    }
}

function processClearAllCachesResult(btn, text, opt) {
    if (btn == 'yes') {
        SystemMonitorController.clearAllCaches();
    }
}

function processResetHibernateStats(btn, text, opt) {
    if (btn == 'yes') {
        SystemMonitorController.resetHibernateStatus();
        SystemMonitorController.getHibernateStatus(handleSuccess);
    }
}
function processDisableStats(btn, text, opt) {
    if (btn == 'yes') {
        SystemMonitorController.disableStatistics();
    }
}
function processEnableStats(btn, text, opt) {
    if (btn == 'yes') {
        SystemMonitorController.enableStatistics();

    }
}
