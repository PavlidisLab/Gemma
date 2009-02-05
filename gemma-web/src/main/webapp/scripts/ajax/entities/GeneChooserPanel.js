/*
 * GeneGrid, GeneChooserPanel, GeneChooserToolbar. Widget for picking genes.
 * 
 * Version : $Id$ Author : luke, paul
 */
Ext.namespace('Gemma');

/**
 * The maximum number of genes we allow users to put in at once.
 * 
 * @type Number
 */
Gemma.MAX_GENES_PER_QUERY = 20;

/**
 * Widget that allows user to search for and select one or more genes from the database. The selected genes are kept in
 * a table which can be edited. This component is the top part of the coexpression interface, but should be reusable.
 * 
 * @class Gemma.GeneChooserPanel
 * @extends Gemma.GemmaGridPanel
 */

Gemma.GeneGrid = Ext.extend(Ext.grid.GridPanel, {

			collapsible : true,
			autoWidth : true,
			stateful : false,
			frame : true,
			layout : 'fit',
			viewConfig : {
				forceFit : true,
				emptyText : "Multiple genes can be listed here"
			},
			autoScroll : true,
			columns : [{
						header : 'Symbol',
						toolTip : 'Gene symbol',
						dataIndex : 'officialSymbol',
						width : 75,
						sortable : true
					}, {
						id : 'desc',
						toolTip : 'Gene name',
						header : 'Name',
						dataIndex : 'officialName'
					}],
			autoExpandColumn : 'desc',

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
								geneData.push([genes[i].id, genes[i].taxon.scientificName, genes[i].officialSymbol,
										genes[i].officialName]);
							}
							/*
							 * FIXME this can result in the same gene listed twice. This is taken care of at the server
							 * side but looks funny.
							 */
							this.getStore().loadData(geneData);
							if (callback) {
								callback(args);
							}
						}.createDelegate(this));
			},

			initComponent : function() {
				Ext.apply(this, {
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

				Gemma.GeneGrid.superclass.initComponent.call(this);

				if (this.genes) {
					var genes = this.genes instanceof Array ? this.genes : this.genes.split(",");
					this.loadGenes(genes);
				}

			},

			removeGene : function() {
				var selected = this.getSelectionModel().getSelections();
				for (var i = 0; i < selected.length; ++i) {
					this.getStore().remove(selected[i]);
				}
			},

			record : Ext.data.Record.create([{
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
					}]),

			addGene : function(gene) {
				if (!gene) {
					return;
				}

				if (this.getStore().find("id", gene.id) < 0) {
					var Constructor = this.record;
					var record = new Constructor(gene);
					this.getStore().add([record]);
				}
			},

			/**
			 * Given text, search Gemma for matching genes. Used to 'bulk load' genes from the GUI.
			 * 
			 * @param {}
			 *            e
			 */
			getGenesFromList : function(e, taxon) {
				var taxonId = taxon.id;
				var text = e.geneNames;
				GenePickerController.searchMultipleGenes(text, taxonId, function(genes) {
							var geneData = [];
							var warned = false;
							for (var i = 0; i < genes.length; ++i) {
								if (i >= Gemma.MAX_GENES_PER_QUERY) {
									if (!warned) {
										Ext.Msg.alert("Too many genes", "You can only search up to "
														+ Gemma.MAX_GENES_PER_QUERY
														+ " genes, some of your selections will be ignored.");
										warned = true;
									}
									break;
								}
								geneData.push([genes[i].id, genes[i].taxon.scientificName, genes[i].officialSymbol,
										genes[i].officialName]);

							}
							this.getStore().loadData(geneData, true);
						}.createDelegate(this));
			}

		});

Gemma.GeneChooserToolBar = Ext.extend(Ext.Toolbar, {

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

			getTaxonId : function() {
				if (this.taxonCombo) {
					return this.taxonCombo.getValue();
				} else {
					return this.geneCombo.getTaxon().id;
				}
			},

			/**
			 * Check if the taxon needs to be changed, and if so, update it for the taxoncombo and the genecombo.
			 * 
			 * @param {}
			 *            taxon
			 */
			taxonChanged : function(taxon, updateTaxonCombo) {
				
				
				if (!taxon) {
					return;
				}
				
				// Update the genecombo and the table.
				if (!updateTaxonCombo) {
					this.geneCombo.setTaxon(taxon);
					
					//update genecombo list box
					var all = this.getStore().getRange();
					
					for (var i = 0; i < all.length; ++i) {						
						if (all[i].data.taxon.id != taxon.id) {
							this.getStore().remove(all[i]);
						}
					}
					
					this.fireEvent("taxonchanged", taxon); 
					
				}
				else{
					// Update the taxon combo.
					this.taxonCombo.setTaxon(taxon);
				}

			},

			getTaxon : function() {
				return this.taxonCombo.getTaxon();
			},

			getGenesFromList : function(e) {
				this.geneGrid.getGenesFromList(e, this.getTaxon());
			},

			getStore : function() {
				return this.geneGrid.getStore();
			},

			initComponent : function() {

				Gemma.GeneChooserToolBar.superclass.initComponent.call(this);

				this.taxonCombo = new Gemma.TaxonCombo({
							listeners : {
								'select' : {
									fn : function(cb, rec, index) {
										this.taxonChanged(rec.data, false);
									}.createDelegate(this)
								},
								'ready' : {
									fn : function(taxon) {
										this.taxonChanged(taxon, false);
										this.fireEvent('ready', taxon);
									}.createDelegate(this)
								}
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
							handler : function() {
								this.geneGrid.addGene(this.geneCombo.getGene());
								this.geneCombo.reset();
								this.addButton.disable();
							}.createDelegate(this)
						});

				this.removeButton = new Ext.Toolbar.Button({
							icon : "/Gemma/images/icons/subtract.png",
							cls : "x-btn-icon",
							tooltip : "Remove the selected gene from the list",
							disabled : true,
							handler : function() {
								this.geneGrid.removeGene();
								this.removeButton.disable();
							}.createDelegate(this)
						});

				this.chooser = new Gemma.GeneImportPanel({
							listeners : {
								'commit' : {
									fn : this.getGenesFromList.createDelegate(this),
									scope : this
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
				 * code down here has to be called after the super-constructor so that we know we're a grid...
				 */
				this.geneGrid.getSelectionModel().on("selectionchange", function(model) {
							var selected = model.getSelections();
							if (selected.length > 0) {
								this.removeButton.enable();
							} else {
								this.removeButton.disable();
							}
						}.createDelegate(this));

				this.addEvents("taxonchanged", "ready");
			},

			afterRender : function(c, l) {
				Gemma.GeneChooserToolBar.superclass.afterRender.call(this, c, l);

				this.add(this.taxonCombo);
				this.addSpacer();
				this.add(this.geneCombo, this.addButton);
				this.addSpacer();
				this.add(this.removeButton);
				this.addSpacer();
				this.add(this.multiButton);

			}

		});

/**
 * 
 * @class Gemma.GeneChooserPanel
 * @extends Ext.Panel
 */
Gemma.GeneChooserPanel = Ext.extend(Ext.Panel, {

			layout : 'border',

			initComponent : function() {

				this.geneGrid = new Gemma.GeneGrid({
							height : 100,
							region : 'center',
							frame : false,
							style : "border: #a3bad9 solid 1px;",
							split : true
						});

				this.toolbar = new Gemma.GeneChooserToolBar({
							geneGrid : this.geneGrid,
							style : "border: #a3bad9 solid 1px;",
							listeners : {
								'taxonchanged' : {
									fn : function(taxon) {
										this.fireEvent("taxonchanged", taxon);
									}.createDelegate(this),
									scope : this
								}
							}
						});

				var geneToolbar = new Ext.Panel({
							tbar : this.toolbar,
							frame : false,
							region : 'north'
						});

				Ext.apply(this, {
							items : [geneToolbar, this.geneGrid]
						});
				Gemma.GeneChooserPanel.superclass.initComponent.call(this);
				this.addEvents('taxonchanged', 'ready', 'addgenes', 'removegenes');

				this.toolbar.geneCombo.on("select", function() {
							this.fireEvent("addgenes");
						}, this);

				this.geneGrid.getStore().on("remove", function() {
							this.fireEvent("removegenes");
						}, this);
				
				this.geneGrid.getStore().on("add", function() {
							this.fireEvent("addgenes");
						}, this);

						
			},

			getTaxonId : function() {
				return this.toolbar.getTaxonId();
			},

			setGene : function(geneId, callback, args) {
				this.toolbar.setGene(geneId, callback, args);
				this.fireEvent("addgenes", [geneId]);
			},

			loadGenes : function(geneIds, callback, args) {
				this.geneGrid.loadGenes(geneIds, callback, args);
				this.fireEvent("addgenes", geneIds);
			},

			getGeneIds : function() {
				var ids = [];
				var all = this.geneGrid.getStore().getRange();
				for (var i = 0; i < all.length; ++i) {
					ids.push(all[i].data.id);
				}
				var gene = this.toolbar.geneCombo.getGene();
				if (gene) {
					for (var i = 0; i < ids.length; ++i) {
						// don't add twice.
						if (ids[i] == gene.id) {
							return ids;
						}
					}
					ids.push(gene.id);
				}
				return ids;
			},

			taxonChanged : function(taxon) {
				this.toolbar.taxonChanged(taxon);
			}

		});

/*
 * instance methods...
 */

Ext.reg('genechooser', Gemma.GeneChooserPanel);
