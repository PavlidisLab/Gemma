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
				html: Gemma.HelpText.CommonWarnings.BrowserWarnings.ie8
			});
		}else if(Ext.isIE){
			Ext.DomHelper.append('analysis-results-search-form', {
				tag: 'p',
				cls: 'trouble',
				id: 'browserWarning',
				html: Gemma.HelpText.CommonWarnings.BrowserWarnings.ieNot8
			});
		}else{
			Ext.DomHelper.append('analysis-results-search-form', {
				tag: 'p',
				cls: 'trouble',
				id: 'browserWarning',
				html: Gemma.HelpText.CommonWarnings.BrowserWarnings.generic
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
	this.resultsPanel = new Ext.TabPanel({
		id:'analysis-results-search-form-results-panel',
		renderTo : 'analysis-results-search-form-results',
		height: 610,
		defaults: {
			autoScroll: true,
			width: 850
		},
		deferredRender: true,
		hidden:true
				
		//layout:'fit' //only works with one component per container
		//border:false,
		//autoHeight:true,
		
		//bodyStyle:'text-align:left;',
		//style:'text-align:left'
		
	});
		
	// uncomment this to have results grid resize with window, (panel must have layout: 'fit')
	//Ext.EventManager.onWindowResize(this.resultsPanel.doLayout, this.resultsPanel); 

	// get ready to show results
	searchPanel.on("beforesearch",function(panel){
		
		// before every search, clear the results in preparation for new (possibly blank) results 
		
		//this.resultsPanel.removeAll() causes CytoscapePanel's afterrender event fire for some reason.  
		//Set coexgridref to null so that we can prevent the afterrender listener function from executing in CytoscapePanel.js
		if (Ext.getCmp("cytoscaperesults")){
			Ext.getCmp("cytoscaperesults").coexGridRef=null;
		}
		this.resultsPanel.removeAll(); 
		this.resultsPanel.hide();
		panel.clearError();
		
		// once the user performs a search, hide the main page elements 
		// hide and remove main page elements with an animation (if they haven't already been removed)

		//if(left) left.animate({height:{to:0},opacity:{to:0}},2,function(){left.remove()});
		//if(right) right.animate({height:{to:0},opacity:{to:0}},2,function(){right.remove()});
		
		if (Ext.get('frontPageContent')) {
			Ext.get('frontPageContent').remove();
		}
				
		if (Ext.get('frontPageSlideShow')) {
			Ext.get('frontPageSlideShow').remove();
		}
						
		if (Ext.get('sloganText')) {
			Ext.get('sloganText').remove();
		}
		
		// remove previous diff visualization result
		Ext.DomHelper.overwrite('meta-heatmap-div',{html:''});
		Ext.DomHelper.overwrite('tutorial-control-div',{html:''});
		
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
			
			var errorPanel = new Ext.Panel({
				html: "<br> "+result.errorState,
				border: false,
				title : "No Coexpression Search Results"
			});
			
			
			resultsPanel.add(errorPanel);
			//Ext.DomHelper.overwrite('analysis-results-search-form-messages', result.errorState);
			//if (knownGeneGrid) {knownGeneGrid.getStore().removeAll();}
			resultsPanel.show();
			resultsPanel.doLayout();
			errorPanel.show();
			
			return;
		}
		/*
		var summaryPanel = new Ext.Panel({
				width : 900,
				id : 'summarypanel',
				height : 300,
				collapsed : true
			});
*/
		/*
		var summaryGrid = new Gemma.CoexpressionSummaryGrid({
					genes : result.queryGenes,
					//renderTo : "summarypanel",
					summary : result.summary,
					//collapsed:true,
					//collapsible:true,
					width:900
				});
		*/
		
		/*
		var knownGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
				width : 900,
				colspan : 2,
				collapsible : false,
				collapsed : false
			});
	*/
		if(!knownGeneGrid){
			var knownGeneGrid = new Gemma.CoexpressionGrid({
				width : 900,
				height : 400,
				title : "Coexpression Results",
				colspan : 2,
				user : user,
				tabPanelViewFlag : true,
				layoutOnTabChange:true,
				hideMode:'offsets',
				currentResultsStringency: searchPanel.getLastCoexpressionSearchCommand().stringency,
				initialDisplayStringency: searchPanel.getLastCoexpressionSearchCommand().displayStringency
				//hidden:true
			});
		}
		
		var cytoscapePanel = new Gemma.CytoscapePanel({
					id : "cytoscaperesults",
					title : "Visualization",
					queryGenes : result.queryGenes,
					knownGeneResults : result.knownGeneResults,
					coexCommand: searchPanel.getLastCoexpressionSearchCommand(),
					coexGridRef: knownGeneGrid,
					searchPanelRef: searchPanel,
					width:850,
					taxonId: searchPanel.getTaxonId(),
					taxonName: searchPanel.getTaxonName(),
					hideMode:'visibility'
					
				});
		
		
		
	//var items = [summaryPanel, knownGeneDatasetGrid, knownGeneGrid];
		
			if (admin) {

		/*
		 * Note: doing allPanel.add doesn't work. Probably something to do with the table layout; indeed:
		 * https://extjs.com/forum/showthread.php?p=173090
		 */
/*
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

		//items.push(predictedGeneDatasetGrid);
		//items.push(predictedGeneGrid);
		//items.push(probeAlignedDatasetGrid);
		//items.push(probeAlignedGrid);
*/
		}
		
		panel.collapsePreviews();
		/*resultsPanel.add(
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
		});*/
		//resultsPanel.add(summaryGrid);
		
		resultsPanel.add(knownGeneGrid);
		resultsPanel.add(cytoscapePanel);
		//resultsPanel.add(knownGeneDatasetGrid);		
		resultsPanel.show();
		resultsPanel.doLayout();
		//cytoscapePanel.show();

		//reset coex summary panel
		//Ext.DomHelper.overwrite('summarypanel', "");
		
/*
		var eeMap = {};
		if (result.datasets) {
			for (var i = 0; i < result.datasets.length; ++i) {
				var ee = result.datasets[i];
				eeMap[ee.id] = ee;
			}
		}
*/
		
		/*summaryPanel = new Gemma.CoexpressionSummaryGrid({
					genes : result.queryGenes,
					renderTo : "summarypanel",
					summary : result.summary
				});*/

		// create expression experiment image map
		/*var imageMap = Ext.get("eeMap");
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
*/
				
		

		/*
		Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.knownGeneDatasets, eeMap);
		knownGeneDatasetGrid.loadData(result.knownGeneDatasets);
		*/
		knownGeneGrid.cytoscapeRef=cytoscapePanel;
		
		if (knownGeneGrid.initialDisplayStringency > knownGeneGrid.currentResultsStringency){
        	var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(result.knownGeneResults, Gemma.CoexValueObjectUtil.getCurrentQueryGeneIds(result.queryGenes), knownGeneGrid.initialDisplayStringency);
        	
        	knownGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, trimmed.trimmedKnownGeneResults,
    				result.knownGeneDatasets, result.knownGeneResults, Gemma.CoexValueObjectUtil.getCurrentQueryGeneIds(result.queryGenes));
        	
        }
		else{
		
		knownGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
				result.knownGeneDatasets, result.knownGeneResults, Gemma.CoexValueObjectUtil.getCurrentQueryGeneIds(result.queryGenes));
		}	
		knownGeneGrid.show();
				
		if (admin) {
			/*
			Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.predictedGeneDatasets, eeMap);
			predictedGeneDatasetGrid.loadData(result.predictedGeneDatasets);
			predictedGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.predictedGeneResults,
					result.predictedGeneDatasets);

			Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.probeAlignedRegionDatasets, eeMap);
			probeAlignedDatasetGrid.loadData(result.probeAlignedRegionDatasets);
			probeAlignedGrid.loadData(result.isCannedAnalysis, result.queryGenes.length,
					result.probeAlignedRegionResults, result.probeAlignedRegionDatasets);
			*/
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
			Ext.apply(data, {applyTo : 'meta-heatmap-div'});
			this.diffVisualizer = new Gemma.MetaHeatmapDataSelection(data);
			
			this.diffVisualizer.on('visualizationLoaded', function(){
				panel.loadMask.hide();
			}, this);
			
		}
		else {
			
			this.resultsPanel.removeAll(); 
			this.resultsPanel.hide(); 
			var diffExResultsGrid = new Gemma.DiffExpressionGrid({
				renderTo: 'meta-heatmap-div',
				title: "Differentially expressed genes",
				searchPanel: searchPanel,
				viewConfig: {
					forceFit: true
				},
				width:'auto',
				style:'width:100%'
				
			});
			
			diffExResultsGrid.loadData(result);
			
			var link = panel.getDiffExBookmarkableLink();
			//diffExResultsGrid.setTitle(String.format("Differentially expressed genes <a href='{0}'>(bookmarkable link)</a> <a target='_blank' href='{0}&export'>(export as text)</a>", link));
			diffExResultsGrid.setTitle(String.format("Differentially expressed genes <a target='_blank' href='{0}&export'>(export as text)</a>", link));

		}
	}, this);
});