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
			
	// panel to hold all results of searches 
	this.resultsPanel = new Ext.TabPanel({
			id:'analysis-results-search-form-results-panel',
			height: 610,
			defaults: {
				autoScroll: true,
				width: 850
			},
			//deferredRender: true,
			hidden:true
			
		});
		
	// override actions triggered by nav keys for combo boxes (ie tab should not bring the user to the next box)

		
	// uncomment this to have results grid resize with window, (panel must have layout: 'fit')
	//Ext.EventManager.onWindowResize(resultsPanel.doLayout, resultsPanel); 

	// get ready to show results
	searchPanel.on("beforesearch",function(panel){
		
		// before every search, clear the results in preparation for new (possibly blank) results		
		
		var flashPanel = resultsPanel.getItem('cytoscaperesults');
		if (flashPanel){
			flashPanel.stopRender=true;
		}
		this.resultsPanel.removeAll(); 
		this.resultsPanel.hide();
		
		
		panel.clearError();
		
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
		
		var tut = Ext.getCmp('tutorial-cntlPanel-diff-ex');
		if (tut) {
			tut.hideTutorial();
			this.diffExTutorialAlreadyShown = true;
		}
		tut = Ext.getCmp('tutorial-cntlPanel-coex');
		if (tut) {
			tut.hideTutorial();
			this.coexTutorialAlreadyShown = true;
		}
		//Ext.DomHelper.overwrite('tutorial-control-div',{html:''});
		
		
		
		//clear browser warning
		if (redirectToClassic) {
			document.getElementById('browserWarning').style.display = "none";
		}
		
	},this);
		
	searchPanel.on("showCoexResults",function(panel,result, showCoexTutorial){
		resultsPanel.showCoexTutorial = showCoexTutorial; 
		/*
		 * Report any errors.
		 */
		if (result.errorState) {
			
			var errorPanel = new Ext.Panel({
				html: "<br> "+result.errorState,
				border: false,
				title : "No Coexpression Search Results"
			});
			
			resultsPanel.render('analysis-results-search-form-results');
			
			resultsPanel.add(errorPanel);
			//Ext.DomHelper.overwrite('analysis-results-search-form-messages', result.errorState);
			//if (knownGeneGrid) {knownGeneGrid.getStore().removeAll();}
			resultsPanel.show();
			resultsPanel.doLayout();
			errorPanel.show();
			
			return;
		}

		if (!knownGeneGrid) {
		
			//sometimes when the results get trimmed to the display stringency, the trimmed results are empty, in this case just use retrieved results and reset initial display stringency
						
			//add in the 'my genes only' results
			 var displayedResults = Gemma.CoexValueObjectUtil.combineKnownGeneResultsAndQueryGeneOnlyResults(result.knownGeneResults, result.queryGenesOnlyResults);
			
			if (searchPanel.getLastCoexpressionSearchCommand().displayStringency > searchPanel.getLastCoexpressionSearchCommand().stringency) {
			
				var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(result.knownGeneResults, searchPanel.getLastCoexpressionSearchCommand().displayStringency);
				
				if (trimmed.length != 0) {
					displayedResults = trimmed;
					initialDisplayStringency = searchPanel.getLastCoexpressionSearchCommand().displayStringency;
					
				} else {
					//there are no results after trimming at initial Display stringency so reset coexCommand.displayStringency
					searchPanel.getLastCoexpressionSearchCommand().displayStringency = searchPanel.getLastCoexpressionSearchCommand().stringency;
					
				}
			}			
			
			
			var coexpressionSearchData = new Gemma.CoexpressionSearchData({
				
				coexGridCoexCommand: searchPanel.getLastCoexpressionSearchCommand(),
				cytoscapeCoexCommand: Gemma.CytoscapePanelUtil.getCoexVizCommandFromCoexGridCommand(searchPanel.getLastCoexpressionSearchCommand()),
				coexGridResults: result
				
			});
						
			var knownGeneGrid = new Gemma.CoexpressionGrid({
				width: 900,
				height: 400,
				title: "Table",
				ref: 'coexGridResults',
				id: 'coexGridResults',
				colspan: 2,
				user: user,
				tabPanelViewFlag: true,
				layoutOnTabChange: true,
				hideMode: 'offsets',
				
				coexpressionSearchData: coexpressionSearchData
			
			});
			
			
		}		
		
		
		var cytoscapePanel = new Gemma.CytoscapePanel({
					id : "cytoscaperesults",
					ref: 'coexCytoscapeResults',
					title : "Visualization",									
					width:850,
					taxonId: searchPanel.getTaxonId(),
					taxonName: searchPanel.getTaxonName(),
					hideMode:'visibility',
					coexpressionSearchData: coexpressionSearchData
					
				});
		
		if (showCoexTutorial) {
			setupCoexTutorial(resultsPanel, knownGeneGrid, cytoscapePanel);
		}					
		
		panel.collapsePreviews();
		
		resultsPanel.add(knownGeneGrid);
		resultsPanel.add(cytoscapePanel);	
	    
		cytoscapePanel.relayEvents(knownGeneGrid, ['stringencyUpdateFromCoexGrid','queryGenesOnlyUpdateFromCoexGrid']);
	    knownGeneGrid.relayEvents(cytoscapePanel, ['stringencyUpdateFromCoexpressionViz', 'dataUpdateFromCoexpressionViz', 'queryGenesOnlyUpdateFromCoexpressionViz']);
	    knownGeneGrid.relayEvents(coexpressionSearchData, ['searchForCoexGridDataComplete']);	    
	    searchPanel.relayEvents(cytoscapePanel, ['queryUpdateFromCoexpressionViz', 'beforesearch']);
	    
		// won't fire the render event if it's already rendered
		resultsPanel.render('analysis-results-search-form-results');
		
		resultsPanel.show();
		resultsPanel.doLayout();
		
		knownGeneGrid.loadData(result.isCannedAnalysis, 2, displayedResults,
				result.knownGeneDatasets);
		
		knownGeneGrid.show();

	});
	
	var setupCoexTutorial = function(resultsPanel, knownGeneGrid, cytoscapePanel){
		if (this.coexTutorialControlPanel) {
			// need to make a new one because we've created new target elements 
			this.coexTutorialControlPanel.destroy();
		}
		var tutorialControlPanel = new Gemma.Tutorial.ControlPanel({
				renderTo: 'tutorial-control-div',
				// need id to clear tutorial between searches
				id: 'tutorial-cntlPanel-coex',
			//stateId: 'coExVisualiserTutorial'
			});
			this.coexTutorialControlPanel = tutorialControlPanel;
			// if hidden is stateful, the panel will be created hidden if the tutorial has already been shown
			if (!tutorialControlPanel.hidden) {
					var tipDefs = [];
					tipDefs.push({
						element: knownGeneGrid.getTopToolbar().stringencyfield,
						title: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.stringencyTitle,
						text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.stringencyText
						
					});
					
					// NOTE want this tip to point to header of column, not sure how to do that yet... the way below doesn't work
					// NOTE added arbitrary tbspacers with refs to jury rig this to work
					tipDefs.push({
						element: knownGeneGrid.getTopToolbar().arbitraryTutorialTooltip2,
						title: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.supportColumnTitle,
						text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.supportColumnText
						
					});
					
					tipDefs.push({
						element: knownGeneGrid.getTopToolbar().arbitraryTutorialTooltip3,
						title: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.nodeDegreeColumnTitle,
						text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.nodeDegreeColumnText
						
					});
					
					tipDefs.push({
						element: knownGeneGrid.getTopToolbar().searchInGrid,						
						title: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.visualizeTabTitle,
						text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchNonWidget.CoexpressionTutorial.visualizeTabText,
						position: {
							moveDown: -25
						}											
						
						
					});
					
					
					tutorialControlPanel.addTips(tipDefs);
				
			}
		
		
		resultsPanel.on('afterlayout', function(){
			if (!resultsPanel.tutorialStarted && resultsPanel.showCoexTutorial) {
				this.coexTutorialControlPanel.show();
				this.coexTutorialControlPanel.playTips(0);
				
				
				resultsPanel.on('beforetabchange', function(tabPanel, newTab, currTab){
						
						this.coexTutorialControlPanel.hideTips(this.coexTutorialControlPanel.tips);
						this.coexTutorialControlPanel.hide();
						if (newTab && newTab.id == 'cytoscaperesults'){
							resultsPanel.showCoexTutorial = false;
						}
				}, this);
				
			}
		}, this);
		
		this.coexTutorialControlPanel.on('tutorialHidden', function(){
			this.coexTutorialControlPanel.hide();
			this.coexTutorialControlPanel.hidden= true;
		}, this);
		
	};
	
	
	searchPanel.on("showDiffExResults",function(panel,result, data){
		
		if (!redirectToClassic) {
			
			// show metaheatmap viewer (but not control panel)
			// control panel is responsible for creating the visualisation view space
			Ext.apply(data, {applyTo : 'meta-heatmap-div'});
			
			// override showing tutorial, for now only works with non-widget version
			/*if (this.diffExTutorialAlreadyShown) {
				Ext.apply(data,{showTutorial:false});
			}*/
			
			this.diffVisualizer = new Gemma.MetaHeatmapDataSelection(data);
			
			this.diffVisualizer.on('visualizationLoaded', function(){
				panel.loadMask.hide();
			}, this);
			
		}
		else {
			
			var flashPanel = resultsPanel.getItem('cytoscaperesults');
			if (flashPanel){
				flashPanel.stopRender=true;
			}

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