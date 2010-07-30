Ext.namespace('Gemma');

/*
 * Gemma.DiffExpressionGrid constructor... config is a hash with the following options:
 */
Gemma.DiffExpressionGrid = Ext.extend(Ext.grid.GridPanel, {

	width : 800,
	height : 400,
	collapsible : true,
	editable : false,
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

		Ext.apply(this, {
					store : new Ext.data.Store({
								proxy : new Ext.data.MemoryProxy(this.records),
								reader : new Ext.data.ListRangeReader({}, this.record)
							})
				});

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
							return String.format(
									" &nbsp; <a href='/Gemma/gene/showGene.html?id={0}' ext:qtip='{1}'> {2} </a> ",
									value.id, value.officialName, value.officialSymbol);
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
					}, {
						id : 'linkOut',
						dataIndex : "gene",
						header : "Out links",
						sortable : false,
						width : 30,
						tooltip : "Links to other websites for more relevent information (if relevent and/or available)",
						renderer : this.linkOutStyler
					}]
		});

		Gemma.DiffExpressionGrid.superclass.initComponent.call(this);

		this.originalTitle = this.title;

		this.on("cellclick", this.rowClickHandler.createDelegate(this), this);
	},

	rowClickHandler : function(grid, rowIndex, columnIndex, e) {

		if (this.getSelectionModel().hasSelection()) {

			var record = this.getStore().getAt(rowIndex);
			var fieldName = this.getColumnModel().getDataIndex(columnIndex);
			var gene = record.data.gene;

			if (fieldName == 'visualize') {

				// destroy if already open
				if (this.visWindow) {
					this.visWindow.close();
				}

				var activeExperiments = record.data.activeExperiments;
				var activeExperimentIds = [];

				for (var i = 0; i < activeExperiments.size(); i++) {
					/*
					 * FIXME properly handle subsets.
					 */
					activeExperimentIds.push(activeExperiments[i].sourceExperiment
							? activeExperiments[i].sourceExperiment
							: activeExperiments[i].id);
				}

				var downloadDedvLink = String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1}", activeExperimentIds
								.join(','), gene.id);

				this.visWindow = new Gemma.VisualizationDifferentialWindow({
							readMethod : DEDVController.getDEDVForDiffExVisualization,
							downloadLink : downloadDedvLink,
							title : "Differential expression of " + gene.officialSymbol
						});

				this.visWindow.show({
							params : [activeExperimentIds, [gene.id], Ext.getCmp('thresholdField').getValue(),
									this.searchPanel.efChooserPanel.eeFactorsMap]
						});
			} else if (fieldName == 'details') {

				// destroy if already open
				if (this.detailsWindow) {
					this.detailsWindow.close();
				}

				var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
							width : 750,
							height : 300
						});

				this.detailsWindow = new Ext.Window({
							modal : false,
							layout : 'fit',
							title : 'Details for ' + gene.officialSymbol,
							closeAction : 'close',
							items : [diffExGrid],
							width : 750,
							height : 400
						});

				this.detailsWindow.show();

				var supporting = record.data.probeResults;
				diffExGrid.getStore().loadData(supporting);

			}
		}

	},

	loadData : function(results) {
		this.getStore().proxy.data = results;
		this.getStore().reload({
					resetPage : true
				});
		this.getView().refresh(true); // refresh column headers
	},

	linkOutStyler : function(value, metadata, record, row, col, ds) {
		var popUpWin = "LinkOutController.getAllenBrainAtlasLink('" + value.officialSymbol
				+ "',Gemma.DiffExpressionGrid.linkOutPopUp)";
		return String
				.format(
						'<a title="Allen Brain Atlas Image"  onClick="{0}"> <img src="/Gemma/images/logo/aba-icon.png" ext:qtip="Link to Allen Brain Atlas details" /> </a>',
						popUpWin);
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

		return String.format(
				"<a href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1}' > <img src='/Gemma/images/download.gif'/> </a>",
				activeExperimentsString, geneId);
	}

});

Gemma.DiffExpressionGrid.linkOutPopUp = function(linkOutValueObject) {

	// TODO: Make pop up window show more than one image (have a button for scrolling to next image)
	var popUpHtml;

	if (linkOutValueObject == null) {
		window.alert("No Allen Brain Atlas details available for this gene");
		return;
	} else {
		popUpHtml = String.format("<a href='{0}' target='_blank' > <img height=200 width=400 src={1}> </a>",
				linkOutValueObject.abaGeneUrl, linkOutValueObject.abaGeneImageUrls[0]);
	}

	var abaWindowId = "diffExpressionAbaWindow";

	var popUpLinkOutWin = Ext.getCmp(abaWindowId);
	if (popUpLinkOutWin != null) {
		popUpLinkOutWin.close();
		popUpLinkOutWin = null;
	}

	popUpLinkOutWin = new Ext.Window({
				id : abaWindowId,
				stateful : false,
				html : popUpHtml,
				resizable : false
			});
	popUpLinkOutWin.setTitle("<img height=15 src=/Gemma/images/abaExpressionLegend.gif>");

	popUpLinkOutWin.show(this);

};
