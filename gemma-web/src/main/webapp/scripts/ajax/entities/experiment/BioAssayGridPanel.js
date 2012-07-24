Ext.namespace('Gemma');
/**
 * 
 * @class Gemma.BioAssayGrid
 * @extends Gemma.GemmaGridPanel
 * @version $Id$
 * @author Paul
 */
Gemma.BioAssayGrid = Ext.extend(Gemma.GemmaGridPanel, {
            collapsible : false,
            loadMask : true,
            defaults : {
                autoScroll : true
            },

            height : 500,
            width : 800,
            autoScroll : true,

            autoExpandColumn : 'description',

            record : Ext.data.Record.create([{
                        name : "id",
                        type : "int"
                    }, {
                        name : "name",
                        type : "string"
                    }, {
                        name : "description",
                        type : "string"
                    }, {
                        name : "outlier",
                        type : "boolean"
                    }]),

            initComponent : function() {

                Ext.apply(this, {
                            store : new Ext.data.Store({
                                        proxy : new Ext.data.DWRProxy({
                                                    apiActionToHandlerMap : {
                                                        read : {
                                                            dwrFunction : BioAssayController.getBioAssays
                                                        }
                                                    },
                                                    getDwrArgsFunction : function(request, recordDataArray) {
                                                        if (request.options.params
                                                                && request.options.params instanceof Array) {
                                                            return request.options.params;
                                                        }
                                                        return [this.eeId];
                                                    }
                                                }),
                                        reader : new Ext.data.ListRangeReader({
                                                    id : "id"
                                                }, this.record)
                                    })
                        });

                Ext.apply(this, {
                            columns : [{
                                        id : 'name',
                                        header : "Name",
                                        dataIndex : "name",
                                        tooltip : "Name of the bioassay",
                                        scope : this,
                                        // width : 80,
                                        width : 0.15,
                                        sortable : true,
                                        renderer : this.nameRenderer
                                    }, {
                                        id : 'description',
                                        header : "Description",
                                        dataIndex : "description",
                                        tooltip : "The descriptive name of the assay, usually supplied by the submitter",
                                        // width : 120,
                                        width : 0.45,
                                        sortable : true
                                    }]
                        });

                var isAdmin = (Ext.get('hasAdmin')) ? Ext.get('hasAdmin').getValue() : false;

                if (isAdmin) {
                    this.columns.push({
                                header : "Remove as outlier",
                                dataIndex : "id",
                                renderer : this.outlierRemoveRender,

                                width : 0.15
                            });
                }
                Gemma.BioAssayGrid.superclass.initComponent.call(this);

                this.getStore().on("load", function(store, records, options) {
                            this.doLayout.createDelegate(this);
                        }, this);

                if (this.eeId) {
                    this.getStore().load({
                                params : [this.eeId]
                            });
                }

            },

            nameRenderer : function(value, metadata, record, row, col, ds) {
                return "<a href=\"/Gemma/bioAssay/showBioAssay.html?id=" + record.get('id') + "\">"
                        + record.get('name') + "</a>";
            },

            outlierRemoveRender : function(value, metadata, record, row, col, ds) {
                if (record.get('outlier')) {
                    return "<img src=\"/Gemma/images/icons/stop.png\"/>";
                }

                return "<span class=\"link\" onClick=\"Ext.getCmp('eemanager').markOutlierBioAssay(" + record.get('id')
                        + ")\"><img title=\"Click to mark as an outlier\" src=\"/Gemma/images/icons/ok.png\"/></span>";
            }

        });