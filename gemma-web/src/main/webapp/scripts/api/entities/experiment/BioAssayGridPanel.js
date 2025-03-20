Ext.namespace('Gemma');
/**
 * Used in the showBioAssaysFromExpressionExperiment.html page
 *
 * @class Gemma.BioAssayGrid
 * @extends Gemma.GemmaGridPanel
 * @author Paul
 */
Gemma.BioAssayGrid = Ext.extend(Gemma.GemmaGridPanel, {
    collapsible: false,
    loadMask: true,
    defaults: {
        autoScroll: true
    },
    detectedOutlierIds: [],
    height: 500,
    width: 800,
    autoScroll: true,

    autoExpandColumn: 'description',

    record: Ext.data.Record.create([{
        name: "id",
        type: "int"
    }, {
        name: "name",
        type: "string"
    }, {
        name: "description",
        type: "string"
    }, {
        name: "outlier",
        type: "boolean"
    }, {
        name: "userFlaggedOutlier",
        type: "boolean"
    }, {
        name: "predictedOutlier",
        type: "boolean"
    }]),

    initComponent: function () {

        Ext.apply(this, {
            tbar: new Ext.Toolbar({
                items: [{
                    xtype: 'button',
                    text: 'Save outlier changes',
                    id: 'bioassay-outlier-save-button',
                    handler: function (b, e) {
                        console.log("Saving");
                        var outliers = [];
                        var nonOutliers = [];
                        this.store.each(function (record, id) {
                            if (record.get('userFlaggedOutlier') && !record.get('outlier')) {
                                outliers.push(record.get('id'));
                            } else if (!record.get('userFlaggedOutlier') && record.get('outlier')) {
                                nonOutliers.push(record.get('id'));
                            }
                        });

                        if (outliers.length > 0) {
                            Ext.getCmp('eemanager').markOutlierBioAssays(outliers);
                        } else if (nonOutliers.length > 0) {
                            Ext.getCmp('eemanager').unMarkOutlierBioAssays(nonOutliers);
                        }
                    }.createDelegate(this)
                }]
            }),
            store: new Ext.data.Store({
                proxy: new Ext.data.DWRProxy({
                    apiActionToHandlerMap: {
                        read: {
                            dwrFunction: BioAssayController.getBioAssays
                        }
                    },
                    getDwrArgsFunction: function (request, recordDataArray) {
                        if (request.options.params && request.options.params instanceof Array) {
                            return request.options.params;
                        }
                        return [this.eeId];
                    }
                }),
                reader: new Ext.data.ListRangeReader({
                    id: "id"
                }, this.record)

            })
        });

        Ext.apply(this, {
            columns: [{
                id: 'name',
                header: "Name",
                dataIndex: "name",
                tooltip: "Name of the bioassay",
                scope: this,
                // width : 80,
                width: 0.15,
                sortable: true,
                renderer: this.nameRenderer
            }, {
                id: 'description',
                header: "Description",
                dataIndex: "description",
                tooltip: "The descriptive name of the assay, usually supplied by the submitter",
                // width : 120,
                width: 0.45,

                scope: this,
                sortable: true,
                renderer: this.descRenderer
            }]
        });

        var isAdmin = Gemma.SecurityManager.isAdmin();

        if (isAdmin) {

            /*
             * CheckColumn::onMouseDown() sets the outlier status in the record.
             */
            outlierChx = new ExtJsSucksCheckColumn({
                header: "Mark outlier",
                dataIndex: 'userFlaggedOutlier',
                tooltip: 'Check to indicate this sample is an outlier',
                width: 0.15,
                listeners: {
                    'checkchange': function (column, rowIndex, checked, eOpts) {

                    }
                }
            });

            this.columns.push({
                header: "Is outlier",
                dataIndex: "id",
                renderer: this.isOutlierRender,
                width: 0.15
            });

            this.columns.push(outlierChx);
            this.plugins = [outlierChx]; // needed to allow editing.
        }

        var me = this;

        Gemma.BioAssayGrid.superclass.initComponent.call(this);

        this.getStore().on("load", function (store, records, options) {
            this.doLayout.createDelegate(this);
        }, this);

        if (this.eeId) {
            this.getStore().load({
                params: [this.eeId]
            });
        }

    },

    /**
     * @memberOf Gemma.BioAssayGrid
     */
    nameRenderer: function (value, metadata, record, row, col, ds) {
        return "<a  title='Show details of this bioassay' style='cursor:pointer' href='" + Gemma.CONTEXT_PATH + "/bioAssay/showBioAssay.html?id="
            + record.get('id') + "'>" + record.get('name') + "</a>";
    },

    descRenderer: function (value, metadata, record, row, col, ds) {
        var color = 'black';

        if (record.get('outlier')) {
            color = 'red';
            return " <i class='" + color + "'>Removed as an outlier;" + record.get('name') + "</i>";
        }

        if (record.get('predictedOutlier')) {
            color = 'red';
            return "<i class='" + color + "'>Predicted outlier; " + record.get('name') + "</i>";
        }

        return record.get('name');

    },

    isOutlierRender: function (value, metadata, record, row, col, ds) {
        if (record.get('outlier')) {
            return "<i class=\"fa fa-exclamation-triangle fa-lg\"></i>";
        }
        return "";
    }

});

ExtJsSucksCheckColumn = Ext.extend(Ext.ux.grid.CheckColumn, {
    // private
    initComponent: function () {
        Ext.ux.grid.CheckColumn.superclass.initComponent.call(this);

        this.addEvents(
            'checkchange'
        );
    },

    processEvent: function (name, e, grid, rowIndex, colIndex) {
        if (name == 'mousedown') {
            var record = grid.store.getAt(rowIndex);
            record.set(this.dataIndex, !record.data[this.dataIndex]);

            this.fireEvent('checkchange', this, record.data[this.dataIndex]);

            return false; // Cancel row selection.
        } else {
            return Ext.grid.ActionColumn.superclass.processEvent.apply(this, arguments);
        }
    },

    renderer: function (v, p, record) {
        p.css += ' x-grid3-check-col-td';
        return String.format('<div class="x-grid3-check-col{0}">&#160;</div>', v ? '-on' : '');
    },

    // Deprecate use as a plugin. Remove in 4.0
    init: Ext.emptyFn
});
ExtJsSucksCheckColumn = Ext.extend(Ext.ux.grid.CheckColumn, {
    // private
    initComponent: function () {
        Ext.ux.grid.CheckColumn.superclass.initComponent.call(this);

        this.addEvents(
            'checkchange'
        );
    },

    processEvent: function (name, e, grid, rowIndex, colIndex) {
        if (name == 'mousedown') {
            var record = grid.store.getAt(rowIndex);
            record.set(this.dataIndex, !record.data[this.dataIndex]);

            this.fireEvent('checkchange', this, record.data[this.dataIndex]);

            return false; // Cancel row selection.
        } else {
            return Ext.grid.ActionColumn.superclass.processEvent.apply(this, arguments);
        }
    },

    renderer: function (v, p, record) {
        p.css += ' x-grid3-check-col-td';
        return String.format('<div class="x-grid3-check-col{0}">&#160;</div>', v ? '-on' : '');
    },

    // Deprecate use as a plugin. Remove in 4.0
    init: Ext.emptyFn
});