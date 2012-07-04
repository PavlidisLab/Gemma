Ext.namespace('Gemma');

Gemma.DEFAULT_NUMBER_EXPERIMENTS = 50;

Gemma.MyDatasetsPanel = Ext.extend(Ext.Panel, {
    layout : 'border',
    initComponent : function() {

        Gemma.MyDatasetsPanel.superclass.initComponent.call(this);

        var detailsmask = null;

        var showEEDetails = function(model, rowindex, record) {

            if (detailsmask == null) {
                detailsmask = new Ext.LoadMask(dataSetDetailsPanel.body, {
                            msg : "Loading details ..."
                        });
            }

            detailsmask.show();
            ExpressionExperimentController.getDescription(record.id, {
                callback : function(data) {
                    Ext.DomHelper
                            .overwrite(
                                    dataSetDetailsPanel.body,
                                    '<span class="big">'
                                            + Gemma.EEReportGridColumnRenderers.shortNameRenderer(record
                                                            .get('shortName'), null, record)
                                            + '</span>&nbsp;&nbsp;<span class="medium">'
                                            + record.get('name')
                                            + "</span><p>"
                                            + data
                                            + "</p>"
                                            + '<span class="link" onClick="Ext.getCmp(\'eemanager\').showAuditWindow('
                                            + record.id
                                            + ');" ><img ext:qtip="Show history" src="/Gemma/images/icons/pencil.png" /></span>');
                    detailsmask.hide();
                }.createDelegate(this)
            });

        };

        var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
                '<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
                    log : function(id) {
                        // console.log(id);
                    }
                });

        // if the user is an admin, show the "refresh all" button
        var isAdmin = (Ext.get('hasAdmin')) ? Ext.get('hasAdmin').getValue() : false;

        /*
         * If the URL contains a list of IDs, limit ourselves to that.
         */
        var limit = Gemma.DEFAULT_NUMBER_EXPERIMENTS;
        var queryStart = document.URL.indexOf("?");
        var ids = null;
        var taxonid = null;
        var filterMode = null;
        var showPublic = true;// !isAdmin;
        if (queryStart > -1) {
            var urlParams = Ext.urlDecode(document.URL.substr(queryStart + 1));
            ids = urlParams.ids ? urlParams.ids.split(',') : null;
            taxonid = urlParams.taxon ? urlParams.taxon : null;
            limit = urlParams.taxon ? urlParams.limit : Gemma.DEFAULT_NUMBER_EXPERIMENTS;
            filterMode = urlParams.filter ? urlParams.filter : null;
            showPublic = urlParams.showPublic ? urlParams.showPublic : showPublic;
        }

        var reportGrid = new Gemma.EEReportGrid({
                    region : 'center',
                    taxonid : taxonid,
                    limit : limit,
                    filterType : filterMode,
                    ids : ids,
                    showPublic : showPublic
                });

        reportGrid.getSelectionModel().on('rowselect', showEEDetails, this, {
            buffer : 100
                // keep from firing too many times at once
            });

        var dataSetDetailsPanel = new Ext.Panel({
                    id : 'dataSetDetailsPanel',
                    region : 'south',
                    split : true,
                    bodyStyle : 'padding:8px',
                    height : 200,
                    autoScroll : true
                });

        this.refreshAllLink = (isAdmin)
                ? '<span style="font-weight:normal"> &nbsp;&nbsp | &nbsp;&nbsp; To update all reports click '
                        + '<span class="link" onClick="Ext.getCmp(\'eemanager\').updateAllEEReports(1)"> here </span></span>'
                : '';

        // only allow admins to mess with batch (in any obvious way)
        var isAdmin = (Ext.getDom('hasAdmin')) ? Ext.getDom('hasAdmin').getValue() : false;
        var c = reportGrid.getColumnModel().findColumnIndex('dateBatchFetch');
        reportGrid.getColumnModel().setHidden(c, !isAdmin);

        this.add([reportGrid, dataSetDetailsPanel]);

    }
});

/**
 * 
 * @class Gemma.EEReportGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.EEReportGrid = Ext.extend(Ext.grid.GridPanel, {

    viewConfig : {
        autoFill : true,
        forceFit : true
    },

    searchForText : function(button, keyev) {
        var text = this.searchInGridField.getValue();
        if (text.length < 2) {
            this.clearFilter();
            return;
        }
        this.getStore().filterBy(this.getSearchFun(text), this, 0);
    },

    showText : function() {
        var string = "";
        this.getStore().each(function(r) {
                    string += r.get('shortName') + "\n";
                });

        var w = new Ext.Window({
                    modal : true,
                    title : "You can copy this text",
                    html : string,
                    height : 400,
                    width : 200,
                    autoScroll : true,
                    bodyCfg : {
                        tag : 'textarea',
                        style : 'background-color : white;font-size:smaller'
                    }
                });
        w.show();

    },

    clearFilter : function() {
        this.getStore().clearFilter();
    },

    getSearchFun : function(text) {
        var value = new RegExp(Ext.escapeRe(text), 'i');
        return function(r, id) {
            var obj = r.data;
            return value.match(obj.name) || value.match(obj.shortName);
        }
    },

    refresh : function() {
        this.getTopToolbar().refresh();
    },

    updateTitle : function(count) {
        this.setTitle('Experiment Manager &nbsp;&nbsp; ( ' + count + ((count == 1) ? ' row' : ' rows') + ' )');
    },

    initComponent : function() {

        var manager = new Gemma.EEManager({
                    editable : true,
                    id : 'eemanager'
                });

        this.manager = manager;

        var limit = (this.limit) ? this.limit : Gemma.DEFAULT_NUMBER_EXPERIMENTS;
        var ids = (this.ids) ? this.ids : null;
        var taxonid = (this.taxonid) ? this.taxonid : null;
        var filterMode = (this.filterMode) ? this.filterMode : null;
        var showPublic = (this.showPublic) ? this.showPublic : false;

        var store = new Gemma.PagingDataStore({
                    autoLoad : true,
                    proxy : new Ext.data.DWRProxy({
                                apiActionToHandlerMap : {
                                    read : {
                                        dwrFunction : ExpressionExperimentController.loadStatusSummaries,
                                        getDwrArgsFunction : function(request, recordDataArray) {
                                            if (request.options.params && request.options.params instanceof Array) {
                                                return request.options.params;
                                            }
                                            return [taxonid, ids, limit, filterMode, showPublic];
                                        }
                                    }
                                }
                            }),
                    reader : new Ext.data.ListRangeReader({
                                id : "id"
                            }, manager.record),
                    remoteSort : false,
                    sortInfo : {
                        field : 'dateLastUpdated',
                        direction : 'DESC'
                    },
                    sort : function(fieldName, dir) {
                        store.fireEvent('beforesort');
                        /*
                         * Sorting this table is slooow. We need to pause to allow time for the loadmask to display.
                         */
                        var t = new Ext.util.DelayedTask(function() {
                                    Gemma.PagingDataStore.superclass.sort.call(store, fieldName, dir);
                                    store.fireEvent('aftersort');
                                });
                        t.delay(100);

                    }
                });

        store.on('load', function(store, records, options) {
                    this.updateTitle(records.size());
                }, this);

        Ext.apply(this, {
                    header : true,
                    store : store,
                    loadMask : true,
                    height : 500,
                    cm : Gemma.EEReportGridColumnModel
                });

        store.addEvents({
                    'beforesort' : true,
                    'aftersort' : true
                });

        manager.on('done', function() {
                    store.reload();
                }, this);

        manager.on('tagsUpdated', function() {
                    store.reload();
                }, this);

        var detailsmask = null;

        var showEEDetails = function(model, rowindex, record) {

            if (detailsmask == null) {
                detailsmask = new Ext.LoadMask(dataSetDetailsPanel.body, {
                            msg : "Loading details ..."
                        });
            }

            detailsmask.show();
            ExpressionExperimentController.getDescription(record.id, {
                callback : function(data) {
                    Ext.DomHelper
                            .overwrite(
                                    dataSetDetailsPanel.body,
                                    '<span class="big">'
                                            + Gemma.EEReportGridColumnRenderers.shortNameRenderer(record
                                                            .get('shortName'), null, record)
                                            + '</span>&nbsp;&nbsp;<span class="medium">'
                                            + record.get('name')
                                            + "</span><p>"
                                            + data
                                            + "</p>"
                                            + '<span class="link" onClick="Ext.getCmp(\'eemanager\').showAuditWindow('
                                            + record.id
                                            + ');" ><img ext:qtip="Show history" src="/Gemma/images/icons/pencil.png" /></span>');
                    detailsmask.hide();
                }.createDelegate(this)
            });

        };

        var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
                '<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
                    log : function(id) {
                        // console.log(id);
                    }
                });

        // Gemma.EEReportGrid.superclass.initComponent.call(this);
        store.on('beforesort', function() {
                    this.loadMask.show();
                }, this);

        store.on('aftersort', function() {
                    this.loadMask.hide()
                }, this);

        // if the user is an admin, show the "refresh all" button
        var isAdmin = (Ext.get('hasAdmin')) ? Ext.get('hasAdmin').getValue() : false;

        this.refreshAllLink = (isAdmin)
                ? '<span style="font-weight:normal"> &nbsp;&nbsp | &nbsp;&nbsp; To update all reports click '
                        + '<span class="link" onClick="Ext.getCmp(\'eemanager\').updateAllEEReports(1)"> here </span></span>'
                : '';

        store.on("exception", function(scope, args, data, e) {
                    Ext.Msg.alert('There was an error', e + ".  \nPlease try again.");
                });

        var topToolbar = new Gemma.EEReportGridToolbar({
                    showPublic : showPublic,
                    listeners : {
                        'loadStore' : {
                            fn : function(paramArr) {
                                this.store.load({
                                            params : paramArr
                                        });
                            },
                            scope : this

                        },
                        'showAsText' : {
                            fn : function() {
                                this.showText();
                            },
                            scope : this
                        }
                    }

                });

        this.searchInGridField = new Ext.form.TextField({
                    enableKeyEvents : true,
                    emptyText : 'Search',
                    tooltip : "Text typed here will act as a filter.",
                    listeners : {
                        "keyup" : {
                            fn : this.searchForText.createDelegate(this),
                            scope : this,
                            options : {
                                delay : 100
                            }
                        }
                    }
                });

        Ext.apply(this, {
                    tbar : topToolbar,
                    bbar : new Ext.Toolbar({
                                items : ['->', {
                                            xtype : 'button',
                                            handler : this.clearFilter.createDelegate(this),
                                            tooltip : "Show all",
                                            scope : this,
                                            cls : 'x-btn-text',
                                            text : 'Reset filter'
                                        }, ' ', this.searchInGridField]
                            })

                });
        Gemma.EEReportGrid.superclass.initComponent.call(this);

    }
});

Gemma.EEReportGridColumnRenderers = {

    dateRenderer : new Ext.util.Format.dateRenderer("y/M/d"),

    adminRenderer : function(value, metadata, record, rowIndex, colIndex, store) {

        if (record.get("currentUserHasWritePermission")) {
            var adminLink = '<span class="link"  onClick="Ext.getCmp(\'eemanager\').updateEEReport('
                    + value
                    + ')"><img src="/Gemma/images/icons/arrow_refresh_small.png" ext:qtip="Refresh statistics"  ext:qtip="refresh"/></span>';

            var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
            if (isAdmin) {
                adminLink = adminLink
                        + '&nbsp;&nbsp;&nbsp;<span class="link" onClick="return Ext.getCmp(\'eemanager\').deleteExperiment('
                        + value
                        + ')"><img src="/Gemma/images/icons/cross.png" ext:qtip="Delete the experiment from the system" ext:qtip="delete" /></span>&nbsp;';
            }
            return adminLink;
        }
        return "(no permission)";

    },

    shortNameRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        return '<a href="/Gemma/expressionExperiment/showExpressionExperiment.html?id='
                + (record.get("sourceExperiment") ? record.get("sourceExperiment") : record.get("id"))
                + '" target="_blank">' + value + '</a>';
    },

    experimentalDesignEditRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var url = '<a target="_blank" href="/Gemma/experimentalDesign/showExperimentalDesign.html?eeid='
                + id
                + '"><img src="/Gemma/images/icons/pencil.png" alt="view/edit experimental design" ext:qtip="view/edit experimental design"/></a>';
        return value + '&nbsp;' + url;
    },

    experimentTaggerRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var taxonId = record.get('taxonId');

        var url = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').tagger(' + id + ',' + taxonId + ','
                + record.get("currentUserHasWritePermission") + ',' + (record.get("validatedAnnotations") !== null)
                + ')"><img src="/Gemma/images/icons/pencil.png" alt="view tags" ext:qtip="add/view tags"/></span>';
        value = value + '&nbsp;' + url;

        if (record.get("currentUserHasWritePermission")) {
            var turl;
            if (record.get('autoTagDate')) {
                var icon = "/Gemma/images/icons/wand.png";
                turl = '<span class="link"  onClick="return Ext.getCmp(\'eemanager\').autoTag(' + id + ')"><img src="'
                        + icon + '" alt="run auto-tagger" ext:qtip="tagger was run on '
                        + Ext.util.Format.date(record.get('autoTagDate'), 'y/M/d') + '; click to re-run"/></span>';
            } else {
                var icon = "/Gemma/images/icons/wand--plus.png";
                turl = '<span class="link"  onClick="return Ext.getCmp(\'eemanager\').autoTag(' + id + ')"><img src="'
                        + icon + '" alt="run auto-tagger" ext:qtip="add tags automatically"/></span>';
            }
            value = value + '&nbsp;' + turl;
        }

        return value;
    },

    linkAnalysisRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var runurl = "";

        var BIG_ENOUGH_FOR_LINKS = 7; // FIXME externalize this! And it should
        // be
        // based on biomaterials. and it should
        // come from the server side.

        if (record.get("currentUserHasWritePermission")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doLinks('
                    + id
                    + ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run coexpression analysis"  alt="link analysis" /></span>';
        }

        if (record.get('bioAssayCount') < BIG_ENOUGH_FOR_LINKS) {
            return '<span style="color:#CCC;">Too small</span>&nbsp;';
        }

        if (record.get('dateLinkAnalysis')) {
            var type = record.get('linkAnalysisEventType');
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedLinkAnalysisEventImpl') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            } else if (type == 'TooSmallDatasetLinkAnalysisEventImpl') {
                color = '#CCC';
                qtip = 'ext:qtip="Too small to perform link analysis"';
                suggestRun = false;
            }

            return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d') + '&nbsp;'
                    + (suggestRun ? runurl : '');
        } else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
    },

    pcaDateRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var runurl = "";

        if (record.get("currentUserHasWritePermission")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doPca('
                    + id
                    + ', '
                    + false
                    + ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run PCA analysis"  alt="PCA analysis" /></span>';
        }

        /*
         * FIXME logic can be more complex here, but it should probably be done on the server. Only offer the factor
         * analysis if there is batch information or ExperimentalFactors.
         */
        if (record.get('datePcaAnalysis')) {
            var type = record.get('pcaAnalysisEventType');
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedPCAAnalysisEventImpl') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            // pass in parameter indicating we already have the pca.
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doPca('
                    + id
                    + ', '
                    + true
                    + ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run PCA analysis"  alt="PCA analysis" /></span>';

            return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d') + '&nbsp;'
                    + (suggestRun ? runurl : '');
        } else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
    },

    batchDateRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var runurl = "";
        if (record.get("currentUserHasWritePermission")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doBatchInfoFetch('
                    + id
                    + ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run batch info fetch"  alt="Fetch batch information" /></span>';
        }

        /*
         * See bug 2626. If we have batch information, do not let us clobber it from here. FIXME hasBatchInformation is
         * not populated here.
         */
        var hasBatchInformation = record.get('hasBatchInformation');
        if (record.get('dateBatchFetch')) {
            var type = record.get('batchFetchEventType');
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedBatchInformationFetchingEventImpl') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            } else if (type == 'FailedBatchInformationMissingEventImpl') {
                if (hasBatchInformation) {
                    return '<span style="color:#000;">Provided</span>&nbsp;';
                } else {
                    color = '#CCC';
                    qtip = 'ext:qtip="Raw data files not available from source"';
                    suggestRun = false;
                }
            }

            return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d') + '&nbsp;'
                    + (suggestRun ? runurl : '');
        } else if (!hasBatchInformation) {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        } else {
            return '<span style="color:#000;">Provided</span>&nbsp;';
        }
    },

    missingValueAnalysisRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');

        var runurl = "";
        if (record.get("currentUserHasWritePermission")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doMissingValues('
                    + id
                    + ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run missing value analysis" alt="missing value computation"  /></span>';
        }

        /*
         * Offer missing value analysis if it's possible (this might need tweaking).
         */
        if (record.get('technologyType') != 'ONECOLOR' && record.get('hasEitherIntensity')) {
            if (record.get('dateMissingValueAnalysis')) {
                var type = record.get('missingValueAnalysisEventType');
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedMissingValueAnalysisEventImpl') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }

                return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
                        + '&nbsp;' + (suggestRun ? runurl : '');
            } else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }

        } else {
            return '<span style="color:#CCF;" ext:qtip="Only relevant for two-channel microarray studies with intensity data available.">NA</span>';
        }
    },

    processedVectorCreateRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var runurl = "";
        if (record.get("currentUserHasWritePermission")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doProcessedVectors('
                    + id
                    + ')"><img src="/Gemma/images/icons/control_play_blue.png" ext:qtip="Run processed vector generation" alt="processed vector generation"/></span>';
        }

        if (record.get('dateProcessedDataVectorComputation')) {
            var type = record.get('processedDataVectorComputationEventType');
            var color = "#000";

            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedProcessedVectorComputationEventImpl') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }

            return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d') + '&nbsp;'
                    + (suggestRun ? runurl : '');
        } else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
    },

    differentialAnalysisRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');

        var diffIsPossible = function(record) {
            return record.get("numPopulatedFactors") > 0 && record.get("currentUserHasWritePermission");
        };

        var runurl = "";
        if (record.get("currentUserHasWritePermission")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doDifferential('
                    + id
                    + ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" ext:qtip="Run differential expression analysis"/></span>';
        }

        if (diffIsPossible(record)) {

            if (record.get('dateDifferentialAnalysis')) {
                var type = record.get('differentialAnalysisEventType');

                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';

                if (type == 'FailedDifferentialExpressionAnalysisEventImpl') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                } else if (record.get('differentialExpressionAnalyses').length == 0) {
                    // we ran it, but the analyses were apparently deleted.
                    return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
                }

                // TODO: add tooltip describing the analysis.
                return '<span style="color:' + color + ';" ' + qtip + '>' + Ext.util.Format.date(value, 'y/M/d')
                        + '&nbsp;' + (suggestRun ? runurl : '');
            } else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
        } else {
            return '<span style="color:#CCF;" ext:qtip="You must create at least one experimental factor to enable this analysis.">NA</span>';
        }
    },

    flagRenderer : function(value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var result = '';
        if (record.get('validated')) {
            result = result
                    + '<img src="/Gemma/images/icons/emoticon_smile.png" alt="validated" ext:qtip="validated"/>';
        }

        if (record.get('troubled')) {
            result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ext:qtip="trouble: '
                    + record.get('troubleDetails') + '"/>';
        }

        result = result
                + Gemma.SecurityManager.getSecurityLink(
                        'ubic.gemma.model.expression.experiment.ExpressionExperimentImpl', id, record.get('isPublic'),
                        record.get('isShared'), record.get('currentUserHasWritePermission'),null, null,null, record.get('currentUserIsOwner'));

        return result;

    }
};

Gemma.EEReportGridColumnModel = new Ext.grid.ColumnModel({
    columns : [{
                header : 'Short Name',
                sortable : true,
                dataIndex : 'shortName',
                renderer : Gemma.EEReportGridColumnRenderers.shortNameRenderer
            }, {
                header : 'Name',
                sortable : true,
                dataIndex : 'name'
            }, {
                header : 'Taxon',
                sortable : true,
                dataIndex : 'taxon',
                width : 40
            }, {
                header : 'Flags',
                sortable : true,
                renderer : Gemma.EEReportGridColumnRenderers.flagRenderer,
                tooltip : 'Status flags',
                width : 40
            }, {
                header : '#ADs',
                sortable : true,
                dataIndex : 'arrayDesignCount',
                tooltip : "The number of different platforms used in the study",
                width : 35
            }, {
                header : '#BAs',
                sortable : true,
                dataIndex : 'bioAssayCount',
                tooltip : 'The number of samples in the study',
                width : 35
            }, {
                header : '#Prof',
                sortable : true,
                dataIndex : 'processedExpressionVectorCount',
                tooltip : 'The number of expression profiles',
                width : 45
            }, {
                header : '#Facs',
                sortable : true,
                dataIndex : 'numPopulatedFactors',
                renderer : Gemma.EEReportGridColumnRenderers.experimentalDesignEditRenderer,
                tooltip : 'The number of experimental factors (variables) defined for the study, excluding any batch factors',
                width : 45
            }, {
                header : '#tags',
                sortable : true,
                dataIndex : 'numAnnotations',
                renderer : Gemma.EEReportGridColumnRenderers.experimentTaggerRenderer,
                tooltip : 'The number of terms the experiment is tagged with',
                width : 60
            }, {
                header : 'Created',
                sortable : true,
                dataIndex : 'dateCreated',
                tooltip : 'Create date',
                renderer : Gemma.EEReportGridColumnRenderers.dateRenderer,
                width : 80
            }, {
                header : 'Updated',
                sortable : true,
                dataIndex : 'dateLastUpdated',
                tooltip : 'Update date; not all possible types of updates are considered.',
                renderer : Gemma.EEReportGridColumnRenderers.dateRenderer,
                width : 80
            }, {
                header : 'MissingVals',
                sortable : true,
                dataIndex : 'dateMissingValueAnalysis',
                tooltip : 'Status of missing value computation (two-channel studies only)',
                renderer : Gemma.EEReportGridColumnRenderers.missingValueAnalysisRenderer,
                width : 80
            }, {
                header : 'BatchInfo',
                sortable : true,
                dataIndex : 'dateBatchFetch',
                tooltip : 'Status of batch information',
                renderer : Gemma.EEReportGridColumnRenderers.batchDateRenderer,
                width : 90
            }, {
                header : 'ProcProf',
                sortable : true,
                dataIndex : 'dateProcessedDataVectorComputation',
                tooltip : 'Status of processed expression profile configuration',
                renderer : Gemma.EEReportGridColumnRenderers.processedVectorCreateRenderer,
                width : 80
            }, {
                header : 'Diff',
                sortable : true,
                dataIndex : 'dateDifferentialAnalysis',
                tooltip : 'Status of differential expression analysis. Must have factors to enable',
                renderer : Gemma.EEReportGridColumnRenderers.differentialAnalysisRenderer,
                width : 90
            }, {
                header : 'Links',
                sortable : true,
                dataIndex : 'dateLinkAnalysis',
                tooltip : 'Status of coexpression analysis',
                renderer : Gemma.EEReportGridColumnRenderers.linkAnalysisRenderer,
                width : 90
            }, {
                header : 'PCA',
                sortable : true,
                dataIndex : 'datePcaAnalysis',
                tooltip : 'Status of PCA analysis',
                renderer : Gemma.EEReportGridColumnRenderers.pcaDateRenderer,
                width : 90
            }, {
                header : 'Admin',
                sortable : false,
                dataIndex : 'id',
                renderer : Gemma.EEReportGridColumnRenderers.adminRenderer,
                width : 60
            }]

});

Gemma.EEReportGridToolbar = Ext.extend(Ext.Toolbar, {

    getSearchFun : function(text) {
        var value = new RegExp(Ext.escapeRe(text), 'i');
        return function(r, id) {
            var obj = r.data;
            return value.match(obj.name) || value.match(obj.shortName);
        }
    },

    refresh : function() {
        this.setFiltersToDefault();
        this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);
    },

    filterType : null,

    filterByNeed : function(box, record, index) {
        this.filterType = record.get('filterType');
        this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);
    },

    filterByTaxon : function(box, record, index) {

        // if user selected 'All taxa', load all datasets
        if (record.get('commonName') == "All taxa") {
            this.taxonid = null;
        } else {
            this.taxonid = record.get('id')
        }

        this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);

    },

    filterByLimit : function(box, record, index) {
        this.limit = record.get('count');
        // can't do "50 recently updated" and search results
        // this.searchCombo.clearValue();

        this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);
    },

    filterBySearch : function(box, record, index) {
        this.ids = record.get('memberIds');
        // if user selected an experiment instead of an experiment group, member
        // ids will be null
        if (this.ids === null) {
            this.ids = [record.get('id')];
        }
        // can't do "50 recently updated" and search results
        // this.limitCombo.clearValue();

        this.fireEvent('loadStore', [this.taxonid, this.ids, Gemma.DEFAULT_NUMBER_EXPERIMENTS, this.filterType,
                        this.showPublic]);
    },

    getBookmark : function() {
        var url = Gemma.BASEURL + "/expressionExperiment/showAllExpressionExperimentLinkSummaries.html?";
        if (this.ids) {
            url += "&ids=" + this.ids.join(",");
        }
        if (this.taxonid) {
            url += "&taxon=" + this.taxonid;
        }
        if (this.limit) {
            url += "&limit=" + this.limit;
        }
        if (this.filterType) {
            url += "&filter=" + this.filterType;
        }
        if (this.showPublic) {
            url += "&showPublic=" + this.showPublic;
        }

        Ext.Msg.alert("Your link to this list", '<a style="background-color:white" href="' + url + '">' + url + '</a>');
    },

    initComponent : function() {

        this.filterCombo = new Ext.form.ComboBox({
                    typeAhead : true,
                    triggerAction : 'all',
                    lazyRender : true,
                    mode : 'local',
                    defaultValue : '0',
                    emptyText : "Filter by property",
                    store : new Ext.data.ArrayStore({
                                id : 0,
                                fields : ['filterType', 'displayText'],
                                data : [[0, 'No filter'], [1, 'Need diff. expression analysis'],
                                        [2, 'Need coexpression analysis'], [3, 'Has diff. expression analysis'],
                                        [4, 'Has coexpression analysis'], [5, 'Troubled'], [6, 'No factors'],
                                        [7, 'No tags'], [8, 'Needs batch info'], [9, 'Has batch info'],
                                        [10, 'Needs PCA'], [11, 'Has PCA']]
                            }),
                    valueField : 'filterType',
                    displayField : 'displayText',
                    listeners : {
                        scope : this,
                        'select' : this.filterByNeed
                    }

                });

        this.taxonCombo = new Gemma.TaxonCombo({
                    isDisplayTaxonWithDatasets : true,
                    stateId : null, // don't remember taxon value if
                    // user navigates away then comes
                    // back
                    emptyText : "Filter by taxon",
                    allTaxa : true, // want an 'All taxa' option
                    listeners : {
                        scope : this,
                        'select' : this.filterByTaxon
                    }
                });

        this.limitCombo = new Ext.form.ComboBox({
                    typeAhead : true,
                    width : 150,
                    triggerAction : 'all',
                    lazyRender : true,
                    mode : 'local',
                    defaultValue : '50',
                    emptyText : "Number to display",
                    store : new Ext.data.ArrayStore({
                                id : 0,
                                fields : ['count', 'displayText'],
                                data : [[50, '50 recently updated'], [100, '100 recently updated'],
                                        [200, '200 recently updated'], [300, '300 recently updated'],
                                        [500, '500 recently updated'], [-50, '50 oldest updates'],
                                        [-100, '100 oldest updates'], [-200, '200 oldest updates'],
                                        [-300, '300 oldest updates'], [-500, '500 oldest updates']]
                            }),
                    valueField : 'count',
                    displayField : 'displayText',
                    listeners : {
                        scope : this,
                        'select' : this.filterByLimit
                    }

                });

        this.searchCombo = new Gemma.ExperimentAndExperimentGroupCombo({
                    width : 220,
                    emptyText : "Search for experiments",
                    listeners : {
                        scope : this,
                        'select' : this.filterBySearch
                    }
                });

        // hacky fix for reference needed to "this" in applyState
        var toolbarScope = this;
        this.showPublicCheck = new Ext.form.Checkbox({
                    tooltip : "Show/hide your public data sets.",
                    boxLabel : "Show your public data",
                    checked : this.showPublic,
                    handler : function(checkbox, event) {
                        this.showPublic = checkbox.getValue();
                        this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType,
                                        this.showPublic]);
                    },
                    scope : this
                });

        // set combos to initial values
        this.on('afterrender', function() {
                    this.taxonCombo.getStore().on('doneLoading', function() {
                        if (this.taxonid) {
                            this.taxonCombo.setValue(this.taxonid);
                        } else {
                            this.taxonid = (this.taxonCombo.getStore().getAt(0)) ? this.taxonCombo.getStore().getAt(0)
                                    .get('id') : "-1";
                            this.taxonCombo.setValue(this.taxonid);
                        }
                    }, this);

                    if (this.limit) {
                        this.limitCombo.setValue(this.limit);
                    } else {
                        this.limitCombo.setValue(this.limitCombo.defaultValue);
                    }
                    if (this.filterType) {
                        this.filterCombo.setValue(this.filterType);
                    } else {
                        this.filterCombo.setValue(this.filterCombo.defaultValue);
                    }
                }, this);

        // only call this after everything has loaded! otherwise will break
        this.setFiltersToDefault = function() {

            this.taxonid = (this.taxonCombo.getStore().getAt(0)) ? this.taxonCombo.getStore().getAt(0).get('id') : "-1";
            this.taxonCombo.setValue(this.taxonid);

            this.limit = this.limitCombo.defaultValue;
            this.limitCombo.setValue(this.limit);

            this.filteryType = this.filterCombo.defaultValue;
            this.filterCombo.setValue(this.filterCombo.defaultValue);

            this.searchCombo.clearValue();
            this.ids = null;

        };

        Ext.apply(this, {
                    items : [this.searchCombo, this.filterCombo, this.taxonCombo, this.limitCombo, {
                                xtype : 'button',
                                minWidth : 20,
                                cls : 'x-btn-icon',
                                icon : '/Gemma/images/icons/cross.png',
                                handler : this.refresh,
                                tooltip : "Clear filters",
                                scope : this
                            }, '->', this.showPublicCheck, {
                                xtype : 'button',
                                minWidth : 20,
                                cls : 'x-btn-icon',
                                icon : '/Gemma/images/icons/link.png',
                                handler : this.getBookmark,
                                tooltip : "Bookmarkable link",
                                scope : this
                            }, {
                                xtype : 'button',
                                minWidth : 20,
                                cls : 'x-btn-icon',
                                icon : '/Gemma/images/icons/disk.png',
                                handler : function() {
                                    this.fireEvent('showAsText', []);
                                }.createDelegate(this),
                                tooltip : "Download as text",
                                scope : this
                            }]
                });
        Gemma.EEReportGridToolbar.superclass.initComponent.call(this);
    }
});