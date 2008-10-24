Ext.namespace("Gemma");
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var admin = dwr.util.getValue("hasAdmin");

	if (Ext.isIE && !Ext.isIE7) {
		Ext.DomHelper.append('coexpression-all', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page displays improperly in older versions of Internet Explorer.  Please upgrade to Internet Explorer 7.'
		});
	}

	var geneRowClickHandler = function(grid, rowIndex, columnIndex, e) {
		if (this.getSelectionModel().hasSelection()) {

			var record = this.getStore().getAt(rowIndex);
			var fieldName = this.getColumnModel().getDataIndex(columnIndex);

			if (fieldName == 'foundGene') {
				searchPanel.searchForGene(record.get("foundGene").id);
			} else if (fieldName == 'visualize') {
				var queryGene = record.data.queryGene;
				var foundGene = record.data.foundGene;
				var activeExperiments = record.data.supportingExperiments;

				// destroy if already open
				if (visWindow) {
					visWindow.close();
				}

				visWindow = new Gemma.VisualizationWindow({
							admin : admin
						});
				visWindow.displayWindow(activeExperiments, queryGene, foundGene);
			}
		}
	};

	var searchPanel = new Gemma.CoexpressionSearchForm({
				admin : admin,
				id : 'searchpanel'
			});

	var summaryPanel = new Ext.Panel({
				width : 300,
				id : 'summarypanel',
				height : 300
			});

	var knownGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
				width : 800,
				autoHeight : true,
				colspan : 2
			});

	var knownGeneGrid = new Gemma.CoexpressionGrid({
				width : 800,
				title : "Coexpressed genes",
				autoHeight : true,
				pageSize : 25,
				colspan : 2
			});

	var items = [searchPanel, summaryPanel, knownGeneDatasetGrid, knownGeneGrid];

	if (admin) {

		/*
		 * Note: doing allPanel.add doesn't work. Probably something to do with the table layout; indeed:
		 * https://extjs.com/forum/showthread.php?p=173090
		 */

		predictedGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
					width : 800,
					colspan : 2,
					autoHeight : true,
					adjective : "predicted gene",
					collapsed : true
				});

		predictedGeneGrid = new Gemma.CoexpressionGrid({
					width : 800,
					title : "Coexpressed predicted genes",
					pageSize : 25,
					id : 'pred-gene-grid',
					colspan : 2,
					autoHeight : true
				});

		probeAlignedDatasetGrid = new Gemma.CoexpressionDatasetGrid({
					width : 800,
					colspan : 2,
					autoHeight : true,
					id : 'par-dataset-grid',
					adjective : "probe-aligned region",
					collapsed : true
				});

		probeAlignedGrid = new Gemma.CoexpressionGrid({
					width : 800,
					colspan : 2,
					autoHeight : true,
					title : "Coexpressed probe-aligned regions",
					pageSize : 25
				});

		items.push(predictedGeneDatasetGrid);
		items.push(predictedGeneGrid);
		items.push(probeAlignedDatasetGrid);
		items.push(probeAlignedGrid);

		probeAlignedGrid.on("cellclick", geneRowClickHandler, probeAlignedGrid);
		predictedGeneGrid.on("cellclick", geneRowClickHandler, predictedGeneGrid);
	}

	var allPanel = new Ext.Panel({
				layout : 'table',
				renderTo : 'coexpression-all',
				baseCls : 'x-plain-panel',
				autoHeight : true,
				width : 900,
				layoutConfig : {
					columns : 2
				},
				items : items,
				enabled : false
			});

	searchPanel.geneChooserPanel.toolbar.taxonCombo.on('ready', function() {
				Ext.get('loading').remove();
				Ext.get('loading-mask').fadeOut({
							duration : 0.5,
							remove : true
						});
			});

	var predictedGeneDatasetGrid;
	var probeAlignedDatasetGrid;
	var predictedGeneGrid;
	var probeAlignedGrid;
	var visWindow;

	knownGeneGrid.on("cellclick", geneRowClickHandler, knownGeneGrid);

	/*
	 * Handler for the coexpression search results.
	 */
	searchPanel.on("aftersearch", function(panel, result) {

				/*
				 * Report any errors.
				 */
				if (result.errorState) {
					Ext.DomHelper.overwrite('coexpression-messages', result.errorState);
					return;
				}

				var eeMap = {};
				if (result.datasets) {
					for (var i = 0; i < result.datasets.length; ++i) {
						var ee = result.datasets[i];
						eeMap[ee.id] = ee;
					}
				}

				Ext.DomHelper.overwrite('summarypanel', "");
				summaryPanel = new Gemma.CoexpressionSummaryGrid({
							genes : result.queryGenes,
							renderTo : "summarypanel",
							summary : result.summary
						});

				// create expression experiment image map
				var imageMap = Ext.get("eeMap");
				if (!imageMap) {
					imageMap = Ext.getBody().createChild({
								tag : 'map',
								id : 'eeMap',
								name : 'eeMap'
							});
				}

				Gemma.CoexpressionGrid.getBitImageMapTemplate().overwrite(imageMap, result.datasets);

				var link = panel.getBookmarkableLink();
				knownGeneGrid
						.setTitle(String
								.format(
										"Coexpressed genes <a href='{0}'>(bookmarkable link)</a> <a href='{0}&export'>(export as text)</a>",
										link));

				Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.knownGeneDatasets, eeMap);
				knownGeneDatasetGrid.loadData(result.knownGeneDatasets);
				knownGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
						result.knownGeneDatasets);

				if (admin) {
					Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.predictedGeneDatasets, eeMap);
					predictedGeneDatasetGrid.loadData(result.predictedGeneDatasets);
					predictedGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length,
							result.predictedGeneResults, result.predictedGeneDatasets);

					Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.probeAlignedRegionDatasets, eeMap);
					probeAlignedDatasetGrid.loadData(result.probeAlignedRegionDatasets);
					probeAlignedGrid.loadData(result.isCannedAnalysis, result.queryGenes.length,
							result.probeAlignedRegionResults, result.probeAlignedRegionDatasets);
				}
			});

});
