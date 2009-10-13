Ext.namespace('Gemma');

/**
 * Combobox to display available taxa.
 * 
 * @class Gemma.TaxonCombo
 * @extends Ext.form.ComboBox
 */
Gemma.TaxonCombo = Ext.extend(Ext.form.ComboBox, {

	name : "taxcomb",

	displayField : 'commonName',
	valueField : 'id',
	editable : false,
	loadingText : "Loading ...",
	triggerAction : 'all', // so selecting doesn't hide the others
	mode : 'local', // because we load only at startup.
	listWidth : 250,
	width : 120,
	stateId : "Gemma.TaxonCombo",
	stateful : true,
	stateEvents : ['select'],
	isReady : false,

	emptyText : 'Select a taxon',
	// this allows filtering of taxon
	isDisplayTaxonSpecies : false,
	isDisplayTaxonWithGenes : false,

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
			// so we wait for the load.
			this.setState(state.taxon.id);
		}
	},

	setState : function(v) {
		if (this.isReady) {
			this.setTaxon(v);
		} else {
			this.tmpState = v;
			// wait for restoreState is called.
		}
	},

	/**
	 * Called after loading.
	 */
	restoreState : function() {

		if (this.tmpState) {
			this.setTaxon(this.tmpState);
			delete this.tmpState;
		}
		this.isReady = true;

		if (this.getTaxon()) {
			this.fireEvent('ready', this.getTaxon().data);
		} else {
			// FIXME this happens with Diff but not Coexp. [???]
			this.fireEvent('ready');
		}

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
		// option to either display all taxa, those taxa that are a species or those taxa that have genes.
		if (this.isDisplayTaxonSpecies) {
			proxyTaxon = new Ext.data.DWRProxy(GenePickerController.getTaxaSpecies);
		} else if (this.isDisplayTaxonWithGenes) {
			proxyTaxon = new Ext.data.DWRProxy(GenePickerController.getTaxaWithGenes);
		} else {
			proxyTaxon = new Ext.data.DWRProxy(GenePickerController.getTaxa);
		}

		Ext.apply(this, {
					store : new Ext.data.Store({
								proxy : proxyTaxon,
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record),
								remoteSort : false,
								sortInfo : {
									field : 'commonName'
								}
							}),
					tpl : tmpl
				});

		Gemma.TaxonCombo.superclass.initComponent.call(this);

		this.addEvents('ready');

		this.store.load({
					params : [],
					callback : this.restoreState.createDelegate(this),
					scope : this,
					add : false
				});
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
	},

	/**
	 * returns complete taxon object that matches the common name given if successful. Else return -1.
	 * 
	 * @param {commonName}
	 *            the common name of the taxon
	 * 
	 */
	setTaxonByCommonName : function(commonName) {
		var records = this.store.getRange();

		if (!records || records.size() < 1) {
			return -1;
		}

		var taxonId = -1;
		for (var i = 0; i < records.size(); i++) {
			if (records[i].data.commonName === commonName) {
				this.setTaxon(records[i].data.id);
				return records[i].data;
			}
		}

		return -1;

	}

});
