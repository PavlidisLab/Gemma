/*
 * GeneGrid GeneChooserToolbar. Widget for picking genes. Allows user to search for and select one or more genes from
 * the database. The selected genes are kept in a table which can be edited. This component is the top part of the
 * coexpression interface, but should be reusable.
 * 
 * Version : $Id$ Author : luke, paul
 */
Ext.namespace('Gemma');

/**
 * The maximum number of genes we allow users to put in at once.
 * 
 * @type Number
 */
Gemma.MAX_GENES_PER_PASTE = 1000;

/**
 * Table of genes with toolbar for searching.
 * 
 * @class GeneGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.GeneGrid = Ext.extend(Ext.grid.GridPanel, {

			collapsible : false,
			autoWidth : true,
			stateful : false,
			frame : true,
			title : "Genes",
			layout : 'fit',
			width : 400,
			height : 250,

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
				sortable : true,
				renderer : function(value, metadata, record, row, col, ds) {
					return String.format("<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a> ",
							record.data.id, record.data.officialSymbol);
				}
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
				if (!geneIds || geneIds.length === 0) {
					return;
				}

				GenePickerController.getGenes(geneIds, function(genes) {
							var geneData = [];
							for (var i = 0; i < genes.length; ++i) {
								geneData.push([genes[i].id, genes[i].taxonScientificName, genes[i].officialSymbol,
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
							tbar : new Gemma.GeneChooserToolBar({
										geneGrid : this,
										extraButtons : this.extraButtons,
										style : "border: #a3bad9 solid 1px;"
									}),
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

				this.addEvents('addgenes', 'removegenes');

				this.getTopToolbar().geneCombo.on("select", function() {
							this.fireEvent("addgenes");
						}, this);

				this.getStore().on("remove", function() {
							this.fireEvent("removegenes");
						}, this);

				this.getStore().on("add", function() {
							this.fireEvent("addgenes");
						}, this);

				this.on("keypress", function(e) {
							if (!this.getTopToolbar().disabled && e.getCharCode() === Ext.EventObject.DELETE) {
								this.removeGene();
							}
						}, this);

				// See http://www.extjs.com/learn/Tutorial:RelayEvents
				this.relayEvents(this.getTopToolbar(), ['ready', 'taxonchanged']);

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
				this.getSelectionModel().selectLastRow();
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

				if (!taxon) {
					Ext.Msg.alert("Problem", "Please select a taxon first");
					return;
				}

				var loadMask = new Ext.LoadMask(this.getEl(), {
							msg : "Loading genes..."
						});
				loadMask.show();

				var taxonId = taxon.id;
				var text = e.geneNames;
				GenePickerController.searchMultipleGenes(text, taxonId, {

							callback : function(genes) {
								var geneData = [];
								var warned = false;
								for (var i = 0; i < genes.length; ++i) {
									if (i >= Gemma.MAX_GENES_PER_QUERY) {
										if (!warned) {
											Ext.Msg.alert("Too many genes", "You can only search up to " +
															Gemma.MAX_GENES_PER_QUERY +
															" genes, some of your selections will be ignored.");
											warned = true;
										}
										break;
									}

									if (this.getStore().find("id", genes[i].id) < 0) {
										geneData.push([genes[i].id, genes[i].taxonScientificName,
												genes[i].officialSymbol, genes[i].officialName]);
									}

								}
								this.getStore().loadData(geneData, true);
								loadMask.hide();

							}.createDelegate(this),

							errorHandler : function(e) {
								this.getEl().unmask();
								Ext.Msg.alert('There was an error', e);
							}
						});
			},

			getTaxonId : function() {
				return this.getTopToolbar().getTaxonId();
			},

			setGene : function(geneId, callback, args) {
				this.getTopToolbar().setGene(geneId, callback, args);
				this.fireEvent("addgenes", [geneId]);
			},

			/**
			 * 
			 * @return {} list of all geneids currently held, including ones in the grid and possible one in the field.
			 */
			getGeneIds : function() {
				var ids = [];
				var all = this.getStore().getRange();
				for (var i = 0; i < all.length; ++i) {
					ids.push(all[i].data.id);
				}
				var gene = this.getTopToolbar().geneCombo.getGene();
				if (gene) {
					for (var j = 0; j < ids.length; ++j) {
						// don't add twice.
						if (ids[j] === gene.id) {
							return ids;
						}
					}
					ids.push(gene.id);
				}
				return ids;
			},

			taxonChanged : function(taxon) {
				this.getTopToolbar().taxonChanged(taxon, true);
			},

			// returns gene objects in an array
			// gene = {id, officialSymbol, officialName, taxon}
			getGenes : function() {

				var genes = [];
				var all = this.getStore().getRange();
				for (var i = 0; i < all.length; ++i) {
					genes.push(all[i].data);
				}
				var gene = this.getTopToolbar().geneCombo.getGene();
				if (gene) {
					for (var j = 0; j < genes.length; ++j) {
						// don't add twice.
						if (genes[j].id === gene.id) {
							return genes;
						}
					}
					genes.push(gene);
				}
				return genes;

			}

		});

Ext.reg('genechooser', Gemma.GeneGrid);

/**
 * Toolbar with taxon chooser and gene search field.
 * 
 * @class Gemma.GeneChooserToolBar
 * @extends Ext.Toolbar
 */
Gemma.GeneChooserToolBar = Ext.extend(Ext.Toolbar, {

			name : "gctb",

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
			 * Check if the taxon needs to be changed, and if so, update it for the genecombo and the taxonCombo.
			 * 
			 * @param {}
			 *            taxon
			 */
			taxonChanged : function(taxon) {

				if (!taxon || (this.geneCombo.getTaxon() && this.geneCombo.getTaxon().id === taxon.id)) {
					return;
				}

				this.geneCombo.setTaxon(taxon);

				// clear any genes.
				var all = this.getStore().getRange();
				for (var i = 0; i < all.length; ++i) {
					if (all[i].get('taxonId') !== taxon.id) {
						this.getStore().remove(all[i]);
					}
				}

				this.taxonCombo.setTaxon(taxon);

				this.fireEvent("taxonchanged", taxon);
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

				/*
				 * The taxon combo and gene combo have to update each other. Also the taxon combo is stateful.
				 */

				this.taxonCombo = new Gemma.TaxonCombo({
							isDisplayTaxonWithGenes : true,
							listeners : {
								'select' : {
									fn : function(cb, rec, index) {
										this.taxonChanged(rec.data, false);
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
											this.taxonCombo.setTaxon(rec.get("taxonId"));
										} else {
											this.taxonCombo.setTaxon(rec.taxonId);
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
								//this.removeButton.disable();
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

								if (!this.getTaxon()) {
									Ext.Msg.alert("Problem", "Please select a taxon first");
									return;
								}

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

				this.relayEvents(this.taxonCombo, ['ready']);
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

				if (this.extraButtons) {
					for (var i = 0; i < this.extraButtons.length; i++) {
						this.addSpacer();
						this.add(this.extraButtons[i]);
					}
				}

			}

		});

/**
 * pop-up to put in multiple genes.
 * 
 * @class Gemma.GeneImportPanel
 * @extends Ext.Window
 */
Gemma.GeneImportPanel = Ext.extend(Ext.Window, {

			title : "Import multiple genes (one symbol or NCBI id per line, up to " + Gemma.MAX_GENES_PER_PASTE + ")",
			modal : true,
			layout : 'fit',
			stateful : false,
			autoHeight : false,
			width : 350,
			height : 300,
			closeAction : 'hide',
			easing : 3,
			showTaxonCombo: false,

			onCommit : function() {
				if(this.showTaxonCombo &&
					 (typeof this._taxonCombo.getTaxon() === 'undefined' || isNaN(this._taxonCombo.getTaxon().id))){
					this._taxonCombo.markInvalid("This field is required");
					return;
				}
				this.hide();
				this.fireEvent("commit", {
							geneNames : this._geneText.getValue()
						});
			},

			initComponent : function() {

				this.addEvents({
							"commit" : true
						});

				if(this.showTaxonCombo){
					Ext.apply(this, {
							layout:'form',
							width : 420,
							height : 400,
							padding:10,
							items : [{
										xtype : 'taxonCombo',
										ref:'_taxonCombo',
										emptyText : 'Select a taxon (required)',
										fieldLabel : 'Select a taxon',
										width: 250,
										msgTarget:'side'
									},{
										xtype : 'textarea',
										ref:'_geneText',
										fieldLabel : "Paste in gene symbols, one per line, up to " +
												Gemma.MAX_GENES_PER_QUERY,
										width : 250,
										height:290
									}]
						});
				}else{
					Ext.apply(this, {
							items : [{
										id : 'gene-list-text',
										xtype : 'textarea',
										ref:'_geneText',
										fieldLabel : "Paste in gene symbols, one per line, up to " +
												Gemma.MAX_GENES_PER_QUERY,
										width : 290,
										height: 290
									}]
						});
				}
				Ext.apply(this, {
							buttons : [{
										text : 'OK',
										handler : this.onCommit,
										scope : this
									},  {
										text : 'Clear',
										scope:this,
										handler : function() {
											this._geneText.setValue("");
										}
									},{
										text : 'Cancel',
										handler : function() {
											this.hide();
										}.createDelegate(this),
										scope : this
									}]
						});

				Gemma.GeneImportPanel.superclass.initComponent.call(this);
			}

		});
