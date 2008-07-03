Ext.namespace('Gemma');

/*
 * Gemma.CoexpressionGrid constructor... config is a hash with the following
 * options:
 */
Gemma.CoexpressionGrid = function(config) {

	this.pageSize = config.pageSize;

	var dataSets; // The data sets that are 'relevant'. Updated during
	// loaddata.

	/*
	 * establish default config options...
	 */
	var superConfig = {
		collapsible : true,
		editable : false,
		autoHeight : true,
		style : "margin-bottom: 1em;"
	};

	if (this.pageSize) {
		superConfig.store = new Gemma.PagingDataStore({
			proxy : new Ext.data.MemoryProxy([]),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, Gemma.CoexpressionGrid.getRecord()),
			sortInfo : {
				field : 'sortKey',
				direction : 'ASC'
			},
			pageSize : this.pageSize
		});
		superConfig.bbar = new Gemma.PagingToolbar({
			pageSize : this.pageSize,
			store : superConfig.store
		});
	} else {
		superConfig.ds = new Ext.data.Store({
			proxy : new Ext.data.MemoryProxy([]),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, Gemma.CoexpressionGrid.getRecord()),
			sortInfo : {
				field : 'sortKey',
				direction : 'ASC'
			}
		});
	}

	this.rowExpander = new Gemma.CoexpressionGridRowExpander({
		tpl : ""
	});

	superConfig.cm = new Ext.grid.ColumnModel([this.rowExpander, {
		id : 'query',
		header : "Query Gene",
		dataIndex : "queryGene",
		tooltip : "Query Gene"
	}, {
		id : 'found',
		header : "Coexpressed Gene",
		dataIndex : "foundGene",
		renderer : Gemma.CoexpressionGrid.getFoundGeneStyler(),
		tooltip : "Coexpressed Gene"
	}, {
		id : 'support',
		header : "Support",
		dataIndex : "supportKey",
		width : 75,
		renderer : Gemma.CoexpressionGrid.getSupportStyler(),
		tooltip : "# of Datasets that confirm coexpression"
	}, {
		id : 'go',
		header : "GO Overlap",
		dataIndex : "goSim",
		width : 75,
		renderer : Gemma.CoexpressionGrid.getGoStyler(),
		tooltip : "GO Similarity Score"

	}, {
		id : 'datasets',
		header : "Datasets",
		dataIndex : "datasetVector",
		renderer : Gemma.CoexpressionGrid.getBitImageStyler(),
		tooltip : "Dataset relevence map",
		sortable : false
	}]);
	superConfig.cm.defaultSortable = true;
	superConfig.plugins = this.rowExpander;

	superConfig.autoExpandColumn = 'found';

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.CoexpressionGrid.superclass.constructor.call(this, superConfig);

	this.originalTitle = this.title;

};

/*
 * static methods
 */
Gemma.CoexpressionGrid.getRecord = function() {
	if (Gemma.CoexpressionGrid.record === undefined) {
		Gemma.CoexpressionGrid.record = Ext.data.Record.create([{
			name : "queryGene",
			type : "string",
			convert : function(g) {
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
		}]);
	}
	return Gemma.CoexpressionGrid.record;
};

Gemma.CoexpressionGrid.getRowExpander = function() {
	if (Gemma.CoexpressionGrid.rowExpander === undefined) {
		Gemma.CoexpressionGrid.rowExpander = new Gemma.CoexpressionGridRowExpander({
			tpl : "",
			grid : this
		});
	}
	return Gemma.CoexpressionGrid.rowExpander;
};

/*
 * instance methods...
 */
Ext.extend(Gemma.CoexpressionGrid, Ext.grid.GridPanel, {

	viewConfig : {
		forceFit : true
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
			var imageWidth = Gemma.CoexpressionGrid.bitImageBarWidth
					* first.data.datasetVector.length;
			cm.setColumnWidth(c, imageWidth < headerWidth
					? headerWidth
					: imageWidth);
		}
	}

});

/*
 * Stylers
 * 
 */
Gemma.CoexpressionGrid.getFoundGeneStyler = function() {
	if (Gemma.CoexpressionGrid.foundGeneStyler === undefined) {
		Gemma.CoexpressionGrid.foundGeneTemplate = new Ext.Template(

				"<img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' />",

				" &nbsp; ",
				"<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}");
		Gemma.CoexpressionGrid.foundGeneStyler = function(value, metadata,
				record, row, col, ds) {
			var g = record.data.foundGene;
			if (g.officialName === null) {
				g.officialName = "";
			}
			return Gemma.CoexpressionGrid.foundGeneTemplate.apply(g);
		};
	}
	return Gemma.CoexpressionGrid.foundGeneStyler;
};

Gemma.CoexpressionGrid.getSupportStyler = function() {
	if (Gemma.CoexpressionGrid.supportStyler === undefined) {
		Gemma.CoexpressionGrid.supportStyler = function(value, metadata,
				record, row, col, ds) {
			var d = record.data;
			if (d.posLinks || d.negLinks) {
				var s = "";
				if (d.posLinks) {
					s = s
							+ String
									.format(
											"<span class='positiveLink'>{0}{1}</span> ",
											d.posLinks, Gemma.CoexpressionGrid
													.getSpecificLinkString(
															d.posLinks,
															d.nonSpecPosLinks));
				}
				if (d.negLinks) {
					s = s
							+ String
									.format(
											"<span class='negativeLink'>{0}{1}</span> ",
											d.negLinks, Gemma.CoexpressionGrid
													.getSpecificLinkString(
															d.negLinks,
															d.nonSpecNegLinks));
				}
				s = s
						+ String.format("{0}/ {1}", d.hybWQuery ? " *" : "",
								d.numTestedIn);
				return s;
			} else {
				return "-";
			}
		};
	}
	return Gemma.CoexpressionGrid.supportStyler;
};

Gemma.CoexpressionGrid.getSpecificLinkString = function(total, nonSpecific) {
	return nonSpecific
			? String.format("<span class='specificLink'> ({0})</span>", total
					- nonSpecific)
			: "";
};

Gemma.CoexpressionGrid.getGoStyler = function() {
	if (Gemma.CoexpressionGrid.goStyler === undefined) {
		Gemma.CoexpressionGrid.goStyler = function(value, metadata, record,
				row, col, ds) {
			var d = record.data;
			if (d.goSim || d.maxGoSim) {
				return String.format("{0}/{1}", d.goSim, d.maxGoSim);
			} else {
				return "-";
			}
		};
	}
	return Gemma.CoexpressionGrid.goStyler;
};

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

Gemma.CoexpressionGrid.getBitImageStyler = function() {
	if (Gemma.CoexpressionGrid.bitImageStyler === undefined) {
		Gemma.CoexpressionGrid.bitImageStyler = function(value, metadata,
				record, row, col, ds) {
			var bits = record.data.datasetVector;
			var width = Gemma.CoexpressionGrid.bitImageBarWidth * bits.length;
			var height = Gemma.CoexpressionGrid.bitImageBarHeight;
			var s = '<span style="background-color:#DDDDDD;">'
					+ '<img src="/Gemma/spark?type=bar&width=' + width
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
		};
	}
	return Gemma.CoexpressionGrid.bitImageStyler;
};
