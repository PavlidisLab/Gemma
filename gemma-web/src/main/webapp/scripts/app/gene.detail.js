Ext.namespace('Gemma');

Gemma.DIFF_THRESHOLD = 0.01;
Gemma.MAX_DIFF_RESULTS = 25;

/**
 * 
 */
Gemma.GeneGOGrid = Ext
		.extend(
				Gemma.GemmaGridPanel,
				{
					record : Ext.data.Record.create( [ {
						name : "id",
						type : "int"
					}, {
						name : "termUri"
					}, {
						name : "termName"
					}, {
						name : "evidenceCode"
					} ]),

					golink : function(d) {
						var g = d.replace("_", ":");
						return "<a target='_blank' href='http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&query="
								+ g + "'>" + g + "</a>";
					},

					initComponent : function() {
						Ext.apply(this, {
							columns : [ {
								header : "ID",
								dataIndex : "termUri",
								renderer : this.golink
							}, {
								header : "Term",
								dataIndex : "termName"
							}, {
								header : "Evidence Code",
								dataIndex : "evidenceCode"
							} ],

							store : new Ext.data.Store( {
								proxy : new Ext.data.DWRProxy(
										GeneController.findGOTerms),
								reader : new Ext.data.ListRangeReader( {
									id : "id"
								}, this.record),
								remoteSort : false
							})
						});

						Gemma.GeneGOGrid.superclass.initComponent.call(this);

						this.getStore().setDefaultSort('termUri');

						this.getStore().load( {
							params : [ this.geneid ]
						});
					}

				});

/**
 * 
 */
Gemma.GeneProductGrid = Ext.extend(Gemma.GemmaGridPanel, {

	record : Ext.data.Record.create( [ {
		name : "id"
	}, {
		name : "name"
	}, {
		name : "description"
	}, {
		name : "type",
		convert : function(d) {
			return d.value;
		}.createDelegate()
	} ]),

	initComponent : function() {
		Ext.apply(this, {
			columns : [ {
				header : "Name",
				dataIndex : "name"
			}, {
				header : "Type",
				dataIndex : "type"
			}, {
				header : "Description",
				dataIndex : "description"
			} ],

			store : new Ext.data.Store( {
				proxy : new Ext.data.DWRProxy(GeneController.getProducts),
				reader : new Ext.data.ListRangeReader( {
					id : "id"
				}, this.record),
				remoteSort : false
			})
		});

		Gemma.GeneProductGrid.superclass.initComponent.call(this);

		this.getStore().setDefaultSort('type', 'name');

		this.getStore().load( {
			params : [ this.geneid ]
		});
	}

});

Ext.onReady( function() {

	Ext.QuickTips.init();

	geneid = dwr.util.getValue("gene");

	var gpGrid = new Gemma.GeneProductGrid( {
		geneid : geneid,
		renderTo : "geneproduct-grid",
		height : 200,
		width : 400
	});

	var gogrid = new Gemma.GeneGOGrid( {
		renderTo : "go-grid",
		geneid : geneid,
		height : 200,
		width : 500
	});

	// Coexpression grid.

		var coexpressedGeneGrid = new Gemma.CoexpressionGridLite( {
			width : 400,
			colspan : 2,
			// user : false,
			renderTo : "coexpression-grid"
		});

		coexpressedGeneGrid.doSearch( {
			geneIds : [ geneid ],
			quick : true,
			stringency : 2,
			forceProbeLevelSearch : false
		});

		// diff expression grid

		var diffExGrid = new Gemma.ProbeLevelDiffExGrid( {
			width : 650,
			height : 200,
			renderTo : "diff-grid"
		});

		var eeNameColumnIndex = diffExGrid.getColumnModel().getIndexById(
				'expressionExperimentName');
		diffExGrid.getColumnModel().setHidden(eeNameColumnIndex, true);
		var visColumnIndex = diffExGrid.getColumnModel().getIndexById(
				'visualize');
		diffExGrid.getColumnModel().setHidden(visColumnIndex, false);

		diffExGrid.getStore().load( {
			params : [ geneid, Gemma.DIFF_THRESHOLD, Gemma.MAX_DIFF_RESULTS /*
														 * how many results to
														 * return
														 */]
		});

		var visDifWindow = null;

		diffExGrid.geneDiffRowClickHandler = function(grid, rowIndex,
				columnIndex, e) {
			if (diffExGrid.getSelectionModel().hasSelection()) {

				var record = diffExGrid.getStore().getAt(rowIndex);
				var fieldName = diffExGrid.getColumnModel().getDataIndex(
						columnIndex);
				var gene = record.data.gene;
				if (fieldName == 'visualize') {

					var ee = record.data.expressionExperiment;

					if (visDifWindow != null)
						visDifWindow.close();

					visDifWindow = new Gemma.VisualizationDifferentialWindow();

					visDifWindow.dv.store = new Gemma.VisualizationStore(
							{
								readMethod : DEDVController.getDEDVForDiffExVisualizationByExperiment
							});

					visDifWindow.displayWindow = function(ee) {

						this.setTitle("Visualization of probes in:  "
								+ ee.shortName);

						var downloadDedvLink = String
								.format(
										"<a ext:qtip='Download raw data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1}' > <img src='/Gemma/images/download.gif'/></a>",
										ee.id, geneid);

						this.thumbnailPanel.setTitle("Thumbnails &nbsp; "
								+ downloadDedvLink);

						var params = [];
						params.push(ee.id);
						params.push(geneid); // Gene would be nicer but
						// don't
						// have access to the full
						// object... hmmmm
						params.push(Gemma.DIFF_THRESHOLD);

						this.show();
						this.dv.store.load( {
							params : params
						});

					}.createDelegate(visDifWindow); // Without the
					// createDelegate IE has
					// fails to re-render the
					// zoom panel

					visDifWindow.displayWindow(ee, geneid);

				}
			}
		};

		diffExGrid.on("cellclick", diffExGrid.geneDiffRowClickHandler
				.createDelegate(visDifWindow), diffExGrid);

	});

Gemma.geneLinkOutPopUp = function(abaImageUrl) {

	// TODO put a null, empty string check in.
	win = new Ext.Window( {
		html : "<img src='" + abaImageUrl + "'>",
		width : 500,
		height : 400,
		autoScroll : true
	});
	win
			.setTitle("<img height=15  src='/Gemma/images/abaExpressionLegend.gif'>");
	win.show(this);

};
