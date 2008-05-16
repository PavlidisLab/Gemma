Ext.namespace('Ext.Gemma');

/*
 * Ext.Gemma.CoexpressionGrid constructor... config is a hash with the following
 * options:
 */
Ext.Gemma.CoexpressionGrid = function(config) {
	Ext.QuickTips.init();

	this.pageSize = config.pageSize;
	delete config.pageSize;

	/*
	 * keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;

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
		superConfig.store = new Ext.Gemma.PagingDataStore({
			proxy : new Ext.data.MemoryProxy([]),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, Ext.Gemma.CoexpressionGrid.getRecord()),
			sortInfo : {
				field : 'sortKey',
				direction : 'ASC'
			},
			pageSize : this.pageSize
		});
		superConfig.bbar = new Ext.Gemma.PagingToolbar({
			pageSize : this.pageSize,
			store : superConfig.store
		});
	} else {
		superConfig.ds = new Ext.data.Store({
			proxy : new Ext.data.MemoryProxy([]),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, Ext.Gemma.CoexpressionGrid.getRecord()),
			sortInfo : {
				field : 'sortKey',
				direction : 'ASC'
			}
		});
	}

	this.rowExpander = new Ext.Gemma.CoexpressionGridRowExpander({
		tpl : ""
	});

	superConfig.cm = new Ext.grid.ColumnModel([this.rowExpander, {
		id : 'query',
		header : "Query Gene",
		dataIndex : "queryGene"
	}, {
		id : 'found',
		header : "Coexpressed Gene",
		dataIndex : "foundGene",
		renderer : Ext.Gemma.CoexpressionGrid.getFoundGeneStyler()
	}, {
		id : 'support',
		header : "Support",
		dataIndex : "supportKey",
		width : 75,
		renderer : Ext.Gemma.CoexpressionGrid.getSupportStyler()
	}, {
		id : 'go',
		header : "GO Overlap",
		dataIndex : "goSim",
		width : 75,
		renderer : Ext.Gemma.CoexpressionGrid.getGoStyler()
	}, {
		id : 'datasets',
		header : "Datasets",
		dataIndex : "datasetVector",
		renderer : Ext.Gemma.CoexpressionGrid.getBitImageStyler(),
		sortable : false
	}]);
	superConfig.cm.defaultSortable = true;
	superConfig.plugins = this.rowExpander;

	superConfig.autoExpandColumn = 'found';

	for (property in config) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionGrid.superclass.constructor.call(this, superConfig);

	this.originalTitle = this.title;

};

/*
 * static methods
 */
Ext.Gemma.CoexpressionGrid.getRecord = function() {
	if (Ext.Gemma.CoexpressionGrid.record === undefined) {
		Ext.Gemma.CoexpressionGrid.record = Ext.data.Record.create([{
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
	return Ext.Gemma.CoexpressionGrid.record;
};

Ext.Gemma.CoexpressionGrid.getRowExpander = function() {
	if (Ext.Gemma.CoexpressionGrid.rowExpander === undefined) {
		Ext.Gemma.CoexpressionGrid.rowExpander = new Ext.Gemma.CoexpressionGridRowExpander({
			tpl : "",
			grid : this
		});
	}
	return Ext.Gemma.CoexpressionGrid.rowExpander;
};

Ext.Gemma.CoexpressionGrid.searchForGene = function(geneId) {
	var f = Ext.Gemma.CoexpressionSearchForm.searchForGene;
	f(geneId);
};

/*
 * instance methods...
 */
Ext.extend(Ext.Gemma.CoexpressionGrid, Ext.Gemma.GemmaGridPanel, {

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
			var imageWidth = Ext.Gemma.CoexpressionGrid.bitImageBarWidth
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
Ext.Gemma.CoexpressionGrid.getFoundGeneStyler = function() {
	if (Ext.Gemma.CoexpressionGrid.foundGeneStyler === undefined) {
		Ext.Gemma.CoexpressionGrid.foundGeneTemplate = new Ext.Template(
				"<a href='' onClick='Ext.Gemma.CoexpressionGrid.searchForGene({id}); return false;'>",
				"<img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' />",
				"</a>",
				" &nbsp; ",
				"<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}");
		Ext.Gemma.CoexpressionGrid.foundGeneStyler = function(value, metadata,
				record, row, col, ds) {
			var g = record.data.foundGene;
			if (g.officialName === null) {
				g.officialName = "";
			}
			return Ext.Gemma.CoexpressionGrid.foundGeneTemplate.apply(g);
		};
	}
	return Ext.Gemma.CoexpressionGrid.foundGeneStyler;
};

Ext.Gemma.CoexpressionGrid.getSupportStyler = function() {
	if (Ext.Gemma.CoexpressionGrid.supportStyler === undefined) {
		Ext.Gemma.CoexpressionGrid.supportStyler = function(value, metadata,
				record, row, col, ds) {
			var d = record.data;
			if (d.posLinks || d.negLinks) {
				var s = "";
				if (d.posLinks) {
					s = s
							+ String
									.format(
											"<span class='positiveLink'>{0}{1}</span> ",
											d.posLinks,
											Ext.Gemma.CoexpressionGrid
													.getSpecificLinkString(
															d.posLinks,
															d.nonSpecPosLinks));
				}
				if (d.negLinks) {
					s = s
							+ String
									.format(
											"<span class='negativeLink'>{0}{1}</span> ",
											d.negLinks,
											Ext.Gemma.CoexpressionGrid
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
	return Ext.Gemma.CoexpressionGrid.supportStyler;
};

Ext.Gemma.CoexpressionGrid.getSpecificLinkString = function(total, nonSpecific) {
	return nonSpecific
			? String.format("<span class='specificLink'> ({0})</span>", total
					- nonSpecific)
			: "";
};

Ext.Gemma.CoexpressionGrid.getGoStyler = function() {
	if (Ext.Gemma.CoexpressionGrid.goStyler === undefined) {
		Ext.Gemma.CoexpressionGrid.goStyler = function(value, metadata, record,
				row, col, ds) {
			var d = record.data;
			if (d.goSim || d.maxGoSim) {
				return String.format("{0}/{1}", d.goSim, d.maxGoSim);
			} else {
				return "-";
			}
		};
	}
	return Ext.Gemma.CoexpressionGrid.goStyler;
};

Ext.Gemma.CoexpressionGrid.bitImageBarWidth = 1;
Ext.Gemma.CoexpressionGrid.bitImageBarHeight = 10;

Ext.Gemma.CoexpressionGrid.getBitImageMapTemplate = function() {
	if (Ext.Gemma.CoexpressionGrid.bitImageMapTemplate === undefined) {
		Ext.Gemma.CoexpressionGrid.bitImageMapTemplate = new Ext.XTemplate(
				'<tpl for=".">',
				'<area shape="rect" coords="{[ (xindex - 1) * this.barx ]},0,{[ xindex * this.barx ]},{[ this.bary ]}" ext:qtip="{name}" href="{externalUri}" />',
				'</tpl>', {
					barx : Ext.Gemma.CoexpressionGrid.bitImageBarWidth,
					bary : Ext.Gemma.CoexpressionGrid.bitImageBarHeight - 1
				});
	}
	return Ext.Gemma.CoexpressionGrid.bitImageMapTemplate;
};

Ext.Gemma.CoexpressionGrid.getBitImageStyler = function() {
	if (Ext.Gemma.CoexpressionGrid.bitImageStyler === undefined) {
		Ext.Gemma.CoexpressionGrid.bitImageStyler = function(value, metadata,
				record, row, col, ds) {
			var bits = record.data.datasetVector;
			var width = Ext.Gemma.CoexpressionGrid.bitImageBarWidth
					* bits.length;
			var height = Ext.Gemma.CoexpressionGrid.bitImageBarHeight;
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
	return Ext.Gemma.CoexpressionGrid.bitImageStyler;
};
