Ext.namespace('Ext.Gemma');

/*
 * Ext.Gemma.DiffExpressionGrid constructor... config is a hash with the
 * following options:
 */
Ext.Gemma.DiffExpressionGrid = Ext.extend(Ext.Gemma.GemmaGridPanel, {

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
		type: "float"
	}, {
		name : "activeExperiments"
	}, {
		name : "numSearchedExperiments",
		type: "int"
	},{
		name:  "sortKey",
		type: "string"
	},{
		name: "probeResults"
	}]),

	initComponent : function() {
		if (this.pageSize) {
			Ext.apply(this, {
				store : new Ext.Gemma.PagingDataStore({
					proxy : new Ext.data.MemoryProxy([]),
					reader : new Ext.data.ListRangeReader({
						id : "id"
					}, this.record),
					pageSize : this.pageSize
				})
			});
			Ext.apply(this, {
				bbar : new Ext.Gemma.PagingToolbar({
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
				header : "Query Gene",
				dataIndex : "gene",
				sortable: false
			}, {
				id : 'fisherPValue',
				dataIndex: "fisherPValue",
				header : "metaP",
				sortable: false,
				width : 75
			}, 
			{
				id : 'activeExperiments',
				dataIndex: "activeExperiments",
				header : "support",
				sortable: false,
				width : 75,
				renderer :Ext.Gemma.DiffExpressionGrid.getSupportStyler()
			}, 
			]
		});

		Ext.apply(this, {
				rowExpander : new Ext.Gemma.DiffExpressionGridRowExpander({
					tpl : ""
				})
			});
			this.columns.unshift(this.rowExpander);
			Ext.apply(this, {
				plugins : this.rowExpander
			});

		Ext.Gemma.DiffExpressionGrid.superclass.initComponent.call(this);

		this.originalTitle = this.title;
	},

	loadData : function(results) {

		this.rowExpander.clearCache();
		//this.datasets = datasets; // the datasets that are 'relevant'.
		this.getStore().proxy.data = results;
		this.getStore().reload({
			resetPage : true
		});
		this.getView().refresh(true); // refresh column headers
		//this.resizeDatasetColumn();
	},

	resizeDatasetColumn : function() {
		var first = this.getStore().getAt(0);
		if (first) {
			var cm = this.getColumnModel();
			var c = cm.getIndexById('datasets');
			var headerWidth = this.view.getHeaderCell(c).firstChild.scrollWidth;
			var imageWidth = Ext.Gemma.DiffExpressionGrid.bitImageBarWidth
					* first.data.datasetVector.length;
			cm.setColumnWidth(c, imageWidth < headerWidth
					? headerWidth
					: imageWidth);
		}
	}

});

Ext.Gemma.DiffExpressionGrid.searchForGene = function(geneId) {
	var f = Ext.Gemma.DiffExpressionSearchForm.searchForGene;
	f(geneId);
};

/*
 * Stylers
 * 
 */
Ext.Gemma.DiffExpressionGrid.getFoundGeneStyler = function() {
	if (Ext.Gemma.DiffExpressionGrid.foundGeneStyler === undefined) {
		Ext.Gemma.DiffExpressionGrid.foundGeneTemplate = new Ext.Template(
				"<a href='' onClick='Ext.Gemma.DiffExpressionGrid.searchForGene({id}); return false;'>",
				"<img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' />",
				"</a>",
				" &nbsp; ",
				"<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}");
		Ext.Gemma.DiffExpressionGrid.foundGeneStyler = function(value,
				metadata, record, row, col, ds) {
			var g = record.data.foundGene;
			if (g.officialName === null) {
				g.officialName = "";
			}
			return Ext.Gemma.DiffExpressionGrid.foundGeneTemplate.apply(g);
		};
	}
	return Ext.Gemma.DiffExpressionGrid.foundGeneStyler;
};

Ext.Gemma.DiffExpressionGrid.getSupportStyler = function() {
	if (Ext.Gemma.DiffExpressionGrid.supportStyler === undefined) {
		Ext.Gemma.DiffExpressionGrid.supportStyler = function(value, metadata,
				record, row, col, ds) {
			var d = record.data;
			return String.format("{0}/{1}", d.activeExperiments.size(), d.numSearchedExperiments) ;
		};
	}
	return Ext.Gemma.DiffExpressionGrid.supportStyler;
};
