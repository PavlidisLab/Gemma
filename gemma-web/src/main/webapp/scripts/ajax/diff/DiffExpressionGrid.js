Ext.namespace('Gemma');

/*
 * Gemma.DiffExpressionGrid constructor... config is a hash with the following options:
 */
Gemma.DiffExpressionGrid = Ext.extend(Gemma.GemmaGridPanel, {

	collapsible : true,
	editable : false,
	autoHeight : true,
	width : 600,
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
		name : "numExperimentsInScope",
		type : "int"
	}, {
		name : "numMetThreshold",
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
				tooltip : "Combined p-value for the studies you chose, using the Fisher method.",
				renderer : function(p) {
					if (p < 0.0001) {
						return sprintf("%.3e", p);
					} else {
						return sprintf("%.3f", p);
					}
				},
				sortable : true,
				width : 75
			}, {
				id : 'activeExperiments',
				dataIndex : "activeExperiments",
				header : "# Datasets Tested In",
				sortable : false,
				width : 75,
				tooltip : "# datasets with diff evidence for the gene  / num datasets with diff results in the scope / num datasets in the scope",
				renderer : Gemma.DiffExpressionGrid.getSupportStyler()
			}, {
				id : 'numSignificant',
				dataIndex : "numMetThresholdt",
				header : "# Significant Probes",
				sortable : true,
				width : 75,
				tooltip : "How many probes met the q-value threshold you selected / num datasets with diff evidence for gene",
				renderer : Gemma.DiffExpressionGrid.getMetThresholdStyler()
			}]
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
			var imageWidth = Gemma.DiffExpressionGrid.bitImageBarWidth * first.data.datasetVector.length;
			cm.setColumnWidth(c, imageWidth < headerWidth ? headerWidth : imageWidth);
		}
	}

});

/**
 * 
 * @param {}
 *            geneId
 */
Gemma.DiffExpressionGrid.searchForGene = function(geneId) {
	var f = Gemma.DiffExpressionSearchForm.searchForGene;
	f(geneId);
};

/**
 * 
 * @return {}
 */
Gemma.DiffExpressionGrid.getSupportStyler = function() {
	if (Gemma.DiffExpressionGrid.supportStyler === undefined) {
		Gemma.DiffExpressionGrid.supportStyler = function(value, metadata, record, row, col, ds) {
			var d = record.data;
			return String.format("{0}/{1}/{2}", d.activeExperiments.size(), d.numSearchedExperiments,
					d.numExperimentsInScope);
		};
	}
	return Gemma.DiffExpressionGrid.supportStyler;
};

/**
 * 
 * @return {}
 */
Gemma.DiffExpressionGrid.getMetThresholdStyler = function() {
	if (Gemma.DiffExpressionGrid.metThresholdStyler === undefined) {
		Gemma.DiffExpressionGrid.metThresholdStyler = function(value, metadata, record, row, col, ds) {
			var d = record.data;
			return String.format("{0}/{1}", d.numMetThreshold, d.probeResults.size());
		};
	}
	return Gemma.DiffExpressionGrid.metThresholdStyler;
};
