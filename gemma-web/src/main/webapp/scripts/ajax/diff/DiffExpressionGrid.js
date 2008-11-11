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
			stateful : false,

			viewConfig : {
				forceFit : true,
				emptyText : "Results will be displayed here"
			},

			record : Ext.data.Record.create([{
						name : "gene"
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
										dataIndex : 'visualize',
										renderer : this.visStyler.createDelegate(this),
										tooltip : "Links for visualizing raw data",
										sortable : false,
										width : 30
									}, {
										id : 'gene',
										dataIndex : "gene",
										header : "Query Gene",
										renderer : function(value, metadata, record, row, col, ds) {
											return value.officialSymbol;
										},
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
									}]
						});

				Gemma.DiffExpressionGrid.superclass.initComponent.call(this);

				this.originalTitle = this.title;
			},

			loadData : function(results) {
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

			anchor_test : function() {
				window.alert("This is an anchor test.")
			},

			visStyler : function(value, metadata, record, row, col, ds) {
				return "<img src='/Gemma/images/icons/chart_curve.png' ext:qtip='Visualize the data' />";
			},

			detailsStyler : function(value, metadata, record, row, col, ds) {
				return "<img src='/Gemma/images/icons/magnifier.png' ext:qtip='Show probe-level details' />";
			},

			downloadDedv : function(value, metadata, record, row, col, ds) {
				var d = record.data;
				var activeExperimentsString = "";
				var activeExperimentsSize = record.data.activeExperiments.size();
				for (var i = 0; i < activeExperimentsSize; i++) {
					if (i === 0) {
						activeExperimentsString = record.data.activeExperiments[i].id;
					} else {
						activeExperimentsString = String.format("{0}, {1}", activeExperimentsString,
								record.data.activeExperiments[i].id);
					}
				}

				var geneId = record.data.probeResults[0].gene.id;

				return String.format("<a href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1}' > download </a>",
						activeExperimentsString, geneId);
			}

		});
