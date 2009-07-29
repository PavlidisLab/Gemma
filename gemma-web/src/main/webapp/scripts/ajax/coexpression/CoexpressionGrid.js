Ext.namespace('Gemma');

/**
 * Grid for showing coexpression results.
 * 
 * @class Gemma.CoexpressionGrid
 * @extends Ext.grid.GridPanel
 * 
 */

Gemma.SHOW_ONLY_MINE = "Show only my data";
Gemma.SHOW_ALL = "Show all results";

Gemma.CoexpressionGrid = Ext.extend(Ext.grid.GridPanel, {

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
										header : "Details",
										dataIndex : 'details',
										renderer : this.detailsStyler.createDelegate(this),
										tooltip : "Links for probe-level details",
										sortable : false,
										width : 30
									}, {
										id : 'visualize',
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
										width : 75,
										renderer : this.supportStyler.createDelegate(this),
										tooltip : "# of Datasets that confirm coexpression",
										sortable : true
									}, {
										id : 'go',
										header : "GO Overlap",
										dataIndex : "goSim",
										width : 75,
										renderer : this.goStyler.createDelegate(this),
										tooltip : "GO Similarity Score",
										sortable : true

									}, {
										id : 'datasets',
										header : "Datasets",
										dataIndex : "datasetVector",
										renderer : this.bitImageStyler.createDelegate(this),
										tooltip : "Dataset relevence map",
										sortable : false
									}, {
										id : 'linkOut',
										dataIndex : "foundGene",
										header : "Out links",
										sortable : false,
										width : 30,
										tooltip : "Links to other websites for more relevent information (if relevent and/or available)",
										renderer : this.linkOutStyler
									}]

						});

				Gemma.CoexpressionGrid.superclass.initComponent.call(this);
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
					}, {
						name : "hybWQuery",
						type : "boolean"
					}, {
						name : "goSim",
						type : "int"
					}, {
						name : "maxGoSim",
						type : "int"
					}, {
						name : "datasetVector",
						type : "string"
					}, {
						name : "supportingExperiments"
					}, {
						name : "containsMyData",
						type : "boolean"
					}]),

			
					
			linkOutStyler : function(value, metadata, record, row, col, ds){
			
				var popUpWin = "LinkOutController.getAlanBrainAtalsLinks('" + value.officialSymbol + "',Gemma.linkOutPopUp)";
					return String.format('<a title="Alan Brian Atlas Image"  onClick="{0}"> <img height=15 width =15 src="/Gemma/images/logo/abaLogo.jpg" ext:qtip="Link to Allen Brain Atlas details" /> </a>', popUpWin);
			},
			
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
					s = s + String.format("/ {0}", d.numTestedIn);
					return s;
				} else {
					return "-";
				}
			},

			/**
			 * For displaying Gene ontology similarity
			 * 
			 */
			goStyler : function(value, metadata, record, row, col, ds) {
				var d = record.data;
				if (d.goSim || d.maxGoSim) {
					return String.format("{0}/{1}", d.goSim, d.maxGoSim);
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
			bitImageStyler : function(value, metadata, record, row, col, ds) {
				var bits = record.data.datasetVector;
				var width = bits.length * Gemma.CoexpressionGrid.bitImageBarWidth;
				var gap = 0;

				var s = ''
				var maxheight = 0;
				for (var i = 0; i < bits.length; ++i) {
					if (i > 0) {
						s = s + ",";
					}
					var state = bits.charAt(i);
					var b = "";
					if (state === "0") {
						b = "0"; // not tested
					} else if (state === "1") {
						b = "2"; // tested but no support
						if (2 > maxheight) {
							maxheight = 2;
						}
					} else if (state === "2") {
						b = "10"; // supported but nonspecific
						if (10 > maxheight) {
							maxheight = 10;
						}
					} else if (state === "3") {
						maxheight = Gemma.CoexpressionGrid.bitImageBarHeight;
						b = Gemma.CoexpressionGrid.bitImageBarHeight; // supported and specific
					}
					s = s + b;
				}

				var result = '<span style="margin:0;padding-top:'
						+ (Gemma.CoexpressionGrid.bitImageBarHeight - maxheight) + 'px;height:'
						+ Gemma.CoexpressionGrid.bitImageBarHeight + ';background-color:#EEEEEE" >'
						+ '<img style="vertical-align:bottom" src="/Gemma/spark?type=bar&width=' + width + '&height='
						+ maxheight + '&highcolor=black&color=black&spacing=' + gap + '&data=';

				// dataset-bits is defined in typo.css

				// eeMap is created in CoexpressionSearch.js
				result = result + s + '" usemap="#eeMap" /></span>';
				return result;
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
					"<img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' />",			
					" &nbsp; ", "<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}"				
					)

		});

Gemma.CoexpressionGrid.bitImageBarHeight = 15;
Gemma.CoexpressionGrid.bitImageBarWidth = 1;

Gemma.CoexpressionGrid.getBitImageMapTemplate = function() {
	if (Gemma.CoexpressionGrid.bitImageMapTemplate === undefined) {
		Gemma.CoexpressionGrid.bitImageMapTemplate = new Ext.XTemplate(
				'<tpl for=".">',
				'<area shape="rect" coords="{[ (xindex - 1) * this.barx ]},0,{[ xindex * this.barx ]},{[ this.bary ]}" ext:qtip="{name}" href="{externalUri}" />',
				'</tpl>', {
					barx : Gemma.CoexpressionGrid.bitImageBarWidth,
					bary : Gemma.CoexpressionGrid.bitImageBarHeight - 1
				});
	}
	return Gemma.CoexpressionGrid.bitImageMapTemplate;
};

	Gemma.linkOutPopUp = function(linkOutValueObject){

			//TODO:  Make pop up window show more than one image (have a button for scrolling to next image)
				var popUpHtml;
				
				if (linkOutValueObject == null){
					window.alert("No Allen Brain Atlas details available for this gene");
					return;
				}else{
					popUpHtml = String.format("<a href={0} target='_blank' > <img src={1}> </a>", linkOutValueObject.abaGeneUrl, linkOutValueObject.abaGeneImageUrls[0]);
				}
					
				popUpLinkOutWin = new Ext.Window({  html: popUpHtml,   autoScroll : true});
				popUpLinkOutWin.show(this);													

			};
