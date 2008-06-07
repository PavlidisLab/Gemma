/*
 * @version : $Id$
 * 
 */
Ext.namespace('Ext.Gemma');

/**
 * Live search field for genes.
 * 
 * @class Ext.Gemma.GeneCombo
 * @extends Ext.form.ComboBox
 */
Ext.Gemma.GeneCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'officialSymbol',
	valueField : 'id',
	width : 140,// default.
	listWidth : 350,
	loadingText : 'Searching...',
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

		var template = new Ext.XTemplate('<tpl for="."><div ext:qtip="{officialName} ({taxon.scientificName})" class="x-combo-list-item">{officialSymbol} {officialName} ({taxon.scientificName})</div></tpl>');

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

		Ext.Gemma.GeneCombo.superclass.initComponent.call(this);
		this.addEvents('genechanged');

		this.store.on("datachanged", function() {
			if (this.store.getCount() === 0) {
				this.fireEvent("invalid", "No matching genes");
				this.setRawValue("No matching genes");
			}
		}, this);
	},

	onSelect : function(record, index) {
		Ext.Gemma.GeneCombo.superclass.onSelect.call(this, record, index);

		if (this.selectedGene && record.data.id != this.selectedGene.id) {
			this.setGene(record.data);
			this.fireEvent('genechanged', this, this.selectedGene);
		}
	},

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
						|| "no description", gene.taxon)
			});
		}
	},

	getTaxon : function() {
		return this.taxon;
	},

	setTaxon : function(taxon) {
		if (this.taxon && this.taxon.id != taxon.id) {
			delete this.selectedGene;
			if (this.tooltip) {
				this.tooltip.destroy();
			}
			this.reset();
		}
		this.taxon = taxon;
	}

});
