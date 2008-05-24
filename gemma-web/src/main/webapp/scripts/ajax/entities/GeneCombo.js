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
		name : "taxon",
		type : "string",
		convert : function(t) {
			return t.scientificName;
		}
	}, {
		name : "officialSymbol",
		type : "string"
	}, {
		name : "officialName",
		type : "string"
	}]),

	initComponent : function() {

		var template = new Ext.XTemplate('<tpl for="."><div ext:qtip="{officialName} ({taxon})" class="x-combo-list-item">{officialSymbol} {officialName} ({taxon})</div></tpl>');

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
		this.setGene(record.data);
		if (record.data.id != this.selectedGene.id) {
			this.selectedGene = record.data;
			this.fireEvent('genechanged', this, this.selectedGene);
		}
	},

	reset : function() {
		Ext.Gemma.GeneCombo.superclass.reset.call(this);
		this.setGene(null);
	},

	getParams : function(query) {
		return [query, this.taxon ? this.taxon.id : -1];
	},

	getGene : function() {
		return this.selectedGene;
	},

	setGene : function(gene) {
		this.selectedGene = gene;
		if (this.tooltip) {
			this.tooltip.destroy();
		}
		if (gene) {
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
		this.taxon = taxon;
		// If taxon has changed, clear.
		if (this.selectedGene && this.selectedGene.taxon.id != taxon.id) {
			this.setGene(null);
			this.reset();
			this.lastQuery = '';
		}
	}

});
