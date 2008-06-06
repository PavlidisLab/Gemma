Ext.namespace("Ext.Gemma");

/**
 * 
 * @class Ext.Gemma.AnalysisCombo
 * @extends Ext.form.ComboBox
 * @deprecated
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
		if (this.ready) {
			Ext.Gemma.AnalysisCombo.superclass.setValue.call(this, v);
		} else {
			this.state = v;
		}
	},

	restoreState : function() {
		if (this.state) {
			Ext.Gemma.AnalysisCombo.superclass.setValue.call(this, v);
			delete this.state;
		}
		this.setValue(this.state);
		delete this.state;
		this.ready = true;
		this.fireEvent('ready');
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
				this.showCustom();
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

		this.addEvents('analysischanged', 'ready');
	},

	setValue : function(v) {
		var changed = false;
		if (this.getValue() != v) {
			changed = true;
		}

		// if setting to a filtered value, reset the filter.
		if (changed && this.store.isFiltered()) {
			this.store.clearFilter();
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
		this.applyFilter(taxon);
	},

	applyFilter : function(taxon) {
		this.store.filterBy(function(record, id) {
			if (!record.data.taxon) {
				return true;
			} else if (record.data.taxon.id == taxon.id) {
				return true;
			}
		});
	},

	showCustom : function(scrollIntoView) {
		var rec = this.store.getById(-1);

		if (!rec) {
			var Constructor = this.record;
			var newrec = new Constructor({
				id : -1,
				name : "[Custom]",
				description : "Unnamed dataset grouping selected by user"
			}, -1);
			if (scrollIntoView) {
				this.store.on("add", this.selectById, this, {
					id : -1
				});
			}
			this.store.add(newrec); // asynch
		} else if (scrollIntoView) {
			this.selectById(-1);
		}

	},

	/**
	 * Given the id (primary key in Gemma) of the analysis, select it.
	 * 
	 * @param {}
	 *            args
	 */
	selectById : function(args) {
		this.store.un("add", this.selectById);
		if (args.id) {
			this.selectByValue(args.id, true);
		} else {
			this.selectByValue(args, true);
		}
	},

	clearCustom : function() {
		var rec = this.store.getById(-1);
		if (rec) {
			this.store.remove(rec);
		}
	}

});

Ext.reg('analysiscombo', Ext.Gemma.AnalysisCombo);