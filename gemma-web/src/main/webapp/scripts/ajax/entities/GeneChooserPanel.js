/*
 * 
 * Version : $Id$
 * Author : luke, paul
 */
Ext.namespace('Gemma');

/**
 * Widget that allows user to search for and select one or more genes from the
 * database. The selected genes are kept in a table which can be edited. This
 * component is the top part of the coexpression interface, but should be
 * reusable.
 * 
 * @class Gemma.GeneChooserPanel
 * @extends Gemma.GemmaGridPanel
 */
Gemma.GeneChooserPanel = Ext.extend(Gemma.GemmaGridPanel, {

	collapsible : true,

	height : 200,
	autoScroll : true,
	emptyText : "Genes will be listed here",

	columns : [{
		header : 'Symbol',
		dataIndex : 'officialSymbol',
		sortable : true
	}, {
		id : 'desc',
		header : 'Name',
		dataIndex : 'officialName'
	}],
	autoExpandColumn : 'desc',

	/**
	 * Given text, search Gemma for matching genes. Used to 'bulk load' genes
	 * from the GUI.
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
				}.createDelegate(this));
	},

	/**
	 * Add to table.
	 * 
	 * @param {}
	 *            geneIds
	 * @param {}
	 *            callback
	 * @param {}
	 *            args
	 */
	loadGenes : function(geneIds, callback, args) {
		GenePickerController.getGenes(geneIds, function(genes) {
			var geneData = [];
			for (var i = 0; i < genes.length; ++i) {
				geneData.push([genes[i].id, genes[i].taxon.scientificName,
						genes[i].officialSymbol, genes[i].officialName]);
			}
			this.getStore().loadData(geneData);
			if (callback) {
				callback(args);
			}
		}.createDelegate(this));
	},

	/**
	 * Set value in combobox.
	 * 
	 * @param {}
	 *            geneId
	 * @param {}
	 *            callback
	 * @param {}
	 *            args
	 */
	setGene : function(geneId, callback, args) {
		GenePickerController.getGenes([geneId], function(genes) {
			var g = genes[0];
			if (g) {
				this.geneCombo.setGene(g);
				this.geneCombo.setValue(g.officialSymbol);
				this.getStore().removeAll();
				this.addButton.enable();
			}
			if (callback) {
				callback(args);
			}
		}.createDelegate(this));
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
		} else {
			return this.geneCombo.getTaxon().id;
		}
	},

	/**
	 * Check if the taxon needs to be changed, and if so, update it for the
	 * taxoncombo and the genecombo.
	 * 
	 * @param {}
	 *            taxon
	 */
	taxonChanged : function(taxon) {
		if (!taxon) {
			return;
		}

		var oldtax = this.geneCombo.getTaxon();

		// Update the genecombo and the table.
		if (!oldtax || oldtax.id != taxon.id) {
			this.geneCombo.setTaxon(taxon);
			this.fireEvent("taxonchanged", taxon);
		}

		// Remove all the genes that are not from the correct taxon.
		var all = this.getStore().getRange();
		for (var i = 0; i < all.length; ++i) {
			if (all[i].data.taxon.id != taxon.id) {
				this.getStore().remove(all[i]);
			}
		}

		// Update the taxon combo.
		if (!this.taxonCombo.getTaxon()
				|| this.taxonCombo.getTaxon().id != taxon.id) {
			this.taxonCombo.setTaxon(taxon);
		}

	},

	addGene : function() {
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
	},

	removeGene : function() {
		var selected = this.getSelectionModel().getSelections();
		for (var i = 0; i < selected.length; ++i) {
			this.getStore().remove(selected[i]);
		}
		this.removeButton.disable();
	},

	initComponent : function() {

		this.taxonCombo = new Gemma.TaxonCombo({
			listeners : {
				'select' : function(cb, rec, index) {
					this.taxonChanged(rec.data);
				}.createDelegate(this)
			}
		});

		this.geneCombo = new Gemma.GeneCombo({
			emptyText : 'Search for a gene',
			listeners : {
				'select' : {
					fn : function(combo, rec, index) {
						if (rec.get) {
							this.taxonCombo.setTaxon(rec.get("taxon"));
						} else {
							this.taxonCombo.setTaxon(rec.taxon);
						}
						this.addButton.enable();
					}.createDelegate(this)
				}
			}
		});

		this.addButton = new Ext.Toolbar.Button({
			icon : "/Gemma/images/icons/add.png",
			cls : "x-btn-icon",
			tooltip : "Add a gene to the list",
			disabled : true,
			handler : this.addGene.createDelegate(this)
		});

		this.removeButton = new Ext.Toolbar.Button({
			icon : "/Gemma/images/icons/subtract.png",
			cls : "x-btn-icon",
			tooltip : "Remove the selected gene from the list",
			disabled : true,
			handler : this.removeGene.createDelegate(this)
		});

		this.chooser = new Gemma.GeneImportPanel({
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

		Ext.apply(this, {
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
					name : 'taxon'
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
			})
		});

		Gemma.GeneChooserPanel.superclass.initComponent.call(this);

		this.addEvents('taxonchanged');

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

Ext.reg('genechooser', Gemma.GeneChooserPanel);
