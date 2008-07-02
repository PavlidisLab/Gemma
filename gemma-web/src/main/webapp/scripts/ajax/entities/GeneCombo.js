/*
 * @version : $Id$
 * 
 */
Ext.namespace('Gemma');

/**
 * Live search field for genes.
 * 
 * @class Gemma.GeneCombo
 * @extends Ext.form.ComboBox
 */
Gemma.GeneCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'officialSymbol',
	valueField : 'id',
	width : 140,// default.
	listWidth : 450, // ridiculously large so IE displays it properly
	// (usually)

	loadingText : 'Searching...',
	emptyText : "Search for a gene",
	minChars : 1,
	selectOnFocus : true,

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "taxon"
	}, {
		name : "officialSymbol",
		type : "string"
	}, {
		name : "officialName",
		type : "string"
	}]),

	initComponent : function() {

		var template = new Ext.XTemplate('<tpl for="."><div style="font-size:11px" class="x-combo-list-item" ext:qtip="{officialName} ({[values.taxon.scientificName]})">{officialSymbol} {officialName} ({[values.taxon.scientificName]})</div></tpl>');

		Ext.apply(this, {
			tpl : template,
			store : new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(GenePickerController.searchGenes),
				reader : new Ext.data.ListRangeReader({
					id : "id"
				}, this.record),
				sortInfo : {
					field : "officialSymbol",
					dir : "ASC"
				}
			})
		});

		Gemma.GeneCombo.superclass.initComponent.call(this);

		this.addEvents('genechanged');

		this.store.on("datachanged", function() {
			if (this.store.getCount() === 0) {
				this.fireEvent("invalid", "No matching genes");
				this.emptyText = "Nothing found";
				this.clearValue();
			}
		}, this);
	},

	onSelect : function(record, index) {
		Gemma.GeneCombo.superclass.onSelect.call(this, record, index);
		if (!this.selectedGene || record.data.id != this.selectedGene.id) {
			this.setGene(record.data);
			this.fireEvent('select', this, this.selectedGene);
		}
	},

	/**
	 * Parameters for AJAX call.
	 * 
	 * @param {}
	 *            query
	 * @return {}
	 */
	getParams : function(query) {
		return [query, this.taxon ? this.taxon.id : -1];
	},

	getGene : function() {
		return this.selectedGene;
	},

	setGene : function(gene) {
		if (this.tooltip) {
			this.tooltip.destroy();
		}
		if (gene) {
			this.selectedGene = gene;
			this.taxon = gene.taxon;
			this.tooltip = new Ext.ToolTip({
				target : this.getEl(),
				html : String.format('{0} ({1})', gene.officialName
						|| "no description", gene.taxon.scientificName)
			});
		}
	},

	getTaxon : function() {
		return this.taxon;
	},

	setTaxon : function(taxon) {
		if (!this.taxon || this.taxon.id != taxon.id) {
			//console.log("taxon =" + taxon.id);
			this.taxon = taxon;
			delete this.selectedGene;
			if (this.tooltip) {
				this.tooltip.destroy();
			}
			this.reset();

			/*
			 * this is to make sure we always search again after a taxon change,
			 * in case the user searches for the same gene. Otherwise Ext just
			 * keeps the old results.
			 */
			this.lastQuery = null;

		}
	}

});
