Ext.namespace('Gemma');

/**
 * Combobox to display available taxa.
 * 
 * @class Gemma.TaxonCombo
 * @extends Ext.form.ComboBox
 */
Gemma.TaxonCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'commonName',
	valueField : 'id',
	editable : false,
	loadingText : "Loading ...",
	triggerAction : 'all', // so selecting doesn't hide the others
	mode : 'local', // because we load only at startup.
	listWidth : 150,
	width : 120,
	stateId : "Gemma.TaxonCombo",
	stateful : true,
	stateEvents : ['select'],
	isReady : false,

	emptyText : 'Select a taxon',

	/**
	 * Custom cookie config.
	 * 
	 * @return {}
	 */
	getState : function() {
		return ({
			taxon : this.getTaxon().data
		});
	},

	/**
	 * Custom cookie config.
	 * 
	 * @param {}
	 *            state
	 */
	applyState : function(state) {
		if (state && state.taxon) {
			// this.setTaxon(state.taxon);
			// so we wait for the load.
			// console.log("apply state");
			this.setState(state.taxon.id);
		}
	},

	setState : function(v) {
		if (this.isReady) {
			// console.log("already ready");
			this.setTaxon(v);
		} else {
			// console.log("storing state");
			this.tmpState = v;
			// wait for restoreState is called.
		}
	},

	restoreState : function() {
		if (this.tmpState) {
			// console.log("restore state");
			this.setTaxon(this.tmpState);
			delete this.tmpState;
			this.isReady = true;
			// console.log("ready");
		}
		this.fireEvent('ready', this.getTaxon().data);
	},

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "commonName",
		type : "string"
	}, {
		name : "scientificName",
		type : "string"
	}]),

	filter : function(taxon) {
		this.store.clearFilter();
		this.store.filterBy(function(record, id) {
			if (taxon.id == record.get("id")) {
				return true;
			} else {
				return false;
			}
		});
		this.setTaxon(taxon);
		this.onLoad();
	},

	initComponent : function() {

		var tmpl = new Ext.XTemplate('<tpl for="."><div class="x-combo-list-item">{commonName} ({scientificName})</div></tpl>');

		Ext.apply(this, {
			store : new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(GenePickerController.getTaxa),
				reader : new Ext.data.ListRangeReader({
					id : "id"
				}, this.record),
				remoteSort : true
			}),
			tpl : tmpl
		});
		
		this.store.load({
			params : [],
			callback : this.restoreState.createDelegate(this),
			scope : this,
			add : false
		});

		Gemma.TaxonCombo.superclass.initComponent.call(this);

		this.addEvents('ready');
	},

	getTaxon : function() {
		return this.store.getById(this.getValue());
	},

	/**
	 * To allow setting programmatically.
	 * 
	 * @param {}
	 *            taxon
	 */
	setTaxon : function(taxon) {
		if (taxon.id) {
			this.setValue(taxon.id);
		} else {
			this.setValue(taxon);
		}
	}

});
