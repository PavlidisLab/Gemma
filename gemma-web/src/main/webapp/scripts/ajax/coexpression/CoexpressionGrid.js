Ext.namespace('Gemma');

/**
 * Grid for showing coexpression results.
 * 
 * @class Gemma.CoexpressionGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.CoexpressionGrid = Ext.extend(Ext.grid.GridPanel, {

	collapsible : true,
	editable : false,
	autoHeight : true,
	style : "margin-bottom: 1em;",

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

		this.rowExpander = new Gemma.CoexpressionGridRowExpander({
			tpl : "",
			grid : this
		});

		Ext.apply(this, {
			columns : [this.rowExpander, {
				id : 'query',
				header : "Query Gene",
				dataIndex : "queryGene",
				tooltip : "Query Gene",
				sortable : true
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
				id : 'download',
				header : "Download",
				renderer : this.downloadDedv.createDelegate(this),
				tooltip : "Link for downloading raw data",
				sortable : false
			}]

		});

		Ext.apply(this, {
			plugins : this.rowExpander
		});

		Gemma.CoexpressionGrid.superclass.initComponent.call(this);
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
		name : "posLinks",
		type : "int"
	}, {
		name : "negLinks",
		type : "int"
	}, {
		name : "numTestedIn",
		type : "int"
	}, {
		name : "nonSpecPosLinks",
		type : "int"
	}, {
		name : "nonSpecNegLinks",
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
	}]),

	/**
	 * 
	 */
	supportStyler : function(value, metadata, record, row, col, ds) {
		var d = record.data;
		if (d.posLinks || d.negLinks) {
			var s = "";
			if (d.posLinks) {
				s = s
						+ String.format("<span class='positiveLink'>{0}{1}</span> ", d.posLinks, this
								.getSpecificLinkString(d.posLinks, d.nonSpecPosLinks));
			}
			if (d.negLinks) {
				s = s
						+ String.format("<span class='negativeLink'>{0}{1}</span> ", d.negLinks, this
								.getSpecificLinkString(d.negLinks, d.nonSpecNegLinks));
			}
			s = s + String.format("{0}/ {1}", d.hybWQuery ? " *" : "", d.numTestedIn);
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
		return nonSpecific ? String.format("<span class='specificLink'> ({0})</span>", total - nonSpecific) : "";
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

	bitImageStyler : function(value, metadata, record, row, col, ds) {
		var bits = record.data.datasetVector;
		var width = Gemma.CoexpressionGrid.bitImageBarWidth * bits.length;
		var height = Gemma.CoexpressionGrid.bitImageBarHeight;
		var s = '<span style="background-color:#DDDDDD;">' + '<img src="/Gemma/spark?type=bar&width=' + width
				+ '&height=' + height + '&color=black&spacing=0&data=';
		for (var i = 0; i < bits.length; ++i) {
			if (i > 0) {
				s = s + ",";
			}
			var state = bits.charAt(i);
			var b = "";
			if (state === "0") {
				b = "0";
			} else if (state === "1") {
				b = "4";
			} else if (state === "2") {
				b = "20";
			}
			s = s + b;
		}
		// eeMap is created in CoexpressionSearch.js
		s = s + '" usemap="#eeMap" /></span>';
		return s;
	},
	
	downloadDedv : function(value, metadata, record, row, col, ds) {

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
		var queryCol = this.getColumnModel().getColumnById('query');
		if (numQueryGenes > 1) {
			queryCol.hidden = false;
		} else {
			queryCol.hidden = true;
		}
		this.rowExpander.clearCache();
		this.datasets = datasets; // the datasets that are 'relevant'.
		this.getStore().proxy.data = data;
		this.getStore().reload({
			resetPage : true
		});
		this.getView().refresh(true); // refresh column headers
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
			" &nbsp; ", "<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}")

});
Gemma.CoexpressionGrid.bitImageBarWidth = 1;
Gemma.CoexpressionGrid.bitImageBarHeight = 10;

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
