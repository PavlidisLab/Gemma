Ext.namespace('Gemma');

/*
 * Gemma.DiffExpressionGrid constructor... config is a hash with the following
 * options:
 */
Gemma.DiffExpressionGrid = Ext.extend(Gemma.GemmaGridPanel, {

	collapsible : true,
	editable : false,
	autoHeight : true,
	style : 'margin-bottom: 1em;',

	record : Ext.data.Record.create([{
		name : "gene",
		convert : function(g) {
			return g.officialSymbol;
		}
	}, {
		name : "fisherPValue",
		type : "float"
	}, {
		name : "activeExperiments"
	}, {
		name : "numSearchedExperiments",
		type : "int"
	}, {
		name : "sortKey",
		type : "string"
	}, {
		name : "probeResults"
	}]),

	initComponent : function() {
		if (this.pageSize) {
			Ext.apply(this, {
				store : new Gemma.PagingDataStore({
					proxy : new Ext.data.MemoryProxy([]),
					reader : new Ext.data.ListRangeReader({
						id : "id"
					}, this.record),
					pageSize : this.pageSize
				})
			});
			Ext.apply(this, {
				bbar : new Gemma.PagingToolbar({
					pageSize : this.pageSize,
					store : this.store
				})
			});
		} else {
			Ext.apply(this, {
				store : new Ext.data.Store({
					proxy : new Ext.data.MemoryProxy(this.records),
					reader : new Ext.data.ListRangeReader({}, this.record)
				})
			});
		}

		Ext.apply(this, {
			columns : [{
				id : 'gene',
				dataIndex : "gene",
				header : "Query Gene",
				sortable : false
			}, {
				id : 'fisherPValue',
				dataIndex : "fisherPValue",
				header : "Meta P-Value",
				toolTip : "Combined p-value for the studies you chose, using the Fisher method.",
				renderer : function(p) {
					if (p < 0.0001) {
						return sprintf("%.3e", p);
					} else {
						return sprintf("%.3f", p)
					}
				},
				sortable : true,
				width : 75
			}, {
				id : 'activeExperiments',
				dataIndex : "activeExperiments",
				header : "Support",
				sortable : false,
				width : 75,
				toolTip : "How many experiments met the q-value threshold you selected / how many were tested.",
				renderer : Gemma.DiffExpressionGrid.getSupportStyler()
			},]
		});

		Ext.apply(this, {
			rowExpander : new Gemma.DiffExpressionGridRowExpander({
				tpl : ""
			})
		});
		this.columns.unshift(this.rowExpander);
		Ext.apply(this, {
			plugins : this.rowExpander
		});

		Gemma.DiffExpressionGrid.superclass.initComponent.call(this);

		this.originalTitle = this.title;
	},

	loadData : function(results) {

		this.rowExpander.clearCache();
		// this.datasets = datasets; // the datasets that are 'relevant'.
		this.getStore().proxy.data = results;
		this.getStore().reload({
			resetPage : true
		});
		this.getView().refresh(true); // refresh column headers
		// this.resizeDatasetColumn();
	},

	resizeDatasetColumn : function() {
		var first = this.getStore().getAt(0);
		if (first) {
			var cm = this.getColumnModel();
			var c = cm.getIndexById('datasets');
			var headerWidth = this.view.getHeaderCell(c).firstChild.scrollWidth;
			var imageWidth = Gemma.DiffExpressionGrid.bitImageBarWidth
					* first.data.datasetVector.length;
			cm.setColumnWidth(c, imageWidth < headerWidth
					? headerWidth
					: imageWidth);
		}
	}

});

Gemma.DiffExpressionGrid.searchForGene = function(geneId) {
	var f = Gemma.DiffExpressionSearchForm.searchForGene;
	f(geneId);
};

/*
 * Stylers
 * 
 */
Gemma.DiffExpressionGrid.getFoundGeneStyler = function() {
	if (Gemma.DiffExpressionGrid.foundGeneStyler === undefined) {
		Gemma.DiffExpressionGrid.foundGeneTemplate = new Ext.Template(
				"<a href='' onClick='Gemma.DiffExpressionGrid.searchForGene({id}); return false;'>",
				"<img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' />",
				"</a>",
				" &nbsp; ",
				"<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}");
		Gemma.DiffExpressionGrid.foundGeneStyler = function(value, metadata,
				record, row, col, ds) {
			var g = record.data.foundGene;
			if (g.officialName === null) {
				g.officialName = "";
			}
			return Gemma.DiffExpressionGrid.foundGeneTemplate.apply(g);
		};
	}
	return Gemma.DiffExpressionGrid.foundGeneStyler;
};

Gemma.DiffExpressionGrid.getSupportStyler = function() {
	if (Gemma.DiffExpressionGrid.supportStyler === undefined) {
		Gemma.DiffExpressionGrid.supportStyler = function(value, metadata,
				record, row, col, ds) {
			var d = record.data;
			return String.format("{0}/{1}", d.activeExperiments.size(),
					d.numSearchedExperiments);
		};
	}
	return Gemma.DiffExpressionGrid.supportStyler;
};
