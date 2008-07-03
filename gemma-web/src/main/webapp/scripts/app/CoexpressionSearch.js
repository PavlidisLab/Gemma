Ext.namespace("Gemma");
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var admin = dwr.util.getValue("hasAdmin");

	if (Ext.isIE && !Ext.isIE7) {
		Ext.DomHelper.append('coexpression-form', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page displays improperly in older versions of Internet Explorer.  Please upgrade to Internet Explorer 7.'
		});
	}

	var searchPanel = new Gemma.CoexpressionSearchForm({});
	searchPanel.render("coexpression-form");

	var summaryPanel;

	var knownGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
		renderTo : "coexpression-results",
		width : 800
	});
	var knownGeneGrid = new Gemma.CoexpressionGrid({
		renderTo : "coexpression-results",
		width : 800,
		title : "Coexpressed genes",
		pageSize : 25
	});
	var predictedGeneGrid;
	var probeAlignedGrid;
	if (admin) {
		var predictedGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
			renderTo : "coexpression-results",
			width : 800,
			adjective : "predicted gene"
		});
		predictedGeneGrid = new Gemma.CoexpressionGrid({
			renderTo : "coexpression-results",
			width : 800,
			title : "Coexpressed predicted genes",
			pageSize : 25,
			collapsed : true
		});
		var probeAlignedDatasetGrid = new Gemma.CoexpressionDatasetGrid({
			renderTo : "coexpression-results",
			width : 800,
			adjective : "probe-aligned region"
		});
		probeAlignedGrid = new Gemma.CoexpressionGrid({
			renderTo : "coexpression-results",
			width : 800,
			title : "Coexpressed probe-aligned regions",
			pageSize : 25,
			collapsed : true
		});
	}

	knownGeneGrid.on("cellclick", function(grid, rowIndex, columnIndex, e) {
		if (this.getSelectionModel().hasSelection()) {

			var record = this.getStore().getAt(rowIndex);
			var fieldName = this.getColumnModel().getDataIndex(columnIndex);

			if (fieldName == 'foundGene') { 
				searchPanel.searchForGene(record.get("foundGene").id);
			}
		}
	}, knownGeneGrid);

	searchPanel.on("aftersearch", function(panel, result) {
		var eeMap = {};
		if (result.datasets) {
			for (var i = 0; i < result.datasets.length; ++i) {
				var ee = result.datasets[i];
				eeMap[ee.id] = ee;
			}
		}

		if (summaryPanel) {
			// grid.destroy() seems to be broken...
			try {
				summaryPanel.destroy();
			} catch (e) {
			}
		}
		summaryPanel = new Gemma.CoexpressionSummaryGrid({
			genes : result.queryGenes,
			renderTo : "coexpression-summary",
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
		Gemma.CoexpressionGrid.getBitImageMapTemplate().overwrite(imageMap,
				result.datasets);

		var link = panel.getBookmarkableLink();
		knownGeneGrid
				.setTitle(String
						.format(
								"Coexpressed genes <a href='{0}'>(bookmarkable link)</a> <a href='{0}&export'>(export as text)</a>",
								link));

		Gemma.CoexpressionDatasetGrid.updateDatasetInfo(
				result.knownGeneDatasets, eeMap);
		knownGeneDatasetGrid.loadData(result.isCannedAnalysis,
				result.queryGenes.length, result.knownGeneDatasets);
		knownGeneGrid.loadData(result.isCannedAnalysis,
				result.queryGenes.length, result.knownGeneResults,
				result.knownGeneDatasets);

		if (admin) {
			Gemma.CoexpressionDatasetGrid.updateDatasetInfo(
					result.predictedGeneDatasets, eeMap);
			predictedGeneDatasetGrid.loadData(result.isCannedAnalysis,
					result.queryGenes.length, result.predictedGeneDatasets);
			predictedGeneGrid.loadData(result.isCannedAnalysis,
					result.queryGenes.length, result.predictedGeneResults,
					result.predictedGeneDatasets);
			Gemma.CoexpressionDatasetGrid.updateDatasetInfo(
					result.probeAlignedRegionDatasets, eeMap);
			probeAlignedDatasetGrid
					.loadData(result.isCannedAnalysis,
							result.queryGenes.length,
							result.probeAlignedRegionDatasets);
			probeAlignedGrid.loadData(result.isCannedAnalysis,
					result.queryGenes.length, result.probeAlignedRegionResults,
					result.probeAlignedRegionDatasets);
		}
	});

});
