Ext.namespace('Gemma');

Gemma.DEFAULT_NUMBER_EXPERIMENTS = 50;

/**
 *
 * @type {MyDatasetsPanel}
 */
Gemma.MyDatasetsPanel = Ext.extend(Ext.Panel,
    {
        layout: 'border',

        /**
         * @memberOf Gemma.MyDatasetsPanel
         */
        initComponent: function () {

            Gemma.MyDatasetsPanel.superclass.initComponent.call(this);

            var detailsMask = null;

            var showEEDetails = function (model, rowIndex, record) {

                if (detailsMask === null) {
                    detailsMask = new Ext.LoadMask(dataSetDetailsPanel.body, {
                        msg: "Loading details ..."
                    });
                }

                detailsMask.show();
                ExpressionExperimentController.getDescription(record.id, {
                    callback: function (data) {
                        Ext.DomHelper.overwrite(dataSetDetailsPanel.body, '<span class="big">'
                            + Gemma.EEReportGridColumnRenderers.shortNameRenderer(record.get('shortName'), null, record)
                            + '</span>&nbsp;&nbsp;<span class="medium">' + record.get('name') + "</span><p>" + data
                            + "</p>" + '<span class="link" onClick="Ext.getCmp(\'eemanager\').showAuditWindow(' + record.id
                            + ');" ><img ext:qtip="Show history" src="' + Gemma.CONTEXT_PATH + '/images/icons/pencil.png" /></span>');
                        detailsMask.hide();
                    }.createDelegate(this)
                });
            };

            var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
                '<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
                    log: function (id) {
                        // console.log(id);
                    }
                });

            // If the user is an admin, show the "refresh all" button
            var isAdmin = (Ext.get('hasAdmin')) ? Ext.get('hasAdmin').getValue() : false;

            // If the URL contains a list of IDs, limit ourselves to that.
            var limit = Gemma.DEFAULT_NUMBER_EXPERIMENTS;
            var queryStart = document.URL.indexOf("?");
            var ids = null;
            var taxonid = null;
            var filterMode = null;
            var showPublic = true;
            if (queryStart > -1) {
                var urlParams = Ext.urlDecode(document.URL.substr(queryStart + 1));
                ids = urlParams.ids ? urlParams.ids.split(',') : null;
                taxonid = urlParams.taxon ? urlParams.taxon : null;
                limit = urlParams.taxon ? urlParams.limit : Gemma.DEFAULT_NUMBER_EXPERIMENTS;
                filterMode = urlParams.filter ? urlParams.filter : null;
                showPublic = urlParams.showPublic ? urlParams.showPublic : showPublic;
            }

            var reportGrid = new Gemma.EEReportGrid({
                region: 'center',
                taxonid: taxonid,
                limit: limit,
                filterType: filterMode,
                ids: ids,
                showPublic: showPublic
            });

            reportGrid.getSelectionModel().on('rowselect', showEEDetails, this, {
                buffer: 100
                // keep from firing too many times at once
            });

            var dataSetDetailsPanel = new Ext.Panel({
                id: 'dataSetDetailsPanel',
                region: 'south',
                split: true,
                bodyStyle: 'padding:8px',
                height: 200,
                autoScroll: true
            });

            // only allow admins to mess with batch (in any obvious way)
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
Gemma.EEReportGrid = Ext.extend(Ext.grid.GridPanel,
    {
        viewConfig: {
            autoFill: true,
            forceFit: true
        },

        filterMode: null,
        showPublic: true,

        /**
         * @memberOf Gemma.EEReportGrid
         */
        searchForText: function (button, keyev) {
            var text = this.searchInGridField.getValue();
            if (text.length < 2) {
                this.clearFilter();
                return;
            }
            this.getStore().filterBy(this.getSearchFun(text), this, 0);
        },

        showText: function () {
            var string = "";
            var sels = this.getSelectionModel().getSelections();

            // If no experiments are selected, show all names; otherwise show the names of selected experiments
            if (sels.length === 0) {
                this.getStore().each(function (r) {
                    string += r.get('shortName') + "\n";
                });
            } else {
                for (var i = 0; i < sels.length; i++) {
                    string += sels[i].get('shortName') + "\n";
                }
            }

            var w = new Ext.Window({
                modal: true,
                title: "You can copy this text",
                html: string,
                height: 400,
                width: 200,
                autoScroll: true,
                bodyCfg: {
                    tag: 'textarea',
                    style: 'background-color : white;font-size:smaller'
                }
            });
            w.show();
        },

        clearFilter: function () {
            this.getTopToolbar().setFiltersToDefault();
            this.getStore().clearFilter();
        },

        getSearchFun: function (text) {
            var value = new RegExp(Ext.escapeRe(text), 'i');
            return function (r, id) {
                var obj = r.data;
                return value.test(obj.name) || value.test(obj.shortName);
            };
        },

        refresh: function () {
            this.getTopToolbar().refresh();
        },

        updateTitle: function (count) {
            this.setTitle('Dataset Manager (' + count + ((count === 1) ? ' row' : ' rows') + ')');
        },

        initComponent: function () {

            var manager = new Gemma.EEManager({
                editable: true,
                id: 'eemanager'
            });

            this.manager = manager;

            var limit = (this.limit) ? this.limit : Gemma.DEFAULT_NUMBER_EXPERIMENTS;
            var ids = (this.ids) ? this.ids : null;
            var taxonid = (this.taxonid) ? this.taxonid : null;
            var filterMode = (this.filterMode) ? this.filterMode : null;
            var showPublic = (this.showPublic) ? this.showPublic : false;
            var store = new Gemma.PagingDataStore({
                autoLoad: true,
                proxy: new Ext.data.DWRProxy({
                    apiActionToHandlerMap: {
                        read: {
                            dwrFunction: ExpressionExperimentController.loadStatusSummaries,
                            getDwrArgsFunction: function (request, recordDataArray) {
                                if (request.options.params && request.options.params instanceof Array) {
                                    return request.options.params;
                                }
                                return [taxonid, ids, limit, filterMode, showPublic];
                            }
                        }
                    }
                }),
                reader: new Ext.data.ListRangeReader({
                    id: "id"
                }, manager.record),
                remoteSort: false,
                sortInfo: {
                    field: 'lastUpdated',
                    direction: 'DESC'
                },
                sort: function (fieldName, dir) {
                    store.fireEvent('beforesort');
                    /*
                     * Sorting this table is slooow. We need to pause to allow time for the loadmask to display.
                     */
                    var t = new Ext.util.DelayedTask(function () {
                        Gemma.PagingDataStore.superclass.sort.call(store, fieldName, dir);
                        store.fireEvent('aftersort');
                    });
                    t.delay(100);

                }
            });

            store.on('load', function (store, records, options) {
                this.updateTitle(records.length);
            }, this);

            Ext.apply(this, {
                header: true,
                store: store,
                loadMask: true,
                height: 500,
                cm: Gemma.EEReportGridColumnModel
            });

            store.addEvents({
                'beforesort': true,
                'aftersort': true
            });

            manager.on('task-completed', function () {
                store.reload();
            }, this);

            manager.on('task-failed', function () {
                store.reload();
            }, this);

            manager.on('tagsUpdated', function () {
                store.reload();
            }, this);

            var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
                '<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
                    log: function (id) {
                        // console.log(id);
                    }
                });

            store.on('beforesort', function () {
                this.loadMask.show();
            }, this);

            store.on('aftersort', function () {
                this.loadMask.hide();
            }, this);

            store.on("exception", function (scope, args, data, e) {
                Ext.Msg.alert('There was an error', e + ".  \nPlease try again.");
            });

            var topToolbar = new Gemma.EEReportGridToolbar({
                showPublic: showPublic,
                listeners: {
                    'loadStore': {
                        fn: function (paramArr) {
                            this.store.load({
                                params: paramArr
                            });
                        },
                        scope: this

                    },
                    'showAsText': {
                        fn: function () {
                            this.showText();
                        },
                        scope: this
                    }
                }

            });

            this.searchInGridField = new Ext.form.TextField({
                enableKeyEvents: true,
                emptyText: 'Search',
                tooltip: "Text typed here will act as a filter.",
                listeners: {
                    "keyup": {
                        fn: this.searchForText.createDelegate(this),
                        scope: this,
                        options: {
                            delay: 100
                        }
                    }
                }
            });

            Ext.apply(this, {
                tbar: topToolbar,
                bbar: new Ext.Toolbar({
                    items: ['->', {
                        xtype: 'button',
                        handler: this.clearFilter.createDelegate(this),
                        tooltip: "Show all",
                        scope: this,
                        cls: 'x-btn-text',
                        text: 'Reset filter'
                    }, ' ', this.searchInGridField]
                })

            });
            Gemma.EEReportGrid.superclass.initComponent.call(this);

        }
    });

Gemma.EEReportGridColumnRenderers = {

    adminRenderer: function (value, metadata, record, rowIndex, colIndex, store) {

        if (record.get("userCanWrite")) {
            var adminLink = '<span class="link"  onClick="Ext.getCmp(\'eemanager\').updateEEReport('
                + value
                + ')"><i class="green fa fa-refresh fa-lg fa-fw" ext:qtip="Refresh statistics"></i></span>';

            var isAdmin = (Ext.get('hasAdmin')) ? Ext.get('hasAdmin').getValue() : false;
            if (isAdmin) {
                adminLink = adminLink
                    + '&nbsp;&nbsp;&nbsp;<span class="link" onClick="return Ext.getCmp(\'eemanager\').deleteExperiment('
                    + value
                    + ')"><i class="red fa fa-times fa-lg fa-fw" ext:qtip="Delete the experiment from the system"></i></span>&nbsp;';
            }
            return adminLink;
        }
        return "(no permission)";

    },

    shortNameRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        return '<a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id='
            + (record.get("sourceExperiment") ? record.get("sourceExperiment") : record.get("id"))
            + '" target="_blank">' + value + '</a>';
    },

    experimentalDesignEditRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var url = '<a target="_blank" href="' + Gemma.CONTEXT_PATH + '/experimentalDesign/showExperimentalDesign.html?eeid='
            + id
            + '"><i class="gray-blue fa fa-pencil fa-lg -fa-fw" ext:qtip="view/edit experimental design"></i></a>';
        return value + '&nbsp;' + url;
    },

    experimentTaggerRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var taxonId = record.get('taxonId');

        var url = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').tagger(' + id + ',' + taxonId + ','
            + record.get("userCanWrite")
            + ')"><i class="gray-blue fa fa-tags fa-lg -fa-fw" ext:qtip="add/view tags"></i></span>';
        value = value + '&nbsp;' + url;

        return value;
    },

    linkAnalysisRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var runurl = "";

        // FIXME externalize this! And it should be based on biomaterials. And it should come from the server side.
        var BIG_ENOUGH_FOR_LINKS = 7;

        if (record.get("userCanWrite")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doLinks(' + id + ')">' +
                '<i class="gray-blue fa fa-play-circle fa-lg fa-fw" ext:qtip="Run coexpression analysis"></i></span>';
        }

        if (record.get('bioAssayCount') < BIG_ENOUGH_FOR_LINKS) {
            return '<span style="color:#CCC;">Too small</span>&nbsp;';
        }

        if (record.get('dateLinkAnalysis')) {
            var type = record.get('linkAnalysisEventType');
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type === 'FailedLinkAnalysisEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            } else if (type === 'TooSmallDatasetLinkAnalysisEvent') {
                color = '#CCC';
                qtip = 'ext:qtip="Too small to perform link analysis"';
                suggestRun = false;
            }

            return '<span style="color:' + color + ';" ' + qtip + '>' + (suggestRun ? runurl : '')
                + Gemma.Renderers.dateRenderer(value) + '&nbsp;';
        } else {
            return '<span style="color:#3A3;">' + runurl + 'Needed</span>&nbsp;';
        }
    },

    diagnosticsRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var runurl = "";

        if (record.get("userCanWrite")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doDiagnostics('
                + id + ', ' + false + ')">' +
                '<i class="gray-blue fa fa-play-circle fa-lg fa-fw" ext:qtip="Update diagnostics"></i></span>';
        }

        /*
         * FIXME this date is just for PCA, so it might be misleading since the button is about all the diagnostics.
         */
        if (record.get('datePcaAnalysis')) {
            var type = record.get('pcaAnalysisEventType');
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type === 'FailedPCAAnalysisEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            // pass in parameter indicating we already have the pca.
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doDiagnostics('
                + id + ', ' + true + ')">' +
                '<i class="gray-blue fa fa-play-circle fa-lg fa-fw" ext:qtip="Run PCA analysis"></i></span>';

            return '<span style="color:' + color + ';" ' + qtip + '>' + (suggestRun ? runurl : '')
                + Gemma.Renderers.dateRenderer(value) + '&nbsp;';
        } else {
            return '<span style="color:#3A3;">' + runurl + 'Needed</span>&nbsp;';
        }
    },

    batchDateRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var dataSource = record.get('externalDatabase');
        var runurl = "";
        if (record.get("userCanWrite")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doBatchInfoFetch(' + id + ')">' +
                '<i class="gray-blue fa fa-play-circle fa-lg fa-fw" ext:qtip="Run batch info fetch"></i></span>';
        }

        // Batch info fetching not allowed for RNA seq and other non-microarray data
        if (record.get('technologyType') == 'NONE') {
            return '<span style="color:#CCF;" ext:qtip="Not microarray data">' + 'NA' + '</span>&nbsp;';
        }

        // If present, display the date and info. If batch information exists without date, display 'Provided'.
        // If no batch information, display 'Needed' with button for GEO and ArrayExpress data. Otherwise, NA.
        var hasBatchInformation = record.get('hasBatchInformation');
        if (record.get('dateBatchFetch')) {
            var type = record.get('batchFetchEventType');
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedBatchInformationFetchingEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            } else if (type == 'FailedBatchInformationMissingEvent') {
                if (hasBatchInformation) {
                    return '<span style="color:#000;">Provided</span>&nbsp;';
                } else {
                    color = '#CCF';
                    qtip = 'ext:qtip="Raw data files not available from source"';
                    suggestRun = false;
                }
            }

            return '<span style="color:' + color + ';" ' + qtip + '>' + (suggestRun ? runurl : '')
                + Gemma.Renderers.dateRenderer(value) + '&nbsp;';
        } else if (hasBatchInformation) {
            return '<span style="color:#000;">Provided</span>&nbsp;';
        } else if (dataSource == 'GEO' || dataSource == 'ArrayExpress') {
            return '<span style="color:#3A3;">' + runurl + 'Needed</span>&nbsp;';
        } else
            return '<span style="color:#CCF;" '
                + 'ext:qtip="Add batch information by creating a \'batch\' experiment factor">' + 'NA' + '</span>&nbsp;';
    },

    missingValueAnalysisRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');

        var runurl = "";
        if (record.get("userCanWrite")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doMissingValues(' + id + ')">' +
                '<i class="gray-blue fa fa-play-circle fa-lg fa-fw" ext:qtip="Run missing value analysis"></i></span>';
        }

        //Offer missing value analysis if it's possible (this might need tweaking).
        if (record.get('technologyType') != 'ONECOLOR' && record.get('technologyType') != 'NONE'
            && record.get('hasEitherIntensity')) {
            if (record.get('dateMissingValueAnalysis')) {
                var type = record.get('missingValueAnalysisEventType');
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedMissingValueAnalysisEvent') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }

                return '<span style="color:' + color + ';" ' + qtip + '>' + (suggestRun ? runurl : '')
                    + Gemma.Renderers.dateRenderer(value) + '&nbsp;';
            } else {
                return '<span style="color:#3A3;">' + runurl + 'Needed</span>&nbsp;';
            }

        } else {
            return '<span style="color:#CCF;" ext:qtip="Only relevant for two-channel microarray studies with intensity data available.">NA</span>';
        }
    },

    /* this also updates diagnostics (PCA etc.) */
    processedVectorCreateRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        var runurl = "";
        if (record.get("userCanWrite")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doProcessedVectors(' + id + ')">' +
                '<i class="gray-blue fa fa-play-circle fa-lg fa-fw" ext:qtip="Run preprocessing"></i></span>';
        }

        if (record.get('dateProcessedDataVectorComputation')) {
            var type = record.get('processedDataVectorComputationEventType');
            var color = "#000";

            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedProcessedVectorComputationEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }

            return '<span style="color:' + color + ';" ' + qtip + '>' + (suggestRun ? runurl : '')
                + Gemma.Renderers.dateRenderer(value) + '&nbsp;';
        } else {
            return '<span style="color:#3A3;">' + runurl + 'Needed</span>&nbsp;';
        }
    },

    differentialAnalysisRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');

        var diffIsPossible = function (record) {
            return record.get("numPopulatedFactors") > 0 && record.get("userCanWrite");
        };

        var runurl = "";
        if (record.get("userCanWrite")) {
            runurl = '<span class="link" onClick="return Ext.getCmp(\'eemanager\').doDifferential(' + id + ')">' +
                '<i class="gray-blue fa fa-play-circle fa-lg fa-fw" ext:qtip="Run differential expression analysis"></i></span>';
        }

        if (diffIsPossible(record)) {
            if (record.get('dateDifferentialAnalysis')) {
                var type = record.get('differentialAnalysisEventType');

                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';

                if (type == 'FailedDifferentialExpressionAnalysisEvent') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                } else if (record.get('hasDifferentialExpressionAnalysis')) {
                    // we ran it, but the analyses were apparently deleted.
                    return '<span style="color:#3A3;">' + runurl + 'Needed</span>&nbsp;';
                }
                return '<span style="color:' + color + ';" ' + qtip + '>' + (suggestRun ? runurl : '')
                    + Gemma.Renderers.dateRenderer(value) + '&nbsp;';
            } else {
                return '<span style="color:#3A3;">' + runurl + 'Needed</span>&nbsp;';
            }
        } else {
            return '<span style="color:#CCF;" ext:qtip="You must create at least one experimental factor to enable this analysis.">NA</span>';
        }
    },

    isPublicRenderer: function (value, metadata, record, rowIndex, colIndex, store) {
        var id = record.get('id');
        return Gemma.SecurityManager.getSecurityLink('ubic.gemma.model.expression.experiment.ExpressionExperiment',
            id, record.get('isPublic'), record.get('isShared'), record.get('userCanWrite'), null, null, null,
            record.get('userOwned'));
    }
};

Gemma.EEReportGridColumnModel = new Ext.grid.ColumnModel({
    columns: [{
        header: 'Short Name',
        sortable: true,
        dataIndex: 'shortName',
        renderer: Gemma.EEReportGridColumnRenderers.shortNameRenderer,
        tooltip: 'Code name of the experiment.',
        width: 60
    }, {
        header: 'Name',
        sortable: true,
        tooltip: 'Full experiment name.',
        dataIndex: 'name'
    }, {
        header: 'Taxon',
        sortable: true,
        dataIndex: 'taxon',
        tooltip: 'The taxon of this experiment.',
        width: 40
    }, {
        header: 'Public',
        sortable: true,
        renderer: Gemma.EEReportGridColumnRenderers.isPublicRenderer,
        tooltip: 'Whether this experiment is visible to public.',
        width: 30
    }, {
        header: '<i class="fa-custom-ext"></i>',
        dataIndex: 'troubled',
        sortable: true,
        renderer: Gemma.Renderers.troubleRenderer,
        tooltip: 'Shows a warning icon for unusable experiments.',
        width: 15
    }, {
        header: '<i class="fa-custom-exc"></i>',
        dataIndex: 'needsAttention',
        sortable: true,
        renderer: Gemma.Renderers.curationRenderer,
        tooltip: 'Shows a warning icon for experiments that are marked for curators attention.',
        width: 15
    }, {
        header: 'Quality',
        dataIndex: 'quality',
        sortable: true,
        renderer: Gemma.Renderers.qualityRenderer,
        tooltip: 'Shows the quality score of curated experiments.<br/><br/>Quality refers to data quality, wherein the same study could have been done twice with the same technical parameters and in one case yield bad quality data, and in another high quality data.<br/><br/>If the experiment is still in curation, this score can change significantly.',
        width: 15
    }, {
        header: 'Suitability',
        dataIndex: 'suitability',
        sortable: true,
        renderer: Gemma.Renderers.suitabilityRenderer,
        tooltip: 'Shows the suitability score of curated experiments.<br/><br/>Suitability refers to technical aspects which, if we were doing the study ourselves, we would have altered to make it optimal for analyses of the sort used in Gemma.<br/><br/>If the experiment is still in curation, this score can change significantly.',
        width: 15
    }, {
        header: 'Curation note',
        sortable: false,
        renderer: Gemma.Renderers.curationNoteStubRenderer,
        tooltip: 'Shows first 50 characters of the curation note for experiments that are marked for curators attention.',
        width: 100
    }, {
        header: '#ADs',
        sortable: true,
        dataIndex: 'arrayDesignCount',
        tooltip: "The number of different platforms used in the study.",
        width: 25
    }, {
        header: '#BAs',
        sortable: true,
        dataIndex: 'bioAssayCount',
        tooltip: 'The number of samples in the study.',
        width: 25
    }, {
        header: '#Prof',
        sortable: true,
        dataIndex: 'processedExpressionVectorCount',
        tooltip: 'The number of expression profiles.',
        width: 25
    }, {
        header: '#Facs',
        sortable: true,
        dataIndex: 'numPopulatedFactors',
        renderer: Gemma.EEReportGridColumnRenderers.experimentalDesignEditRenderer,
        tooltip: 'The number of experimental factors (variables) defined for the study, excluding any batch factors.',
        width: 25
    }, {
        header: '#tags',
        sortable: true,
        dataIndex: 'numAnnotations',
        renderer: Gemma.EEReportGridColumnRenderers.experimentTaggerRenderer,
        tooltip: 'The number of terms the experiment is tagged with.',
        width: 25
    }, {
        header: 'Updated',
        sortable: true,
        dataIndex: 'lastUpdated',
        tooltip: 'Last update (status or curation).',
        renderer: Gemma.Renderers.dateRenderer,
        width: 40
    }, {
        header: 'MissingVals',
        sortable: true,
        dataIndex: 'dateMissingValueAnalysis',
        tooltip: 'Status of missing value computation (two-channel studies only).',
        renderer: Gemma.EEReportGridColumnRenderers.missingValueAnalysisRenderer,
        width: 50
    }, {
        header: 'BatchInfo',
        sortable: true,
        dataIndex: 'dateBatchFetch',
        tooltip: 'Status of batch information.',
        renderer: Gemma.EEReportGridColumnRenderers.batchDateRenderer,
        width: 50
    }, {
        header: 'ProcProf',
        sortable: true,
        dataIndex: 'dateProcessedDataVectorComputation',
        tooltip: 'Status of processed expression profile configuration.',
        renderer: Gemma.EEReportGridColumnRenderers.processedVectorCreateRenderer,
        width: 50
    }, {
        header: 'Diff',
        sortable: true,
        dataIndex: 'dateDifferentialAnalysis',
        tooltip: 'Status of differential expression analysis. Must have factors to enable.',
        renderer: Gemma.EEReportGridColumnRenderers.differentialAnalysisRenderer,
        width: 50
    }, {
        header: 'Links',
        sortable: true,
        dataIndex: 'dateLinkAnalysis',
        tooltip: 'Status of coexpression analysis.',
        renderer: Gemma.EEReportGridColumnRenderers.linkAnalysisRenderer,
        width: 50
    }, {
       // combined diagnostics, not just PCA
        header: 'Diags',
        sortable: true,
        dataIndex: 'datePcaAnalysis',
        tooltip: 'Status of Diagnostics (currently based on PCA date).',
        renderer: Gemma.EEReportGridColumnRenderers.diagnosticsRenderer,
        width: 50
    }, {
        header: 'Admin',
        sortable: false,
        dataIndex: 'id',
        tooltip: 'Administration tools.',
        renderer: Gemma.EEReportGridColumnRenderers.adminRenderer,
        width: 30
    }]
});

Gemma.EEReportGridToolbar = Ext.extend(Ext.Toolbar,
    {

        showPublic: false,
        limit: 50,

        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        getSearchFun: function (text) {
            var value = new RegExp(Ext.escapeRe(text), 'i');
            return function (r, id) {
                var obj = r.data;
                return value.test(obj.name) || value.test(obj.shortName);
            };
        },
        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        refresh: function () {
            this.setFiltersToDefault();
            this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);
        },

        filterType: null,
        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        filterByNeed: function (box, record, index) {
            this.filterType = record.get('filterType');
            this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);
        },
        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        filterByTaxon: function (box, record, index) {

            // if user selected 'All taxa', load all datasets
            if (record.get('commonName') == "All taxa") {
                this.taxonid = null;
            } else {
                this.taxonid = record.get('id');
            }

            this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);

        },
        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        filterByLimit: function (box, record, index) {
            this.limit = record.get('count');
            // can't do "50 recently updated" and search results
            // this.searchCombo.clearValue();

            this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);
        },
        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        filterBySearch: function (combo, storeItem) {
            if (storeItem && storeItem.data && storeItem.data.resultValueObject) {
                var resultVO = storeItem.data.resultValueObject;

                if (resultVO.expressionExperimentIds) {
                    // Case when a group was returned.
                    this.ids = resultVO.expressionExperimentIds;
                } else if (resultVO.id) {
                    // Case when a single experiment was returned.
                    this.ids = [resultVO.id];
                } else {
                    // Case when neither was found.
                    console.log("ComboBox did not return any Expression Experiment ID/IDs.");
                }

                this.fireEvent('loadStore', [this.taxonid, this.ids, Gemma.DEFAULT_NUMBER_EXPERIMENTS, this.filterType,
                    this.showPublic]);
            }
        },
        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        getBookmark: function () {
            var url = Gemma.HOST_URL + "/expressionExperiment/showAllExpressionExperimentLinkSummaries.html?";
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

            Ext.Msg.alert("Your link to this list", '<a style="background-color:white" href="' + url + '">' + url
                + '</a>');
        },

        /**
         * Only call this after everything has loaded! otherwise will break
         * @memberOf Gemma.EEReportGridToolbar
         */
        setFiltersToDefault: function () {

            this.taxonid = (this.taxonCombo.getStore().getAt(0)) ? this.taxonCombo.getStore().getAt(0).get('id')
                : "-1";
            this.taxonCombo.setValue(this.taxonid);

            this.limit = this.limitCombo.defaultValue;
            this.limitCombo.setValue(this.limit);

            this.filterType = this.filterCombo.defaultValue;
            this.filterCombo.setValue(this.filterCombo.defaultValue);

            this.searchCombo.clearValue();
            this.ids = null;
        },

        /**
         * @memberOf Gemma.EEReportGridToolbar
         */
        initComponent: function () {

            this.filterCombo = new Ext.form.ComboBox({
                typeAhead: true,
                triggerAction: 'all',
                lazyRender: true,
                mode: 'local',
                defaultValue: '0',
                emptyText: "Filter by property",
                store: new Ext.data.ArrayStore({
                    id: 0,
                    fields: ['filterType', 'displayText'],
                    data: [[0, 'No filter'], [1, 'Need diff. expression analysis'],
                        [2, 'Need coexpression analysis'], [3, 'Has diff. expression analysis'],
                        [4, 'Has coexpression analysis'], [5, 'Unusable'], [13, 'Usable'], [6, 'No factors'], [7, 'No tags'],
                        [8, 'Needs batch info'], [9, 'Has batch info'], [10, 'Needs PCA'], [11, 'Has PCA'],
                        [12, 'Needs curators attention']]
                }),
                valueField: 'filterType',
                displayField: 'displayText',
                listeners: {
                    scope: this,
                    'select': this.filterByNeed
                }

            });

            this.taxonCombo = new Gemma.TaxonCombo({
                isDisplayTaxonWithDatasets: true,
                stateId: null, // Don't remember taxon value if user navigates away then comes back
                emptyText: "Filter by taxon",
                allTaxa: true, // Want an 'All taxa' option
                listeners: {
                    scope: this,
                    'select': this.filterByTaxon
                }
            });

            this.limitCombo = new Gemma.DataFilterCombo({
                listeners: {
                    scope: this,
                    'select': this.filterByLimit
                }
            });

            this.searchCombo = new Gemma.ExperimentAndExperimentGroupCombo({
                width: 220,
                emptyText: "Search for datasets",
                listeners: {
                    scope: this,
                    'selected': this.filterBySearch
                }
            });

            this.showPublicCheck = new Ext.form.Checkbox({
                tooltip: "Show/hide your public data sets.",
                boxLabel: "Show your public data",
                checked: this.showPublic,
                handler: function (checkbox, event) {
                    this.showPublic = checkbox.getValue();
                    this.fireEvent('loadStore', [this.taxonid, this.ids, this.limit, this.filterType, this.showPublic]);
                },
                scope: this
            });

            // set combos to initial values
            this.on('afterrender', function () {
                this.taxonCombo.getStore().on(
                    'doneLoading',
                    function () {
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

            Ext.apply(this, {
                items: [this.searchCombo, this.filterCombo, this.taxonCombo, this.limitCombo, {
                    xtype: 'button',
                    minWidth: 20,
                    cls: 'x-btn-icon',
                    icon: Gemma.CONTEXT_PATH + '/images/icons/cross.png',
                    handler: this.refresh,
                    tooltip: "Clear filters",
                    scope: this
                }, '->', this.showPublicCheck, {
                    xtype: 'button',
                    minWidth: 20,
                    cls: 'x-btn-icon',
                    icon: Gemma.CONTEXT_PATH + '/images/icons/link.png',
                    handler: this.getBookmark,
                    tooltip: "Bookmarkable link",
                    scope: this
                }, {
                    xtype: 'button',
                    minWidth: 20,
                    cls: 'x-btn-icon',
                    icon: Gemma.CONTEXT_PATH + '/images/icons/disk.png',
                    handler: function () {
                        this.fireEvent('showAsText', []);
                    }.createDelegate(this),
                    tooltip: "Download as text",
                    scope: this
                }]
            });

            Gemma.EEReportGridToolbar.superclass.initComponent.call(this);
        }
    });