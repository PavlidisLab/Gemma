/*
 * 
 * Version : $Id$
 * Author : luke, paul
 */
Ext.namespace('Ext.Gemma');

/**
 * Widget that allows user to search for and select one or more genes from the
 * database. The selected genes are kept in a table which can be edited. This
 * component is the top part of the coexpression interface, but should be
 * reusable.
 * 
 * @class Ext.Gemma.GeneChooserPanel
 * @extends Ext.Gemma.GemmaGridPanel
 */
Ext.Gemma.GeneChooserPanel = Ext.extend(Ext.Gemma.GemmaGridPanel, {

	collapsible : true,

	/**
	 * Given text, search Gemma for matching genes.
	 * 
	 * @param {}
	 *            e
	 */
	getGenesFromList : function(e) {
		var taxonId = this.getTaxonId();
		var text = e.geneNames;
		GenePickerController.searchMultipleGenes(text, taxonId,
				function(genes) {
					var geneData = [];
					for (var i = 0; i < genes.length; ++i) {
						geneData
								.push([genes[i].id,
										genes[i].taxon.scientificName,
										genes[i].officialSymbol,
										genes[i].officialName]);
					}
					this.getStore().loadData(geneData, true);
				}.createDelegate(this, [], true));
	},

	loadGenes : function(geneIds, callback) {
		GenePickerController.getGenes(geneIds, function(genes) {
			var geneData = [];
			for (var i = 0; i < genes.length; ++i) {
				geneData.push([genes[i].id, genes[i].taxon.scientificName,
						genes[i].officialSymbol, genes[i].officialName]);
			}
			this.getStore().loadData(geneData);
			if (callback) {
				callback();
			}
		}.createDelegate(this, [], true));
	},

	setGene : function(geneId, callback) {
		GenePickerController.getGenes([geneId], function(genes) {
			var g = genes[0];
			if (g) {
				g.taxon = g.taxon.scientificName;
				this.geneCombo.setGene(g);
				this.geneCombo.setValue(g.officialSymbol);
				this.getStore().removeAll();
				this.addButton.enable();
			}
			if (callback) {
				callback();
			}
		}.createDelegate(this, [], true));
	},

	getGeneIds : function() {
		var ids = [];
		var all = this.getStore().getRange();
		for (var i = 0; i < all.length; ++i) {
			ids.push(all[i].data.id);
		}
		var gene = this.geneCombo.getGene();
		if (gene) {
			for (var i = 0; i < ids.length; ++i) {
				if (ids[i] == gene.id) {
					return ids;
				}
			}
			ids.push(gene.id);
		}
		return ids;
	},

	getTaxonId : function() {
		if (this.taxonCombo) {
			return this.taxonCombo.getValue();
		}
	},

	taxonChanged : function(taxon) {
		var oldtax = this.geneCombo.getTaxon();

		if (oldtax && oldtax.id == taxon.id) {
			return;
		}

		this.geneCombo.setTaxon(taxon);
		var all = this.getStore().getRange();
		for (var i = 0; i < all.length; ++i) {
			if (all[i].data.taxon != taxon.scientificName) {
				this.getStore().remove(all[i]);
			}
		}
		this.fireEvent('taxonchanged', taxon);
	},

	initComponent : function() {

		this.addEvents('taxonchanged');

		this.taxonCombo = new Ext.Gemma.TaxonCombo({
			listeners : {
				'taxonchanged' : {
					fn : this.taxonChanged.createDelegate(this, [], true)
				}
			}
		});

		this.geneCombo = new Ext.Gemma.GeneCombo({
			emptyText : 'Search for a gene',
			listeners : {
				'select' : {
					fn : function(combo, record, index) {
						var actualTaxon = this.taxonCombo
								.getTaxonByScientificName(record.data.taxon);
						this.taxonCombo.setTaxon(actualTaxon);
						this.addButton.enable();
					}.createDelegate(this, [], true)
				}
			}
		});

		this.addButton = new Ext.Toolbar.Button({
			icon : "/Gemma/images/icons/add.png",
			cls : "x-btn-icon",
			tooltip : "Add a gene to the list",
			disabled : true,
			handler : function() {
				var gene = this.geneCombo.getGene();
				if (!gene) {
					return;
				}
				if (this.getStore().find("id", gene.id) < 0) {
					var Constructor = this.geneCombo.record;
					var record = new Constructor(gene);
					this.getStore().add([record]);
				}
				this.geneCombo.reset();
				this.addButton.disable();
			}.createDelegate(this, [], true)
		});

		this.removeButton = new Ext.Toolbar.Button({
			icon : "/Gemma/images/icons/subtract.png",
			cls : "x-btn-icon",
			tooltip : "Remove the selected gene from the list",
			disabled : true,
			handler : function() {
				var selected = this.getSelectionModel().getSelections();
				for (var i = 0; i < selected.length; ++i) {
					this.getStore().remove(selected[i]);
				}
				this.removeButton.disable();
			}.createDelegate(this, [], true)
		});

		this.chooser = new Ext.Gemma.GeneImportPanel({
			listeners : {
				'commit' : {
					fn : this.getGenesFromList.createDelegate(this)
				}
			}
		});

		this.multiButton = new Ext.Toolbar.Button({
			icon : "/Gemma/images/icons/page_white_put.png",
			cls : "x-btn-icon",
			tooltip : "Import multiple genes",
			disabled : false,
			handler : function() {
				this.geneCombo.reset();
				this.addButton.enable();
				this.chooser.show();
			}.createDelegate(this, [], true)
		});

		/*
		 * establish default config options...
		 */
		Ext.apply(this, {
			height : 200,
			autoScroll : true,
			emptyText : "Genes will be listed here",
			tbar : [this.taxonCombo, {
				xtype : 'tbspacer'
			}, this.geneCombo, this.addButton, {
				xtype : 'tbspacer'
			}, this.removeButton, {
				xtype : 'tbspacer'
			}, this.multiButton],
			store : new Ext.data.SimpleStore({
				fields : [{
					name : 'id',
					type : 'int'
				}, {
					name : 'taxon',
					type : 'string'
				}, {
					name : 'officialSymbol',
					type : 'string'
				}, {
					name : 'officialName',
					type : 'string'
				}],
				sortInfo : {
					field : 'officialSymbol',
					direction : 'ASC'
				}
			}),
			columns : [{
				header : 'Symbol',
				dataIndex : 'officialSymbol',
				sortable : true
			}, {
				id : 'desc',
				header : 'Name',
				dataIndex : 'officialName'
			}],
			autoExpandColumn : 'desc'
		});

		Ext.Gemma.GeneChooserPanel.superclass.initComponent.call(this);

		/*
		 * code down here has to be called after the super-constructor so that
		 * we know we're a grid...
		 */
		this.getSelectionModel().on("selectionchange", function(model) {
			var selected = model.getSelections();
			if (selected.length > 0) {
				this.removeButton.enable();
			} else {
				this.removeButton.disable();
			}
		}.createDelegate(this));

		if (this.genes) {
			var genes = this.genes instanceof Array ? this.genes : this.genes
					.split(",");
			this.loadGenes(genes);
		}

		this.getStore().on("load", function() {
			this.doLayout();
		}, this);

	}
});

/*
 * instance methods...
 */

Ext.reg('genechooser', Ext.Gemma.GeneChooserPanel);
