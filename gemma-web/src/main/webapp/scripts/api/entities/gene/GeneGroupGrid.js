/**
 * A list of gene groups.
 *
 * @class Gemma.GeneGroupPanel
 * @extends Ext.grid.EditorGridPanel
 */
Gemma.GeneGroupPanel = Ext.extend(Ext.grid.EditorGridPanel, {

    selModel: new Ext.grid.RowSelectionModel({
        singleSelect: true
    }),

    name: 'geneGroupGridPanel',
    stripeRows: true,

    /**
     * @memberOf Gemma.GeneGroupPanel
     */
    initComponent: function () {
        Gemma.GeneGroupPanel.superclass.initComponent.call(this);
        if (!this.store) {
            Ext.apply(this, {
                store: new Gemma.GeneGroupStore()
            });
        }
        this.addEvents({
            'dirty': true
        });

        this.record = this.getStore().record;

    },

    afterRender: function () {

        Gemma.GeneGroupPanel.superclass.afterRender.call(this);

        this.loadMask = new Ext.LoadMask(this.body, {
            msg: Gemma.StatusText.Loading.generic,
            store: this.store
        });

        /*
         * these methods don't seem to work anymore this.relayEvents(this.getSelectionModel(), 'rowselect');
         * this.relayEvents(this.getStore(), 'datachanged');
         */

        this.getSelectionModel().on("rowselect", function (selmol, index, rec) {
            this.getStore().setSelected(rec);
            this.fireEvent("rowselect", selmol, index, rec);
        }, this);

        this.getSelectionModel().on("datachanged", function (store) {
            this.fireEvent("datachanged", store);
        }, this);
    },

    columns: [
        {
            header: 'Name',
            dataIndex: 'name',
            width: 0.45,
            editable: true,
            sortable: true,
            editor: new Ext.form.TextField({
                allowBlank: false
            })
        },
        {
            header: 'Details',
            dataIndex: 'id',
            width: 0.06,
            editable: false,
            sortable: false,
            renderer: function (value, metadata, record, rowIndex, colIndex, store) {
                return '<a target="_blank" title="Go to gene group page" '
                    + 'href="' + Gemma.CONTEXT_PATH + '/geneSet/showGeneSet.html?id=' + record.data.id
                    + '"><img src="' + Gemma.CONTEXT_PATH + '/images/magnifier.png"></a>';
            }
        },
        {
            header: 'Description',
            dataIndex: 'description',
            width: 0.45,
            editable: true,
            sortable: true,
            editor: new Ext.form.TextField({
                allowBlank: false
            })
        },
        {
            header: 'Taxon',
            dataIndex: 'taxonName',
            width: 0.15,
            editable: false,
            sortable: true
        },
        {
            header: 'Size',
            sortable: true,
            dataIndex: 'size',
            editable: false,
            width: 0.07,
            tooltip: 'number of genes in group'
        },
        {
            header: 'Flags',
            sortable: true,
            width: 0.1,
            renderer: function (value, metadata, record, rowIndex, colIndex, store) {
                var result = Gemma.SecurityManager.getSecurityLink("ubic.gemma.model.genome.gene.GeneSet",
                    record.get('id'), record.get('isPublic'), record.get('isShared'), record
                        .get('userCanWrite'), null, null, null, record.get('usedOwned'));
                return result;
            },

            tooltip: 'Click to edit permissions'
        }

    ],

    /**
     * Called by outside when adding members.
     *
     * @param {}
     *           store
     */
    updateMembers: function (store) {
        var rec = this.getSelectionModel().getSelected();

        if (!rec) {
            // if no set is currently selected.
            Ext.Msg.alert("Sorry", "You must select a set or create a new set before adding genes.", function () {
                store.un("remove", this.updateMembers);
                store.un("add", this.updateMembers);
                store.removeAll();
            });
            return;
        }

        var ids = [];
        store.each(function (rec) {
            ids.push(rec.get("id"));
        });
        rec.set("geneIds", ids);
        rec.set("size", ids.length);
        this.fireEvent("dirty", rec);
    }
});

/**
 *
 * @param {}
 *           config
 */
Gemma.GeneGroupStore = function (config) {

    /*
     * Leave this here so copies of records can be constructed.
     */
    this.record = Ext.data.Record.create([{
        name: "id",
        type: "int"
    }, {
        name: "name",
        type: "string",
        convert: function (v, rec) {
            if (v.lastIndexOf("GO", 0) == 0) {
                return rec.description;
            }
            return v;
        }
    }, {
        name: "description",
        type: "string",
        convert: function (v, rec) {
            if (rec.name.lastIndexOf("GO", 0) == 0) {
                return rec.name;
            }
            return v;
        }

    }, {
        name: "isPublic",
        type: "boolean"
    }, {
        name: "size",
        type: "int"
    }, {
        name: "isShared",
        type: 'boolean'
    }, {
        name: "taxonName"
    }, {
        name: "taxonId"
    }, {
        name: "userCanWrite",
        type: 'boolean'
    }, {
        name: "userOwned",
        type: 'boolean'
    }, {
        name: "geneIds"
    }]);

    this.reader = new Ext.data.ListRangeReader({
        id: "id"
    }, this.record);

    Gemma.GeneGroupStore.superclass.constructor.call(this, config);

};

/**
 *
 * @class Gemma.GeneGroupStore
 * @extends Ext.data.Store
 */
Ext.extend(Gemma.GeneGroupStore, Ext.data.Store, {

    autoLoad: true,
    autoSave: false,
    selected: null,
    name: "geneGroupData-store",

    proxy: new Ext.data.DWRProxy({
        apiActionToHandlerMap: {
            read: {
                dwrFunction: GeneSetController.getUsersGeneGroups,
                getDwrArgsFunction: function (request) {
                    if (request.params.length > 0) {
                        return [request.params[0], request.params[1]];
                    }
                    return [false, null];
                }
            },
            create: {
                dwrFunction: GeneSetController.create
            },
            update: {
                dwrFunction: GeneSetController.update
            },
            destroy: {
                dwrFunction: GeneSetController.remove
            }
        }
    }),

    writer: new Ext.data.JsonWriter({
        writeAllFields: true
    }),

    getSelected: function () {
        return this.selected;
    },

    setSelected: function (rec) {
        this.previousSelection = this.getSelected();
        if (rec) {
            this.selected = rec;
        }
    },

    getPreviousSelection: function () {
        return this.previousSelection;
    },

    clearSelected: function () {
        this.selected = null;
        delete this.selected;
    },

    listeners: {
        write: function (store, action, result, res, rs) {
            // Ext.Msg.show({
            // title : "Saved",
            // msg : "Changes were saved",
            // icon : Ext.MessageBox.INFO
            // });
        },
        exception: function (proxy, type, action, options, res, arg) {
            // console.log(res);
            if (type === 'remote') {
                Ext.Msg.show({
                    title: 'Error',
                    msg: res,
                    icon: Ext.MessageBox.ERROR
                });
            } else {
                Ext.Msg.show({
                    title: 'Error',
                    msg: arg,
                    icon: Ext.MessageBox.ERROR
                });
            }
        }

    }

});
