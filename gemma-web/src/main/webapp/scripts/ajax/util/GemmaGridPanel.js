Ext.namespace('Ext.Gemma');

/**
 * 
 * @class Ext.Gemma.GemmaGridPanel
 * @extends Ext.grid.EditorGridPanel*
 * @author Luke, Paul
 * @version $Id$
 */
Ext.Gemma.GemmaGridPanel = Ext.extend(Ext.grid.EditorGridPanel, {

	stripeRows : true,
	viewConfig : {
		forceFit : true
	},

	initComponent : function() {
		if (this.height) {
			Ext.apply(this, {
				autoHeight : false
			});
		}

		if (Ext.isIE) {
			Ext.apply(this, {
				width : 800
			});
		}

		Ext.apply(this, {
			selModel : new Ext.grid.RowSelectionModel({})
		});

		Ext.Gemma.GemmaGridPanel.superclass.initComponent.call(this);

	},

	getEditedRecords : function() {
		var edited = [];
		var all = this.getStore().getRange();
		for (var i = 0; i < all.length; ++i) {
			if (all[i].dirty) {
				edited.push(all[i].data);
			}
		}
		return edited;
	},

	getSelectedRecords : function() {
		var records = [];
		var selected = this.getSelectionModel().getSelections();
		for (var i = 0; i < selected.length; ++i) {
			records.push(selected[i].data);
		}
		return records;
	},

	getSelectedIds : function() {
		var ids = [];
		var selected = this.getSelectionModel().getSelections();
		for (var i = 0; i < selected.length; ++i) {
			ids.push(selected[i].id);
		}
		return ids;
	},

	refresh : function(params) {
		var reloadOpts = {
			callback : this.getView().refresh.bind(this.getView())
		};
		if (params) {
			reloadOpts.params = params;
		}
		this.getStore().reload(reloadOpts);
	},

	revertSelected : function() {
		var selected = this.getSelectionModel().getSelections();
		for (var i = 0; i < selected.length; ++i) {
			selected[i].reject();
		}
		this.getView().refresh();
	},

	getReadParams : function() {
		return (typeof this.readParams == "function")
				? this.readParams()
				: this.readParams;
	}

});

/*
 * static methods
 */
Ext.Gemma.GemmaGridPanel.formatTermWithStyle = function(value, uri) {
	var style = uri ? "unusedWithUri" : "unusedNoUri";
	var description = uri || "free text";
	return String.format("<span class='{0}' ext:qtip='{2}'>{1}</span>", style,
			value, description);
};