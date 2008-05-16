Ext.namespace("Ext.Gemma");

/**
 * 
 * @class Ext.Gemma.AnalysisCombo
 * @extends Ext.form.ComboBox
 */
Ext.Gemma.AnalysisCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'name',
	valueField : 'id',
	editable : false,
	loadingText : "Loading ...",
	listWidth : 250,
	forceSelection : true,
	mode : 'local',
	triggerAction : 'all',
	emptyText : 'Select a search scope',

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
		name : "taxon"
	}, {
		name : "numDatasets",
		type : "int"
	}, {
		name : "datasets"
	}]),

	setState : function(v) {
		this.state = v;
	},

	restoreState : function() {
		this.setValue(this.state);
	},

	initComponent : function() {

		var templ = new Ext.XTemplate('<tpl for="."><div ext:qtip="{description}" class="x-combo-list-item">{name}{[ values.taxon ? " (" + values.taxon.scientificName + ")" : "" ]}</div></tpl>');

		Ext.apply(this, {
			store : new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(ExtCoexpressionSearchController.getCannedAnalyses),
				reader : new Ext.data.ListRangeReader({
					id : "id"
				}, this.record),
				remoteSort : true
			}),
			tpl : templ
		});

		var customAnalysisCallback = function(r, options, success) {
			if (this.showCustomOption) {
				var Constructor = this.record;
				var newrec = new Constructor({
					id : -1,
					name : "Custom analysis",
					description : "Select specific datasets to search against"
				}, -1);
				this.store.add(newrec); // asynch
			}
			this.restoreState();
		}.createDelegate(this);

		Ext.Gemma.AnalysisCombo.superclass.initComponent.call(this);

		this.store.load({
			params : [],
			callback : customAnalysisCallback,
			scope : this,
			add : false
		});

		this.doQuery();

		this.addEvents('analysischanged');
	},

	setValue : function(v) {
		var changed = false;
		if (this.getValue() != v) {
			changed = true;
		}

		Ext.Gemma.AnalysisCombo.superclass.setValue.call(this, v);

		if (changed) {
			this.fireEvent('analysischanged', this.getAnalysis());
		}
	},

	getAnalysis : function() {
		var analysis = this.store.getById(this.getValue());
		return analysis;
	},

	taxonChanged : function(taxon) {
		if (this.getAnalysis() && this.getAnalysis().taxon
				&& this.getAnalysis().taxon.id != taxon.id) {
			this.reset();
		}
		this.store.filterBy(function(record, id) {
			if (!record.data.taxon) {
				return true;
			} else if (record.data.taxon.id == taxon.id) {
				return true;
			}
		});
	}

});

Ext.reg('analysiscombo', Ext.Gemma.AnalysisCombo);