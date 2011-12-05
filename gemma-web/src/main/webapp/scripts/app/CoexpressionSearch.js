Ext.namespace("Gemma");
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var admin = Ext.get('hasAdmin').getValue();
	var user = Ext.get('hasUser').getValue();

	if (Ext.isIE7) {
		Ext.DomHelper.append('coexpression-all', {
			tag : 'p',
			cls : 'trouble',
			html : Gemma.HelpText.CommonWarnings.BrowserWarnings.ie7
		});
	}

	var searchPanel = new Gemma.CoexpressionSearchForm({
				admin : admin,
				user : user,
				id : 'searchpanel'
			});

	var summaryPanel = new Ext.Panel({
				width : 300,
				id : 'summarypanel',
				height : 300
			});

	var knownGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
				width : 800,
				colspan : 2
			});

	var knownGeneGrid = new Gemma.CoexpressionGrid({
				width : 800,
				height : 400,
				title : "Coexpressed genes",
				colspan : 2,
				user : user

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
					adjective : "predicted gene",
					collapsed : true
				});

		predictedGeneGrid = new Gemma.CoexpressionGrid({
					width : 800,
					title : "Coexpressed predicted genes",
					id : 'pred-gene-grid',
					colspan : 2,
					height : 400
				});

		probeAlignedDatasetGrid = new Gemma.CoexpressionDatasetGrid({
					width : 800,
					colspan : 2,
					id : 'par-dataset-grid',
					adjective : "probe-aligned region",
					collapsed : true
				});

		probeAlignedGrid = new Gemma.CoexpressionGrid({
					width : 800,
					colspan : 2,
					height : 400,
					title : "Coexpressed probe-aligned regions"
				});

		items.push(predictedGeneDatasetGrid);
		items.push(predictedGeneGrid);
		items.push(probeAlignedDatasetGrid);
		items.push(probeAlignedGrid);

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

	var predictedGeneDatasetGrid;
	var probeAlignedDatasetGrid;
	var predictedGeneGrid;
	var probeAlignedGrid;
	var visWindow;
	var detailsWindow;

	/*
	 * Handler for the coexpression search results.
	 */
	searchPanel.on("aftersearch", function(panel, result) {

		Ext.DomHelper.overwrite('summarypanel', "");
		
		/*
		 * Report any errors.
		 */
		if (result.errorState) {
			Ext.DomHelper.overwrite('coexpression-messages', result.errorState);
			knownGeneGrid.getStore().removeAll();
			return;
		}

		var eeMap = {};
		if (result.datasets) {
			for (var i = 0; i < result.datasets.length; ++i) {
				var ee = result.datasets[i];
				eeMap[ee.id] = ee;
			}
		}

		
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
								"Coexpressed genes &nbsp;&nbsp;&nbsp;<a href='{0}' title='bookmarkable link'><img src=\"/Gemma/images/icons/link.png\" alt='bookmark'/></a>&nbsp; <a href='{0}&export' title='download'><img src=\"/Gemma/images/download.gif\" alt='download'/></a>",
								link));

		Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.knownGeneDatasets, eeMap);
		knownGeneDatasetGrid.loadData(result.knownGeneDatasets);
		knownGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
				result.knownGeneDatasets, result.knownGeneResults, Gemma.CoexValueObjectUtil.getCurrentQueryGeneIds(result.queryGenes));

		if (admin) {
			Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.predictedGeneDatasets, eeMap);
			predictedGeneDatasetGrid.loadData(result.predictedGeneDatasets);
			predictedGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.predictedGeneResults,
					result.predictedGeneDatasets);

			Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.probeAlignedRegionDatasets, eeMap);
			probeAlignedDatasetGrid.loadData(result.probeAlignedRegionDatasets);
			probeAlignedGrid.loadData(result.isCannedAnalysis, result.queryGenes.length,
					result.probeAlignedRegionResults, result.probeAlignedRegionDatasets);
		}
	});

});
