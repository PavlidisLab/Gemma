/**
 * Shows history of an auditable, allows adding events.
 *
 */
Ext.namespace('Gemma');
Gemma.AuditTrailGrid = Ext.extend(Ext.grid.GridPanel, {

    title: "History",
    height: 200,
    width: 720,
    loadMask: true,
    stateful: false,
    loadOnlyOnRender: false,

    record: Ext.data.Record.create([{
        name: "id",
        type: "int"
    }, {
        name: "date",
        type: "date"
    }, {
        name: "actionName",
        type: "string"
    }, {
        name: "note",
        type: "string"
    }, {
        name: "detail",
        type: "string"
    }, {
        name: "performer"
    }, {
        name: "eventTypeName",
        type: "string"
    }]),

    /**
     * @memberOf Gemma.AuditTrailGrid
     */
    createEvent: function (obj) {
        var cb = function () {
            this.getStore().reload();
            this.refreshGrid();
        }.createDelegate(this);
        AuditController.addAuditEvent(this.auditable, obj.type, obj.comment, obj.details, {
            callback: cb
        });
    },

    showAddEventDialog: function () {
        if (!this.addEventDialog) {
            this.addEventDialog = new Gemma.AddAuditEventDialog({
                comment: true
            });
            this.addEventDialog.on("commit", function (resultObj) {
                this.createEvent(resultObj);
            }.createDelegate(this));
        }
        this.addEventDialog.show();
    },

    refreshGrid: function () {
        this.getStore().load({
            params: [this.auditable]
        });
    },

    eventTypeRenderer: function (value, metaData, record, rowIndex, colIndex, store) {
        var ret = value.replace(/.*\./, '').replace("Impl", '').replace(/([A-Z])/g, ' $1');

        if (value.indexOf("Trouble") !== -1) {
            if (value === 'NotTroubledStatusFlagEvent') {
                ret = '<i class="green fa fa-check-circle fa-lg fa-fw"></i>' + ret;
            } else {
                ret = '<i class="red fa fa-exclamation-triangle fa-lg fa-fw"></i>' + ret;
            }
        }else if (value.indexOf("DoesNotNeedAttention") !== -1) {
            ret = '<i class="green fa fa-check-circle-o fa-lg fa-fw"></i>' + ret;
        }else if (value.indexOf("NeedsAttention") !== -1
            || (value.indexOf("Failed") !== -1
                    && (value.indexOf("Analysis") !== -1 || value.indexOf("VectorComputation") !== -1))) {
            ret = '<i class="gold fa fa-exclamation-circle fa-lg fa-fw"></i>' + ret;
        }else if (value.indexOf("CurationNote") !== -1) {
            ret = '<i class="dark-gray fa fa-pencil-square-o fa-lg fa-fw"></i>' + ret;
        }else if (value.indexOf("Geeq") !== -1) {
            ret = '<i class="gray-blue fa fa-star fa-lg fa-fw"></i>' + ret;
        }
        return ret;
    },

    initComponent: function () {

        Ext.apply(this, {
            columns: [{
                header: "Date",
                width: 105,
                dataIndex: "date",
                renderer: Gemma.Renderers.dateTimeRenderer,
                sortable: true
            }, {
                header: "Action",
                width: 50,
                hidden: true,
                dataIndex: "actionName"
            }, {
                header: "Performer",
                width: 80,
                dataIndex: "performer"
            }, {
                header: "Event type",
                width: 170,
                dataIndex: "eventTypeName",
                renderer: this.eventTypeRenderer
            }, {
                header: "Comment",
                width: 275,
                dataIndex: "note"
            }],
            store: new Ext.data.Store({
                proxy: new Ext.data.DWRProxy(AuditController.getEvents),
                reader: new Ext.data.ListRangeReader({
                    id: "id"
                }, this.record),
                remoteSort: false
            }),
            tbar: [{
                xtype: 'button',
                icon: Gemma.CONTEXT_PATH + "/images/icons/add.png",
                tooltip: 'Add&nbsp;a&nbsp;comment',
                handler: this.showAddEventDialog,
                scope: this
            }, {
                xtype: 'button',
                icon: Gemma.CONTEXT_PATH + "/images/icons/arrow_refresh_small.png",
                tooltip: 'Reload view from the database',
                handler: this.refreshGrid,
                scope: this
            }]
        });

        Gemma.AuditTrailGrid.superclass.initComponent.call(this);

        this.getColumnModel().defaultSortable = false;
        this.getStore().setDefaultSort('date', 'desc');

        if (!this.loadOnlyOnRender) {
            this.getStore().load({
                params: [this.auditable]
            });
        } else {
            this.on('render', function () {
                this.getStore().load({
                    params: [this.auditable]
                });
            });
        }

        this.on('rowdblclick', function (grid, row, event) {
            var record = this.getStore().getAt(row).data;
            var note = record.note;
            var detail = record.detail;
            detail = detail.replace("\n", "<br />\n");
            var content = "Date: " + record.date + "<br />Performer: " + record.performer + "<br />Note: " + note
                + "<br />Details: " + detail;

            // bug 3934: make this scrollable. I think in extjs 4 the MessageBox is more flexible.
            var w = new Ext.Window({
                title: "Event details",
                html: content,
                bodyStyle: 'padding:7px;background: white; font-size:1.1em',
                autoScroll: true,
                width: 600,
                height: 600
            });

            w.show();
        });
    }
});

Gemma.AddAuditEventDialog = Ext.extend(Ext.Window, {

    comment: false,
    height: 350,
    width: 550,
    shadow: true,
    minWidth: 200,
    minHeight: 150,
    closeAction: "hide",
    modal: true,
    layout: 'fit',
    layoutConfig: {
        forceFit: true
    },

    title: this.comment ? "Add a comment" : "Change usability status",

    /**
     * @memberOf Gemma.AddAuditEventDialog
     */
    validate: function () {
        return this.auditEventTypeCombo.isValid() && this.auditEventCommentField.isValid()
            && this.auditEventDetailField.isValid();
    },

    initComponent: function () {

        this.auditEventTypeStore = new Ext.data.SimpleStore({
            fields: ['type', 'description', 'icon'],
            data: this.comment
                ? [['CommentedEvent', 'Comment', 'pencil-square-o']]
                : [['TroubledStatusFlagEvent', 'Mark as unusable', 'exclamation-triangle'],
                  ['NotTroubledStatusFlagEvent', 'Mark as usable', 'check-circle'],
                    ['UnsuitableForDifferentialExpressionAnalysisEvent', 'Mark as unusable for DEA', 'exclamation-triangle'],
                    ['ResetSuitabilityForDifferentialExpressionAnalysisEvent', 'Reset usability for DEA', 'check-circle']
                ]
        });

        var self = this;

        this.auditEventTypeCombo = new Ext.form.ComboBox({
            fieldLabel: 'Event type',
            store: this.auditEventTypeStore,
            displayField: 'description',
            valueField: 'type',
            typeAhead: true,
            mode: 'local',
            allowBlank: false,
            triggerAction: 'all',
            emptyText: 'Select an event type',
            editable: false,
            width: 180,
            tpl:
            '<tpl for=".">' +
            '<tpl if="0==0"><div class="x-combo-list-item" ><i class="fa fa-{icon} fa-fw"></i>{description}</div></tpl>' +
            '</tpl>',
            selectOnFocus: true,
            listeners: {
                afterrender: function(combo) {
                    var recordSelected = combo.getStore().getAt(0);
                    combo.setValue(recordSelected.data.type);
                    if(self.comment){
                        combo.hide();
                    }
                }
            }
        });
        this.auditEventCommentField = new Ext.form.TextField({
            fieldLabel: 'Comment',
            width: 400,
            allowBlank: true
        });
        this.auditEventDetailField = new Ext.form.TextArea({
            fieldLabel: 'Details',
            height: 200,
            width: 400,
            allowBlank: true
        });

        this.fs = new Ext.form.FieldSet({
            items: [this.auditEventTypeCombo, this.auditEventCommentField, this.auditEventDetailField]
        });

        Ext.apply(this, {
            items: [this.fs],
            buttons: [{
                text: 'Add Event',
                handler: function () {
                    if (this.validate()) {
                        this.hide();
                        this.fireEvent('commit', {
                            comment: this.auditEventCommentField.getValue(),
                            type: this.auditEventTypeCombo.getValue(),
                            details: this.auditEventDetailField.getValue()
                        });
                    } else {
                        Ext.Msg.alert("Error", "You must fill in the required fields");
                    }
                }.createDelegate(this),
                scope: this
            }, {
                text: 'Cancel',
                handler: this.hide.createDelegate(this)
            }]
        });

        Gemma.AddAuditEventDialog.superclass.initComponent.call(this);

        this.addEvents('commit');
    }


});
