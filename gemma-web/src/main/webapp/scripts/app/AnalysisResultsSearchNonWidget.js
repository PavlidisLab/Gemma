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

	var user = (Ext.get('hasUser') !== null) ? Ext.get('hasUser').getValue(): null;
	// check if canvas is supported (not supported in IE < 9; need to use excanvas in IE8)
	if (!document.createElement("canvas").getContext) {
        Ext.DomHelper.append('analysis-results-search-form', {
            tag: 'p',
            cls: 'trouble',
            id: 'browserWarning',
            html: Gemma.HelpText.CommonWarnings.BrowserWarnings.generic
        });
    }

    var coexpressionSearchData = new Gemma.CoexpressionSearchData();

    // panel for performing search, appears on load
	var searchPanel = new Gemma.AnalysisResultsSearchForm({
		width: Gemma.SEARCH_FORM_WIDTH,
        observableSearchResults : coexpressionSearchData
	});

	// window that controls diff visualizer; 
	// it's not part of the results panel so need to keep track separately to be able to delete it
	this.diffVisualizer = null;

	searchPanel.render("analysis-results-search-form");
	searchPanel.checkUrlParams();
			
	// panel to hold all results of searches 
	var coexpressionSearchResultsPanel = new Ext.TabPanel({
			id:'analysis-results-search-form-results-panel',
			height: 610,
			defaults: {
				autoScroll: true,
				width: 850
			},
			hidden:true
		});

	// override actions triggered by nav keys for combo boxes (ie tab should not bring the user to the next box)

	// uncomment this to have results grid resize with window, (panel must have layout: 'fit')
	// Ext.EventManager.onWindowResize(resultsPanel.doLayout, resultsPanel);

    function hideBannerElements() {
        if (Ext.get('frontPageContent')) {
            Ext.get('frontPageContent').remove();
        }
        if (Ext.get('frontPageSlideShow')) {
            Ext.get('frontPageSlideShow').remove();
        }
        if (Ext.get('sloganText')) {
            Ext.get('sloganText').remove();
        }
    }

    function hideTutorials() {
        var tutorial = Ext.getCmp('tutorial-cntlPanel-diff-ex');
        if (tutorial) {
            tutorial.hideTutorial();
        }
        tutorial = Ext.getCmp('tutorial-cntlPanel-coex');
        if (tutorial) {
            tutorial.hideTutorial();
        }
    }

    // Remove previous diff visualization result
    function removeDiffExpressionVisualizer() {
        Ext.DomHelper.overwrite('meta-heatmap-div', {html: ''});
    }

    // get ready to show results
	searchPanel.on("beforesearch", function( panel ) {
		// Before every search, clear the results in preparation for new (possibly blank) results
		var flashPanel = coexpressionSearchResultsPanel.getItem('cytoscaperesults');
		if (flashPanel) {
			flashPanel.stopRender = true;
		}

        coexpressionSearchData.purgeListeners();
        coexpressionSearchData.on('aftersearch', function(result){
            searchPanel.loadMask.hide();
        });

        coexpressionSearchData.on('search-started', function(result){
            searchPanel.loadMask.show();
        });

        coexpressionSearchResultsPanel.setActiveTab(null);
        coexpressionSearchResultsPanel.removeAll();
        coexpressionSearchResultsPanel.hide();

        if (panel) {
            panel.clearError();
        }

        hideBannerElements();
        hideTutorials();

        removeDiffExpressionVisualizer();
    });

    searchPanel.on("coexpression_search_query_ready", function( panel, searchCommand, showTutorial ) {
        coexpressionSearchResultsPanel.showCoexTutorial = showTutorial;

        /**
         * We pick appropriate initial display stringency based on number of experiments
         * (low stringency results aren't meaningful in large experiment sets).
         *
         * @private
         * @param numDatasets
         */
        function decideInitialDisplayStringency ( numDatasets ) {
            var k = 50;
            var initialDisplayStringency = 2;

            if (numDatasets > k) {
                initialDisplayStringency = 2 + Math.round(numDatasets / k);
            }

            if (initialDisplayStringency > 20) {
                initialDisplayStringency = 20;
            }

            return initialDisplayStringency;
        }

        var coexDisplaySettings = new Gemma.CoexpressionDisplaySettings();

        var displayStringency = decideInitialDisplayStringency( searchCommand.eeIds.length );
        coexDisplaySettings.setStringency( displayStringency );

        coexpressionSearchData.setSearchCommand(searchCommand);

        var coexpressionGrid = new Gemma.CoexpressionGrid({
            width: 900,
            height: 400,
            title: "Table",
            ref: 'coexGridResults',
            id: 'coexGridResults',
            colspan: 2,
            user: user,
            layoutOnTabChange: true,
            hideMode: 'offsets',
            coexpressionSearchData: coexpressionSearchData,
            observableDisplaySettings : coexDisplaySettings
        });

        var cytoscapePanel = new Gemma.CytoscapeJSPanel({
            id: "cytoscaperesults",
            ref: 'coexCytoscapeResults',
            title: "Visualization",
            width: 850,
            taxonId: searchPanel.getTaxonId(),
            taxonName: searchPanel.getTaxonName(),
            hideMode: 'visibility',
            searchPanel: searchPanel,
            coexpressionSearchData: coexpressionSearchData,
            coexDisplaySettings : coexDisplaySettings
        });

        coexpressionSearchResultsPanel.add(coexpressionGrid);
        coexpressionSearchResultsPanel.add(cytoscapePanel);

        coexpressionSearchData.search( searchCommand );

        coexpressionSearchData.on("search-results-ready", function () {

            if (showTutorial) {
                setupCoexTutorial(coexpressionSearchResultsPanel, coexpressionGrid);
            }

            panel.collapsePreviews();

            // won't fire the render event if it's already rendered
            coexpressionSearchResultsPanel.render('analysis-results-search-form-results');

            coexpressionSearchResultsPanel.show();
            coexpressionSearchResultsPanel.doLayout();

            coexpressionSearchResultsPanel.setActiveTab(0);
            coexpressionGrid.show();
        });
    });
	
	var setupCoexTutorial = function(coexpressionSearchResultsPanel, knownGeneGrid){
		if (this.coexTutorialControlPanel) {
			// need to make a new one because we've created new target elements 
			this.coexTutorialControlPanel.destroy();
		}
		var tutorialControlPanel = new Gemma.Tutorial.ControlPanel({
				renderTo: 'tutorial-control-div',
				// need id to clear tutorial between searches
				id: 'tutorial-cntlPanel-coex'
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

        coexpressionSearchResultsPanel.on('afterlayout', function(){
			if (!coexpressionSearchResultsPanel.tutorialStarted && coexpressionSearchResultsPanel.showCoexTutorial) {
				this.coexTutorialControlPanel.show();
				this.coexTutorialControlPanel.playTips(0);


                coexpressionSearchResultsPanel.on('beforetabchange', function(tabPanel, newTab, currTab){
						this.coexTutorialControlPanel.hideTips(this.coexTutorialControlPanel.tips);
						this.coexTutorialControlPanel.hide();
						if (newTab && newTab.id === 'cytoscaperesults'){
                            coexpressionSearchResultsPanel.showCoexTutorial = false;
						}
				}, this);
				
			}
		}, this);
		
		this.coexTutorialControlPanel.on('tutorialHidden', function() {
			this.coexTutorialControlPanel.hide();
			this.coexTutorialControlPanel.hidden= true;
		}, this);
		
	};

	searchPanel.on('differential_expression_search_query_ready', function (panel,result, data) {
			// show metaheatmap viewer (but not control panel)
			// control panel is responsible for creating the visualisation view space
			Ext.apply(data, {applyTo : 'meta-heatmap-div'});

			this.diffVisualizer = new Gemma.MetaHeatmapDataSelection( data );

			this.diffVisualizer.on('visualizationLoaded', function() {
				panel.loadMask.hide();
			}, this);
	}, this);
});


