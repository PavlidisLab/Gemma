Ext.namespace('Gemma');

/**
 * A lightweight version to be used with the quick coexpression search. 
 * 
 * @class Gemma.CoexpressionGridLite
 * @extends Ext.grid.GridPanel
 * 
 */

Gemma.SHOW_ONLY_MINE = "Show only my data";
Gemma.SHOW_ALL = "Show all results";

Gemma.CoexpressionGridLite = Ext.extend(Ext.grid.GridPanel, {

			collapsible : true,
			editable : false,
			autoHeight : true,
			style : "margin-bottom: 1em;",
			stateful : false,

			viewConfig : {
				forceFit : true
			},

			initComponent : function() {

				if (this.pageSize) {
					this.store = new Gemma.PagingDataStore({
								proxy : new Ext.data.MemoryProxy([]),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record),
								sortInfo : {
									field : 'sortKey',
									direction : 'ASC'
								},
								pageSize : this.pageSize
							});
					this.bbar = new Gemma.PagingToolbar({
								pageSize : this.pageSize,
								store : this.store
							});
				} else {
					this.ds = new Ext.data.Store({
								proxy : new Ext.data.MemoryProxy([]),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record),
								sortInfo : {
									field : 'sortKey',
									direction : 'ASC'
								}
							});
				}

				Ext.apply(this, {
							tbar : new Ext.Toolbar({
										items : [{
													hidden : !this.user,
													pressed : true,
													enableToggle : true,
													text : Gemma.SHOW_ONLY_MINE,
													tooltip : "Click to show/hide results containing only my data",
													cls : 'x-btn-text-icon details',
													toggleHandler : this.toggleMyData.createDelegate(this)
												}, ' ', ' ', {
													xtype : 'textfield',
													id : 'search-in-grid',
													tabIndex : 1,
													enableKeyEvents : true,
													emptyText : 'Find gene in results',
													listeners : {
														"keyup" : {
															fn : this.searchForText.createDelegate(this),
															scope : this,
															options : {
																delay : 100
															}
														}
													}
												}]
									}),

							columns : [{
										id : 'query',
										header : "Query Gene",
										hidden : true,
										dataIndex : "queryGene",
										tooltip : "Query Gene",
										renderer : this.queryGeneStyler.createDelegate(this) ,
										sortable : true
									}, {
										id : 'details',
										hidden : true,										
										header : "Details",
										dataIndex : 'details',
										renderer : this.detailsStyler.createDelegate(this),
										tooltip : "Links for probe-level details",
										sortable : false,
										width : 30
									}, {
										id : 'visualize',
										hidden : true,
										header : "Visualize",
										dataIndex : "visualize",
										renderer : this.visStyler.createDelegate(this),
										tooltip : "Link for visualizing raw data",
										sortable : false,
										width : 30

									}, {
										id : 'found',
										header : "Coexpressed Gene",
										dataIndex : "foundGene",
										renderer : this.foundGeneStyler.createDelegate(this),
										tooltip : "Coexpressed Gene",
										sortable : true
									}, {
										id : 'support',
										header : "Support",
										dataIndex : "supportKey",
										width : 20,
										renderer : this.supportStyler.createDelegate(this),
										tooltip : "# of Datasets that confirm coexpression",
										sortable : true
									}]

						});

				Gemma.CoexpressionGrid.superclass.initComponent.call(this);
				
					//TODO Add visualization and details?
					//this.on("cellclick", this.geneLiteRowClickHandler.createDelegate(this), this);

			},

		geneLiteRowClickHandler : function(grid, rowIndex, columnIndex, e) {
		if (this.getSelectionModel().hasSelection()) {

			var record = this.getStore().getAt(rowIndex);
			var fieldName = this.getColumnModel().getDataIndex(columnIndex);
			var queryGene = record.get("queryGene");
			var foundGene = record.get("foundGene");			
				
	
			if (fieldName == 'foundGene' && columnIndex != 7) {  //problem with outlink column field name also returns name as foundGene 
				//searchPanel.searchForGene(foundGene.id);
			} else if (fieldName == 'visualize') {

				var foundGene = record.data.foundGene;
				var activeExperiments = record.data.supportingExperiments;
				// destroy if already open
//				if (visWindow !== null) {
//					visWindow.close();
//				}

				var visWindow = new Gemma.CoexpressionVisualizationWindow({
					admin : false
				});
				visWindow.displayWindow(activeExperiments, queryGene, foundGene);
			} else if (fieldName == 'details') {

				var supporting = getSupportingDatasetRecords(record, grid);

				var dsGrid = new Gemma.ExpressionExperimentGrid({
					records : supporting,
					// width : 750,
					// height : 340, Layout will show nothing if this isn't set to something and autoHeight is false.
					// Most likely a loading issue (no data in store, so no height).
					autoHeight : true,
					stateful : false
				});

				// Close if already open
				if (detailsWindow) {
					detailsWindow.close();
				}

				var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
					geneId : foundGene.id,
					threshold : 0.01,
					// width : 750,
					// height : 380,
					stateful : false
				});

				var detailsTP = new Ext.TabPanel({
					layoutOnTabChange : true,
					activeTab : 0,
					stateful : false,
					items : [{
						title : "Supporting datasets",
						items : [dsGrid],
						layout : 'fit',
						autoScroll : true
					}, {
						title : "Differential expression of " + foundGene.officialSymbol,
						items : [diffExGrid],
						layout : 'fit',
						autoScroll : true,
						loaded : false,
						listeners : {
							"activate" : {
								fn : function() {
									if (!this.loaded) {
										diffExGrid.getStore().load({
											params : [foundGene.id, 0.01]
										});
									}
									this.loaded = true;
								}
							}
						}

					}]

				});

				detailsWindow = new Ext.Window({
					modal : false,
					layout : 'fit',
					title : 'Details for ' + foundGene.officialSymbol,
					closeAction : 'close',
					items : [{					
						items : [detailsTP],
						layout : 'fit'
					}],
					width : 760,
					height : 400,
					//autoScroll : true,
					stateful : false
				});

				dsGrid.getStore().load();
				detailsWindow.show();

				diffExGrid.getStore().loadData(supporting);

			}
		}
	},
			toggleMyData : function(btn, pressed) {
				var buttonText = btn.getText();
				if (buttonText == Gemma.SHOW_ALL) {
					this.getStore().clearFilter();
					btn.setText(Gemma.SHOW_ONLY_MINE);
				} else {
					this.getStore().filterBy(function(r, id) {
								return r.get("containsMyData")
							}, this, 0);
					btn.setText(Gemma.SHOW_ALL);
				}
			},

			searchForText : function(button, keyev) {
				var text = Ext.getCmp('search-in-grid').getValue();
				if (text.length < 2) {
					this.getStore().clearFilter();
					return;
				}
				this.getStore().filterBy(this.filter(text), this, 0);
			},

			filter : function(text) {
				var value = new RegExp(Ext.escapeRe(text), 'i');
				return function(r, id) {
					var foundGene = (r.get("foundGene"));
					if (value.test(foundGene.officialSymbol)) {
						return true;
					}

					return false;
				}
			},

			record : Ext.data.Record.create([{
						name : "queryGene",
						sortType : function(g) {
							return g.officialSymbol;
						}
					}, {
						name : "foundGene",
						sortType : function(g) {
							return g.officialSymbol;
						}
					}, {
						name : "sortKey",
						type : "string"
					}, {
						name : "supportKey",
						type : "int",
						sortType : Ext.data.SortTypes.asInt,
						sortDir : "DESC"
					}, {
						name : "posSupp",
						type : "int"
					}, {
						name : "negSupp",
						type : "int"
					}, {
						name : "numTestedIn",
						type : "int"
					}, {
						name : "nonSpecPosSupp",
						type : "int"
					}, {
						name : "nonSpecNegSupp",
						type : "int"
					},{
						name : "supportingExperiments"
					}, {
						name : "hybWQuery",
						type : "boolean"
					}]),

			
					
			
			/**
			 * 
			 */
			supportStyler : function(value, metadata, record, row, col, ds) {
				var d = record.data;
				if (d.posSupp || d.negSupp) {
					var s = "";
					if (d.posSupp) {
						s = s
								+ String.format("<span class='positiveLink'>{0}{1}</span> ", d.posSupp, this
												.getSpecificLinkString(d.posSupp, d.nonSpecPosSupp));
					}
					if (d.negSupp) {
						s = s
								+ String.format("<span class='negativeLink'>{0}{1}</span> ", d.negSupp, this
												.getSpecificLinkString(d.negSupp, d.nonSpecNegSupp));
					}
					//TODO numTestedIn isn't set in the quick coexpressio method... might want this info
					//s = s + String.format("/ {0}", d.numTestedIn);
					return s;
				} else {
					return "-";
				}
			},

			getSpecificLinkString : function(total, nonSpecific) {
				return nonSpecific
						? String.format("<span class='specificLink'> ({0})</span>", total - nonSpecific)
						: "";
			},

			/**
			 * Display the target (found) genes.
			 * 
			 */
			foundGeneStyler : function(value, metadata, record, row, col, ds) {

				var g = record.data.foundGene;

				if (g.officialName === null) {
					g.officialName = "";
				}
				
				g.taxonId = g.taxon.id;
				g.taxonName = g.taxon.commonName;
				
				return this.foundGeneTemplate.apply(g);
			},

			queryGeneStyler : function(value, metadata, record, row, col, ds) {

				var g = record.data.queryGene;

				if (g.officialName === null) {
					g.officialName = "";
				}
				
				g.abaGeneUrl = record.data.abaQueryGeneUrl;
				
				return this.foundGeneTemplate.apply(g);
			},
		
			visStyler : function(value, metadata, record, row, col, ds) {
				return "<img src='/Gemma/images/icons/chart_curve.png' ext:qtip='Visualize the data' />";
			},

			detailsStyler : function(value, metadata, record, row, col, ds) {
				return "<img src='/Gemma/images/icons/magnifier.png' ext:qtip='Show probe-level details' /> ";
			},

			//Creates a link for downloading raw dedv's data in tab delimted format
			//currently not used as same function in visulazation widget
			downloadDedvStyler : function(value, metadata, record, row, col, ds) {

				var queryGene = record.data.queryGene;
				var foundGene = record.data.foundGene;
				
				var activeExperimentsString = "";
				var activeExperimentsSize = record.data.supportingExperiments.size();
				
				for (var i = 0; i < activeExperimentsSize; i++) {
					if (i === 0) {
						activeExperimentsString = record.data.supportingExperiments[i];
					} else {
						activeExperimentsString = String.format("{0}, {1}", activeExperimentsString,
								record.data.supportingExperiments[i]);
					}
				}
		
			return String.format("<a href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1},{2}' > download </a>",
				activeExperimentsString, queryGene.id, foundGene.id);
		},
			

			loadData : function(isCannedAnalysis, numQueryGenes, data, datasets) {
				var queryIndex = this.getColumnModel().getIndexById('query');
				if (numQueryGenes > 1) {
					this.getColumnModel().setHidden(queryIndex, false);
				} else {
					this.getColumnModel().setHidden(queryIndex, true);

				}
				this.datasets = datasets; // the datasets that are 'relevant'.
				this.getStore().proxy.data = data;
				this.getStore().reload({
							resetPage : true
						});
				// this.getView().refresh(true); // refresh column headers
				this.resizeDatasetColumn();
			},

			resizeDatasetColumn : function() {
				var first = this.getStore().getAt(0);
				if (first) {
					var cm = this.getColumnModel();
					var c = cm.getIndexById('datasets');
					var headerWidth = this.view.getHeaderCell(c).firstChild.scrollWidth;
					var imageWidth = Gemma.CoexpressionGrid.bitImageBarWidth * first.data.datasetVector.length;
					cm.setColumnWidth(c, imageWidth < headerWidth ? headerWidth : imageWidth);
				}
			},

			foundGeneTemplate : new Ext.Template(
					"<a href='/Gemma/searchCoexpression.html?g={id}&s=3&t={taxonId}&an=all {taxonName}'> <img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' /> </a>",			
					" &nbsp; ", "<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}"				
					)

		});



