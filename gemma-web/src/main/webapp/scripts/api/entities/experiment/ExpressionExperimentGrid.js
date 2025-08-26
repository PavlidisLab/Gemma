/*
 */
Ext.namespace('Gemma');

/**
 *
 * Grid to display ExpressionExperiments. Author: Paul (based on Luke's CoexpressionDatasetGrid) $Id:
 * ExpressionExperimentGrid.js,v 1.13 2008/04/23 19:54:46 kelsey Exp $
 */
Gemma.ExpressionExperimentGrid = Ext.extend(Gemma.GemmaGridPanel, {

    /*
     * Do not set header : true here - it breaks it.
     */
    collapsible: false,
    readMethod: ExpressionExperimentController.loadExpressionExperiments.createDelegate(this, [], true),

    autoExpandColumn: 'name',

    showAnalysisInfo: true,

    /**
     * @cfg Controls whether the experiment short name should be a link (to the experiment's page). Defaults to
     *      true but should be false when the user is required to select rows. (In this case, clicking the name has
     *      two functions: (1) select row and (2) go to page)
     */
    experimentNameAsLink: true,
    editable: true,
    stateful: false,
    loadOnlyOnRender: false,
    loadMask: true,
    record: Ext.data.Record.create([{
        name: "id",
        type: "int"
    }, {
        name: "shortName",
        type: "string"
    }, {
        name: "name",
        type: "string"
    }, {
        name: "arrayDesignCount",
        type: "int"
    }, {
        name: "bioAssayCount",
        type: "int"
    }, {
        name: "externalUri",
        type: "string"
    }, {
        name: "description",
        type: "string"
    }, {
        name: "hasCoexpressionAnalysis",
        type: "boolean"
    }, {
        name: "hasDifferentialExpressionAnalysis",
        type: "boolean"
    }, {
        name: 'taxonId',
        type: 'int'
    }, {
        name : "troubled",
        type: "boolean"
    }]),

    /**
     * @memberOf Gemma.ExpressionExperimentGrid
     */
    searchForText: function (button, keyev) {
        var text = this.searchInGridField.getValue();
        if (text.length < 2) {
            this.clearFilter();
            return;
        }
        this.getStore().filterBy(this.getSearchFun(text), this, 0);
    },

    clearFilter: function () {
        this.getStore().clearFilter();
    },

    getSearchFun: function (text) {
        var value = new RegExp(Ext.escapeRe(text), 'i');
        return function (r, id) {
            var obj = r.data;
            return value.test(obj.name) || value.test(obj.shortName);
        };
    },

    initComponent: function () {
        this.searchInGridField = new Ext.form.TextField({
            enableKeyEvents: true,
            emptyText: 'Filter',
            tooltip: "Text typed here will ",
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

        if (!this.records) {

            if (this.platformId != null) {
                this.readMethod = ExpressionExperimentController.loadExperimentsForPlatform.createDelegate(this, [],
                    true);
            }

            Ext.apply(this, {
                store: new Ext.data.Store({
                    proxy: new Ext.data.DWRProxy(this.readMethod),
                    reader: new Ext.data.ListRangeReader({
                        id: "id"
                    }, this.record)
                })
            });
        } else {
            Ext.apply(this, {
                store: new Ext.data.Store({
                    proxy: new Ext.data.MemoryProxy(this.records),
                    reader: new Ext.data.ListRangeReader({}, this.record)
                })
            });
        }
        Ext.apply(this, {
            bbar: new Ext.Toolbar({
                items: ['->', {
                    xtype: 'button',
                    handler: this.clearFilter.createDelegate(this),
                    scope: this,
                    cls: 'x-btn-text',
                    text: 'Reset filter'
                }, ' ', this.searchInGridField]
            })
        });

        Ext.apply(this, {
            columns: [
                {
                    id: 'shortName',
                    header: "Dataset",
                    dataIndex: "shortName",
                    tooltip: "The unique short name for the dataset, often the accession number from the "
                    + "originating source database. Click on the name to view the details page.",
                    renderer: this.formatEE,
                    scope: this,
                    // width : 80,
                    width: 0.15,
                    sortable: true
                }, {
                    id: 'name',
                    header: "Name",
                    dataIndex: "name",
                    tooltip: "The descriptive name of the dataset, usually supplied by the submitter",
                    // width : 120,
                    width: 0.45,
                    sortable: true
                }, {
                    id: 'arrays',
                    header: "Arrays",
                    dataIndex: "arrayDesignCount",
                    hidden: true,
                    tooltip: "The number of different types of array platforms used",
                    // width : 50,
                    width: 0.15,
                    sortable: true
                }, {
                    id: 'assays',
                    header: "Assays",
                    dataIndex: "bioAssayCount",
                    renderer: this.formatAssayCount,
                    tooltip: "The number of arrays (~samples) present in the study",
                    // width : 50,
                    width: 0.1,
                    sortable: true
                }]
        });

        if (this.showAnalysisInfo) {
            this.columns.push({
                id: 'analyses',
                header: "Diff.An.",
                dataIndex: "hasDifferentialExpressionAnalysis",
                tooltip: "Indicates whether differential expression data is available for the study",
                renderer: this.formatAnalysisInfo,
                sortable: true,
                width: 0.05
            });
            this.columns.push({
                id: 'analyses',
                header: "Coexp.An.",
                dataIndex: "hasCoexpressionAnalysis",
                tooltip: "Indicates whether coexpression data is available for the study",
                renderer: this.formatAnalysisInfo,
                sortable: true,
                width: 0.05
            });
        }

        if (this.rowExpander) {
            Ext.apply(this, {
                rowExpander: new Gemma.EEGridRowExpander({
                    tpl: ""
                })
            });
            this.columns.unshift(this.rowExpander);
            Ext.apply(this, {
                plugins: this.rowExpander
            });
        }

        Gemma.ExpressionExperimentGrid.superclass.initComponent.call(this);

        this.on("keypress", function (e) {
            if (e.getCharCode() === Ext.EventObject.DELETE) {
                this.removeSelected();
            }
        }, this);

        this.getStore().on("load", function (store, records, options) {
            this.doLayout.createDelegate(this);
        }, this);

        if (!this.loadOnlyOnRender) {
            this.load();
        } else {
            this.on('render', function () {
                this.load();
            });
        }

    },

    load: function () {
        if (this.eeids) {
            this.getStore().load({
                params: [this.eeids]
            });
        } else if (this.platformId) {
            this.getStore().load({
                params: [this.platformId]
            });
        }

    },

    afterRender: function () {
        Gemma.ExpressionExperimentGrid.superclass.afterRender.call(this);
        if (this.getTopToolbar()) {
            this.getTopToolbar().grid = this;
        }
    },

    removeSelected: function () {
        var recs = this.getSelectionModel().getSelections();
        for (var x = 0; x < recs.length; x++) { // for r in recs
            // does
            // not
            // work!
            this.getStore().remove(recs[x]);
            this.getView().refresh();
        }
    },

    formatAnalysisInfo: function (value, metadata, record, row, col, ds) {
        if (value)
            return '<i id="aliasHelp" class="fa fa-check fa-fw" style="font-size:smaller;color:green"></i>';
        //   return "<img src='" + Gemma.CONTEXT_PATH + "/images/icons/ok.png' height='16' width='16' ext:qtip='Has analysis' />";

        return '';
    },

    formatAssayCount: function (value, metadata, record, row, col, ds) {
        return record.get("bioAssayCount");
    },

    formatEE: function (value, metadata, record, row, col, ds) {
        var eeTemplate = new Ext.XTemplate(
            '<tpl for="."><a target="_blank" title="{name}" href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id=',
            '{id}"', ' ext:qtip="{name}">{shortName}</a></tpl>');
        return this.experimentNameAsLink ? eeTemplate.apply(record.data) : value;
    },

    /**
     * Return all the ids of the experiments shown in this grid.
     */
    getEEIds: function () {
        var result = [];
        this.store.each(function (rec) {
            result.push(rec.get("id"));
        });
        return result;
    },

    isEditable: function () {
        return this.editable;
    },

    setEditable: function (b) {
        this.editable = b;
    }

});

Gemma.ExpressionExperimentListView = Ext.extend(Ext.list.ListView, {
    columns: [
        {
            id: 'shortName',
            header: "Dataset",
            dataIndex: "shortName",
            tooltip: "The unique short name for the dataset, often the accession number"
            + " from the originating source database. Click on the name to view the details page.",
            // renderer : this.formatEE, // can't use rendered in list view
            width: 0.2,
            sortable: true
        }, {
            id: 'name',
            header: "Name",
            dataIndex: "name",
            tooltip: "The descriptive name of the dataset, usually supplied by the submitter",
            width: 0.4,
            sortable: true
        }, {
            id: 'arrays',
            header: "Arrays",
            dataIndex: "arrayDesignCount",
            hidden: true,
            tooltip: "The number of different types of array platforms used",
            width: 0.1,
            sortable: true
        }, {
            id: 'assays',
            header: "Assays",
            dataIndex: "bioAssayCount",
            tooltip: "The number of arrays (~samples) present in the study",
            width: 0.1,
            sortable: true
        }],

    store: new Ext.data.Store({
        proxy: new Ext.data.DWRProxy({
            apiActionToHandlerMap: {
                read: {
                    dwrFunction: ExpressionExperimentController.loadExpressionExperiments
                }
            }
        }),
        reader: new Ext.data.ListRangeReader({
            id: "id",
            fields: [{
                name: "id",
                type: "int"
            }, {
                name: "shortName",
                type: "string"
            }, {
                name: "name",
                type: "string"
            }, {
                name: "arrayDesignCount",
                type: "int"
            }, {
                name: "bioAssayCount",
                type: "int"
            }, {
                name: "externalUri",
                type: "string"
            }, {
                name: "description",
                type: "string"
            }, {
                name: 'taxonId',
                type: 'int'
            }]
        })
    })
});

/**
 *
 * @param {}
 *           datasets
 * @param {}
 *           eeMap
 */
Gemma.ExpressionExperimentGrid.updateDatasetInfo = function (datasets, eeMap) {
    for (var i = 0; i < datasets.length; ++i) {
        var ee = eeMap[datasets[i].id];
        if (ee) {
            datasets[i].shortName = ee.shortName;
            datasets[i].name = ee.name;
        }
    }
};

/**
 *
 * @class Gemma.EEGridRowExpander
 * @extends Ext.grid.RowExpander
 */
Gemma.EEGridRowExpander = Ext.extend(Ext.grid.RowExpander, {

    fillExpander: function (data, body, rowIndex) {
        Ext.DomHelper.overwrite(body, {
            tag: 'p',
            html: data
        });
    },

    beforeExpand: function (record, body, rowIndex) {
        Ext.DomHelper.overwrite(body, {
            tag: 'p',
            html: ' Loading...'
        });
        ExpressionExperimentController.getDescription(record.id, {
            callback: this.fillExpander.createDelegate(this, [body, rowIndex], true)
        });
        return true;
    }

});

Gemma.ExpressionExperimentQCGrid = Ext.extend(Gemma.ExpressionExperimentGrid, {
    loadMask: true,
    record: Ext.data.Record.create([{
        name: "id",
        type: "int"
    }, {
        name: "shortName",
        type: "string"
    }, {
        name: "name",
        type: "string"
    }, {
        name: "sampleRemoved",
        type: "string"
    }, {
        name: "batchEffect",
        type: "string"
    }]),
    booleanRenderer: function (value) {
        return (value == "true") ? "Yes" : '<span style="color:grey">No</span>';
    },
    initComponent: function () {

        Gemma.ExpressionExperimentQCGrid.superclass.initComponent.call(this);
        var store = new Ext.data.Store({
            autoLoad: true,
            proxy: new Ext.data.DWRProxy(ExpressionExperimentController.loadExpressionExperimentsWithQcIssues),
            reader: new Ext.data.JsonReader({
                root: 'records', // required.
                successProperty: 'success', // same as default.
                messageProperty: 'message', // optional
                totalProperty: 'total', // default is 'total'; optional unless paging.
                idProperty: "id", // same as default
                fields: this.record
            })
        });
        store.on('load', function (store, records, options) {
           this.setTitle( records.length + " Datasets had samples removed due to outliers" );
        }, this);
        Ext.apply(this, {
            store: store
        });

        Ext.apply(this, {
            colModel: new Ext.grid.ColumnModel({
                defaults: {
                    sortable: true
                },

                columns: [
                    {
                        id: 'shortName',
                        header: "Dataset",
                        dataIndex: "shortName",
                        tooltip: "The unique short name for the dataset, often the accession number "
                        + "from the originating source database. Click on the name to view the details page.",
                        renderer: this.formatEE,
                        scope: this,
                        width: 0.15
                    }, {
                        id: 'name',
                        header: "Name",
                        dataIndex: "name",
                        tooltip: "The descriptive name of the dataset, usually supplied by the submitter",
                        width: 0.55
                    } /*
                            * ,{ id : 'sampleRemoved', header : "Samples Removed", dataIndex : "sampleRemoved", tooltip :
                            * "Datasets that have had samples removed due to outliers.", width:0.1, renderer:
                            * this.booleanRenderer }/*,{ id : 'batchEffect', header : "Batch Effects", dataIndex :
                            * "batchEffect", tooltip : "Datasets that have possible batch effects.", width:0.1,
                            * renderer: this.booleanRenderer }
                            */]
            })
        });

    }
});
