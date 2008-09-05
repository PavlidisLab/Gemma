Ext.namespace('Gemma');

/*
 * Gemma.DiffExpressionGrid constructor... config is a hash with the following options:
 */
Gemma.DiffExpressionGrid = Ext.extend(Ext.grid.GridPanel, {

	width : 800,
	collapsible : true,
	editable : false,
	autoHeight : true,
	style : "margin-bottom: 1em;",

	viewConfig : {
		forceFit : true,
		emptyText : "Results will be displayed here"
	},

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

		Ext.apply(this, {
			plugins : this.rowExpander
		});

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
			rowExpander : new Gemma.DiffExpressionGridRowExpander({
				tpl : "<div height='300px'></div>",
				grid : this
			})
		});

		Ext.apply(this, {
			columns : [this.rowExpander, {
				id : 'gene',
				dataIndex : "gene",
				header : "Query Gene",
				sortable : false
			}, {
				id : 'fisherPValue',
				dataIndex : "fisherPValue",
				header : "Meta P-Value",
				tooltip : "Combined p-value for the datasets you chose, using Fisher's method.",
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
				tooltip : "# datasets testing the gene  / # with diff analysis / # in set",
				renderer : this.supportStyler
			}, {
				id : 'numSignificant',
				dataIndex : "numMetThreshold",
				header : "# Significant",
				sortable : true,
				width : 75,
				tooltip : "How many datasets met the q-value threshold you selected / # testing gene",
				renderer : this.metThresholdStyler
			},
			{
				id : 'dedvData',
				header : 'visulize', 
				width : 75, 
				tooltip:  "click button to visulize the data",
				renderer : this.visulize
			}
			]
		});

		Ext.apply(this, {
			plugins : this.rowExpander
		});

		Gemma.DiffExpressionGrid.superclass.initComponent.call(this);

		this.originalTitle = this.title;
	},

	loadData : function(results) {
		this.rowExpander.clearCache();
		this.getStore().proxy.data = results;
		this.getStore().reload({
			resetPage : true
		});
		this.getView().refresh(true); // refresh column headers
	},

	metThresholdStyler : function(value, metadata, record, row, col, ds) {
		var d = record.data;
		return String.format("{0}/{1}", d.numMetThreshold, d.activeExperiments.size());
	},

	supportStyler : function(value, metadata, record, row, col, ds) {
		var d = record.data;
		return String.format("{0}/{1}/{2}", d.activeExperiments.size(), d.numSearchedExperiments,
				d.numExperimentsInScope);
	},
	
	anchor_test : function(){
		window.alert("This is an anchor test.")
	},
	
	visulize : function(value, metadata, record, row, col, ds) {
		var d = record.data;
		var activeExperimentsString = "";
		for(i=0; i< d.activeExperiments.size(); i++){
			if ( i  == 0) 
				activeExperimentsString =  d.activeExperiments[i].id;
			else
				activeExperimentsString = activeExperimentsString + ", " + d.activeExperiments[i].id;
		}
		
		var geneId = d.probeResults[0].gene.id;

		return "<a href='javascript: DEDVController.getDEDV([" +activeExperimentsString +"],["+ geneId+"])'> Visulize </a>";
	}
	
});
