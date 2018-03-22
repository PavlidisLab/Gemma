Ext.namespace('Gemma');

/**
 *
 * @class Gemma.GemmaGridPanel
 * @extends Ext.grid.EditorGridPanel
 * @author Luke, Paul
 */
Gemma.GemmaGridPanel = Ext.extend(Ext.grid.EditorGridPanel, {
    stripeRows: true,
    stateful: false,
    viewConfig: {
        forceFit: true
    },

    /**
     * @memberOf Gemma.GemmaGridPanel
     * @Override
     * @private
     */
    initComponent: function () {
        Ext.apply(this, {
            selModel: new Ext.grid.RowSelectionModel({})
        });
        Gemma.GemmaGridPanel.superclass.initComponent.call(this);
        this.addEvents('refresh');
    },

    getEditedRecords: function () {
        var edited = [];
        var all = this.getStore().getRange();
        for (var i = 0; i < all.length; ++i) {
            if (all[i].dirty) {
                edited.push(all[i].data);
            }
        }
        return edited;
    },

    getSelectedRecords: function () {
        var records = [];
        var selected = this.getSelectionModel().getSelections();
        for (var i = 0; i < selected.length; ++i) {
            records.push(selected[i].data);
        }
        return records;
    },

    getSelectedIds: function () {
        var ids = [];
        var selected = this.getSelectionModel().getSelections();
        for (var i = 0; i < selected.length; ++i) {
            ids.push(selected[i].id);
        }
        return ids;
    },

    refresh: function (params) {
        var reloadOpts = {
            callback: this.getView().refresh.createDelegate(this.getView())
        };
        if (params) {
            reloadOpts.params = params;
        }
        this.getStore().reload(reloadOpts);
        this.fireEvent('refresh');
    },

    revertSelected: function () {
        var selected = this.getSelectionModel().getSelections();
        for (var i = 0; i < selected.length; ++i) {
            selected[i].reject();
        }
        this.getView().refresh();
    }

});

/*
 * static methods
 */
Gemma.GemmaGridPanel.formatTermWithStyle = function (value, uri) {
    var style = uri ? "unusedWithUri" : "unusedNoUri"; // typo.css
    var description = uri || "free text";
    return String.format("<span class='{0}' ext:qtip='{2}'>{1}</span>", style, value, description);
};