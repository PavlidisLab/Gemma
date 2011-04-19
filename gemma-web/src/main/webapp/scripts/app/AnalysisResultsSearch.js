/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
	var admin = (Ext.get('hasAdmin')!==null)? Ext.get('hasAdmin').getValue(): null;
	var user = (Ext.get('hasUser')!==null)? Ext.get('hasUser').getValue(): null;
	
	if (Ext.isIE && Ext.isIE6) {
		Ext.DomHelper.append('analysis-results-search-form', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 7 or newer.'
		});
	}
	
	// panel for performing search, appears on load
	var searchPanel = new Gemma.AnalysisResultsSearchForm();
	
	// window that controls diff visualizer; 
	// it's not part of the results panel so need to keep track separately to be able to delete it
	this.diffVisualizer = null;

	searchPanel.render("analysis-results-search-form");
	
	// override actions triggered by nav keys for combo boxes (ie tab should not bring the user to the next box)

	// panel to hold all results of searches 
	this.resultsPanel = new Ext.Panel({
		id:'analysis-results-search-form-results-panel',
		renderTo : 'analysis-results-search-form-results',
		//layout:'fit', //only works with one component per container
		border:false,
		autoHeight:true,
		hidden:true,
		bodyStyle:'text-align:left;',
		style:'text-align:left'
		//hideMode:'visibility'
	});
		
	// uncomment this to have results grid resize with window, (panel must have layout: 'fit')
	//Ext.EventManager.onWindowResize(this.resultsPanel.doLayout, this.resultsPanel); 

	// get ready to show results
	searchPanel.on("beforesearch",function(panel){
		
		// before every search, clear the results in preparation for new (possibly blank) results 
		this.resultsPanel.removeAll(); 
		panel.clearError();
		
		// once the user performs a search, hide the main page elements 
		// hide and remove main page elements with an animation (if they haven't already been removed)
		var toHide = Ext.get('frontPageContent');
		//if(left) left.animate({height:{to:0},opacity:{to:0}},2,function(){left.remove()});
		//if(right) right.animate({height:{to:0},opacity:{to:0}},2,function(){right.remove()});
		
		if (toHide) {
			toHide.remove();
		}
		
		
		// remove previous diff visualization result
		Ext.DomHelper.overwrite('meta-heatmap-div',{html:''});
		
	},this);
	
	searchPanel.on("showCoexResults",function(panel,result){
		
		/*
		 * Report any errors.
		 */
		if (result.errorState) {
			resultsPanel.add({html:"<h2>Coexpression Search Results</h2><br>"+result.errorState+"<br><br><br>", border:false});
			//Ext.DomHelper.overwrite('analysis-results-search-form-messages', result.errorState);
			if (knownGeneGrid) {knownGeneGrid.getStore().removeAll();}
			resultsPanel.doLayout();
			resultsPanel.show();
			return;
		}
		
		var summaryPanel = new Ext.Panel({
				width : 900,
				id : 'summarypanel',
				height : 300,
				collapsed : true
			});

		var knownGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
				width : 900,
				colspan : 2,
				collapsed : true
			});
		if(!knownGeneGrid){
			var knownGeneGrid = new Gemma.CoexpressionGrid({
				width : 900,
				height : 400,
				title : "Coexpressed genes",
				colspan : 2,
				user : user
				//hidden:true
			});
		}
		
		
		
	var items = [summaryPanel, knownGeneDatasetGrid, knownGeneGrid];
		
			if (admin) {

		/*
		 * Note: doing allPanel.add doesn't work. Probably something to do with the table layout; indeed:
		 * https://extjs.com/forum/showthread.php?p=173090
		 */

		predictedGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
					width : 900,
					colspan : 2,
					adjective : "predicted gene",
					collapsed : true
				});

		predictedGeneGrid = new Gemma.CoexpressionGrid({
					width : 900,
					title : "Coexpressed predicted genes",
					id : 'pred-gene-grid',
					colspan : 2,
					height : 400,
					collapsed : true
				});

		probeAlignedDatasetGrid = new Gemma.CoexpressionDatasetGrid({
					width : 900,
					colspan : 2,
					id : 'par-dataset-grid',
					adjective : "probe-aligned region",
					collapsed : true
				});

		probeAlignedGrid = new Gemma.CoexpressionGrid({
					width : 900,
					colspan : 2,
					height : 400,
					title : "Coexpressed probe-aligned regions",
					collapsed : true
				});

		items.push(predictedGeneDatasetGrid);
		items.push(predictedGeneGrid);
		items.push(probeAlignedDatasetGrid);
		items.push(probeAlignedGrid);

		}
		
		panel.collapsePreviews();
		resultsPanel.add({html:"<h2>Coexpression Search Results</h2>", border:false});
		resultsPanel.add(items);
		resultsPanel.show();
		resultsPanel.doLayout();

		//reset coex summary panel
		//Ext.DomHelper.overwrite('summarypanel', "");
		

		var eeMap = {};
		if (result.datasets) {
			for (var i = 0; i < result.datasets.length; ++i) {
				var ee = result.datasets[i];
				eeMap[ee.id] = ee;
			}
		}

		
		/*summaryPanel = new Gemma.CoexpressionSummaryGrid({
					genes : result.queryGenes,
					renderTo : "summarypanel",
					summary : result.summary
				});*/

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

		var link = panel.getCoexBookmarkableLink();
		knownGeneGrid
				.setTitle(String
						.format(
								"Coexpressed genes &nbsp;&nbsp;&nbsp;<a href='{0}' title='bookmarkable link'><img src=\"/Gemma/images/icons/link.png\" alt='bookmark'/></a>&nbsp; <a target='_blank' href='{0}&export' title='download'><img src=\"/Gemma/images/download.gif\" alt='download'/></a>",
								link));

				
		resultsPanel.doLayout();
		//resultsPanel.show();

				
		var summaryGrid = new Gemma.CoexpressionSummaryGrid({
					genes : result.queryGenes,
					renderTo : "summarypanel",
					summary : result.summary,
					collapsed:true,
					collapsible:true,
					width:900
				});

		
		Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.knownGeneDatasets, eeMap);
		knownGeneDatasetGrid.loadData(result.knownGeneDatasets);
		knownGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
				result.knownGeneDatasets);
				
		//knownGeneGrid.show();
				
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
	
	searchPanel.on("showDiffExResults",function(panel,result, data){
		
		
		// show metaheatmap viewer (but not control panel)
		// control panel is responsible for creating the visualisation view space
		this.diffVisualizer = new Gemma.MetaHeatmapControlWindow(data); 
		panel.collapsePreviews();
		
		/*		
		var diffExResultsGrid = new Gemma.DiffExpressionGrid({
				//renderTo : "analysis-results-search-form-results",
				title : "Differentially expressed genes",
				searchPanel : searchPanel,
				viewConfig: {forceFit: true},
				height:200,
				width:900
			});
		panel.collapsePreviews();
		resultsPanel.add({html:"<h2>Differential Expression Search Results</h2>", border:false});
		resultsPanel.add(diffExResultsGrid);
		
		var link = panel.getDiffExBookmarkableLink();
		diffExResultsGrid
				.setTitle(String
						.format(
								"Differentially expressed genes <a href='{0}'>(bookmarkable link)</a> <a target='_blank' href='{0}&export'>(export as text)</a>",
								link));

		var resultsP = Ext.get('analysis-results-search-form-results-panel');

		//if(resultsP) resultsP.animate({height:{from:0},opacity:{to:1}},2);
		//resultsPanel.show({height:{from:0},opacity:{from:0}});
		resultsPanel.show();
		resultsPanel.doLayout();
		diffExResultsGrid.loadData(result);
		//this.diffVisualizer.show();
		*/
	});
	

});