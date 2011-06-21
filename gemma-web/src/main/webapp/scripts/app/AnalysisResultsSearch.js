/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');
Gemma.SEARCH_FORM_WIDTH = 900;
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {
	
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
	var admin = (Ext.get('hasAdmin')!==null)? Ext.get('hasAdmin').getValue(): null;
	var user = (Ext.get('hasUser')!==null)? Ext.get('hasUser').getValue(): null;
	var redirectToClassic = false;
	// check if canvas is supported (not supported in IE < 9; need to use excanvas in IE8)
	if (!document.createElement("canvas").getContext) {
		redirectToClassic = true;
		//not supported
		if(Ext.isIE8){
			// excanvas doesn't cover all functionality of new diff ex metaheatmap visualization
			Ext.DomHelper.append('analysis-results-search-form', {
				tag: 'p',
				cls: 'trouble',
				id: 'browserWarning',
				html: 'Advanced differential expression visualizations are not available in your browser (Internet Explorer 8). We suggest upgrading to  '+
						'<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>, '+
						'<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or '+
						'<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.'
			});
		}else if(Ext.isIE){
			Ext.DomHelper.append('analysis-results-search-form', {
				tag: 'p',
				cls: 'trouble',
				id: 'browserWarning',
				html: 'This page may display improperly in older versions of Internet Explorer(IE). Please upgrade to '+
						'<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">IE 9</a>, '+
						'<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or '+
						'<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.'+
				' If you are running IE 9 and you see this message, please make sure you are not in compatibility mode. '
			});
		}else{
			Ext.DomHelper.append('analysis-results-search-form', {
				tag: 'p',
				cls: 'trouble',
				id: 'browserWarning',
				html: 'This page may not display properly in all browsers. (The \"canvas\" element is requried.)'+
						' Please switch to '+
						'<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a>,'+
						'<a href="http://www.google.com/chrome/" target="_blank">Chrome</a> or'+
						'<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>.'
			});
		}
	} 
	
	// panel for performing search, appears on load
	var searchPanel = new Gemma.AnalysisResultsSearchForm({
		width: Gemma.SEARCH_FORM_WIDTH,
		showClassicDiffExResults: redirectToClassic
	});

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
		
		//clear browser warning
		if (redirectToClassic) {
			document.getElementById('browserWarning').style.display = "none";
		}
		
	},this);
		
	searchPanel.on("showCoexResults",function(panel,result){
		
		var coexOptions = new Gemma.CoexpressionSearchOptions();
		coexOptions.on('rerunSearch',function(stringency, forceProbe, queryGenesOnly){
				coexOptions.hide();
				resultsPanel.removeAll();
				searchPanel.redoRecentCoexpressionSearch(stringency, forceProbe, queryGenesOnly);
			},this);
					
		/*
		 * Report any errors.
		 */
		if (result.errorState) {
			resultsPanel.add({
					html: "<h2>Coexpression Search Results</h2><br><br>"+result.errorState + "<br><br><br>",
					border: false
					/*items: { //can't broaden search results, so no use giving refinement options
						xtype: 'button',
						text: 'refine',
						scope: this,
						handler: function(){
							coexOptions.show();
						}
					}*/
			});
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
		resultsPanel.add(
		{
			layout: 'hbox',
			items: [{
				html: "<h2>Coexpression Search Results</h2><br>",
				border:false
			}, {
				xtype: 'button',
				text: 'refine',
				style: 'padding-top: 5px; padding-left: 10px',
				scope: this,
				handler: function(){
					coexOptions.show();
				}
			}],
			border: false
		});
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
	
	searchPanel.on("showOptions",function(stringency, forceProbeLevelSearch, queryGenesOnly){
		if(this.admin){
			resultsPanel.insert(1, {border:false,html:'<h4>Refinements Used:</h4>Stringency = '+stringency+
			'<br>Probe-level search: '+forceProbeLevelSearch+
			"<br>Results only among query genes: "+queryGenesOnly+'<br>'});
		}else{
			resultsPanel.insert(1, {border:false,html:'<h4>Refinements Used:</h4>Stringency = '+stringency+
			"<br>Results only among query genes: "+queryGenesOnly+'<br>'});
		}
		
		resultsPanel.doLayout();
	});
	
	searchPanel.on("showDiffExResults",function(panel,result, data){
		
		if (!redirectToClassic) {
			
			// show metaheatmap viewer (but not control panel)
			// control panel is responsible for creating the visualisation view space
			this.diffVisualizer = new Gemma.MetaHeatmapDataSelection(data);
			
			this.diffVisualizer.on('visualizationLoaded', function(){
				panel.loadMask.hide();
			}, this);
		}
		else {
			
			var diffExResultsGrid = new Gemma.DiffExpressionGrid({
				//renderTo : "analysis-results-search-form-results",
				title: "Differentially expressed genes",
				searchPanel: searchPanel,
				viewConfig: {
					forceFit: true
				},
				height: 200,
				width: 900
			});
			resultsPanel.removeAll();
			panel.collapsePreviews();
			resultsPanel.add({
				html: "<h2>Differential Expression Search Results</h2>",
				border: false
			});
			resultsPanel.add(diffExResultsGrid);
			
			var link = panel.getDiffExBookmarkableLink();
			diffExResultsGrid.setTitle(String.format("Differentially expressed genes <a href='{0}'>(bookmarkable link)</a> <a target='_blank' href='{0}&export'>(export as text)</a>", link));
			
			var resultsP = Ext.get('analysis-results-search-form-results-panel');
			
			//if(resultsP) resultsP.animate({height:{from:0},opacity:{to:1}},2);
			//resultsPanel.show({height:{from:0},opacity:{from:0}});
			resultsPanel.show();
			resultsPanel.doLayout();
			diffExResultsGrid.loadData(result);
		//this.diffVisualizer.show();
		}
	});
});