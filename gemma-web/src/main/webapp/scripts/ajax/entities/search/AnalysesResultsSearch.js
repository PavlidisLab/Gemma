
Ext.namespace('Gemma');
Gemma.AnalysisResultsSearch = Ext.extend(Ext.Panel, {
	SEARCH_FORM_WIDTH : 900,
	autoScroll:true,
	initComponent: function(){
		Gemma.AnalysisResultsSearch.superclass.initComponent.call(this);
		
		this.admin = (Ext.get('hasAdmin') !== null) ? Ext.get('hasAdmin').getValue() : null;
		this.user = (Ext.get('hasUser') !== null) ? Ext.get('hasUser').getValue() : null;
				
		var errorPanel = new Ext.Panel({
			tpl:'<img src="/Gemma/images/icons/warning.png">{msg}',
			border:false,
			hidden:true
		});
		this.add(errorPanel);
		
		
		// check if canvas is supported (not supported in IE < 9; need to use excanvas in IE8)
		var redirectToClassic = false;
		var browserWarning = "";
		if ( !document.createElement("canvas").getContext ) {
			redirectToClassic = true;
			//not supported
			// excanvas doesn't cover all functionality of new diff ex metaheatmap visualization
			if (Ext.isIE8) {
				
				browserWarning = Gemma.HelpText.CommonWarnings.BrowserWarnings.ie8;
				
			}else if (Ext.isIE) {
				
				browserWarning = Gemma.HelpText.CommonWarnings.BrowserWarnings.ieNot8;
				
			}else {
				
				browserWarning = Gemma.HelpText.CommonWarnings.BrowserWarnings.generic;
				
			}
		}

		// panel for performing search, appears on load
		var searchForm = new Gemma.AnalysisResultsSearchForm({
			width: this.SEARCH_FORM_WIDTH,
			showClassicDiffExResults: redirectToClassic,
			bodyStyle: 'text-align:left;',
			style: 'text-align:left'
		});
		
		var diffExResultsDiv = new Ext.Panel();
		// panel to hold all results of searches 
		var coexResultsTabPanel = 	new Ext.TabPanel({
			border: true,
			//hidden: true,
			//deferredRender: true,
			bodyStyle: 'text-align:left;',
			style: 'text-align:left',
			hidden:true
		});

		this.searchPanel = new Ext.Panel({
			items: [searchForm],
			title:'Search Form',
			collapsible: true,
			titleCollapse: true,
			border:false,
			bodyStyle: 'margin-left:auto;margin-right:auto;width:'+this.SEARCH_FORM_WIDTH+'px;'
		});
		this.add(this.searchPanel);
		this.add(new Ext.Panel({items:[coexResultsTabPanel], title:'Results', layout:'fit'}));
		this.add(diffExResultsDiv);
		/*this.add({
			tag: 'div',
			id: 'meta-heatmap-div',
			border: false,
			html: 'Use the form above to search for coexpression or differential expression'
		});*/
		this.doLayout();
		
		
		// window that controls diff visualizer; 
		// it's not part of the results panel so need to keep track separately to be able to delete it
		this.diffVisualizer = null;
		
		
		if (browserWarning !== "") {
			errorPanel.on('render', function(){
				this.update('<img src="/Gemma/images/icons/warning.png">'+browserWarning);
				this.show();
			});
		}
		
		// get ready to show results
		searchForm.on("beforesearch", function(panel){
		
			//this.resultsPanel.removeAll() causes CytoscapePanel's afterrender event fire for some reason.  
			//Set coexgridref to null so that we can prevent the afterrender listener function from executing in CytoscapePanel.js
			if (Ext.getCmp("cytoscaperesults")){
				Ext.getCmp("cytoscaperesults").coexGridRef=null;
			}
			
			// before every search, clear the results in preparation for new (possibly blank) results 
			coexResultsTabPanel.removeAll();
			panel.clearError();
			// clear errors
			errorPanel.update();
			errorPanel.hide();
			coexResultsTabPanel.doLayout();
			coexResultsTabPanel.hide();
			
			// for clearing diff ex
			this.remove(diffExResultsDiv.getId());
			diffExResultsDiv = new Ext.Panel();
			this.add(diffExResultsDiv);
			this.doLayout();
			
		}, this);
		
		
		searchForm.on("handleError", function(msg){
			errorPanel.update({msg: msg});
			errorPanel.show();
		});
		searchForm.on("showCoexResults", function(formPanel, result){
			// in this case, searchForm == formPanel 
			this.showCoExResults(searchForm, result, coexResultsTabPanel,this.searchPanel);
		}, this);
		
		
		searchForm.on("showOptions", function(stringency, forceProbeLevelSearch, queryGenesOnly){
			if (this.admin) {
				coexResultsTabPanel.insert(1, {
					border: false,
					html: '<h4>Refinements Used:</h4>Stringency = ' + stringency +
					'<br>Probe-level search: ' +
					forceProbeLevelSearch +
					"<br>Results only among query genes: " +
					queryGenesOnly +
					'<br>'
				});
			}
			else {
				coexResultsTabPanel.insert(1, {
					border: false,
					html: '<h4>Refinements Used:</h4>Stringency = ' + stringency +
					"<br>Results only among query genes: " +
					queryGenesOnly +
					'<br>'
				});
			}
			
			coexResultsTabPanel.doLayout();
		},this);

		
		searchForm.on("showDiffExResults", function(formPanel, result, data){
			if (!redirectToClassic) {
				
				//Ext.apply(data,{applyTo:'meta-heatmap-div'});
				// show metaheatmap viewer (but not control panel)
				// control panel is responsible for creating the visualisation view space
				
			//coexResultsTabPanel.add(diffExResultsDiv);
			//coexResultsTabPanel.doLayout();
				Ext.apply(data,{applyTo:diffExResultsDiv.getId()});
				
				// override showing tutorial, for now only works with non-widget version
				Ext.apply(data,{showTutorial:false});
				
				this.diffVisualizer = new Gemma.MetaHeatmapDataSelection(data);
				this.diffVisualizer.on('visualizationLoaded', function(){
					this.searchPanel.collapse();
					formPanel.loadMask.hide();
				}, this);
				/*this.diffVisualizer.on('visualizationReady', function(viz){
				 * 			formPanel.loadMask.hide();
		
					coexResultsTabPanel.add(viz);
					coexResultsTabPanel.doLayout();
					viz.refreshVisualization();
					formPanel.loadMask.hide();
				}, this);*/

			}
			else {
			
			var diffExResultsGrid = new Gemma.DiffExpressionGrid({
				renderTo: diffExResultsDiv.getId(),
				title: "Differentially expressed genes",
				searchPanel: formPanel,
				viewConfig: {
					forceFit: true
				},
				width:'auto',
				style:'width:100%'
				
			});
			
			
			diffExResultsGrid.loadData(result);
			
			var link = formPanel.getDiffExBookmarkableLink();
			//diffExResultsGrid.setTitle(String.format("Differentially expressed genes <a href='{0}'>(bookmarkable link)</a> <a target='_blank' href='{0}&export'>(export as text)</a>", link));
			diffExResultsGrid.setTitle(String.format("Differentially expressed genes <a target='_blank' href='{0}&export'>(export as text)</a>", link));

			}
		},this);
	
	},
	
	showCoExResults: function(searchForm, result, coexResultsTabPanel, searchPanel){
	
		var coexOptions = new Gemma.CoexpressionSearchOptions();
		coexOptions.on('rerunSearch', function(stringency, forceProbe, queryGenesOnly){
			coexOptions.hide();
			coexResultsTabPanel.removeAll();
			searchForm.redoRecentCoexpressionSearch(stringency, forceProbe, queryGenesOnly);
		}, this);
		/*
	 * Report any errors.
	 */
		if (result.errorState) {
		
			var emptyPanel = new Ext.Panel({
				html: "<br> " + result.errorState,
				border: false,
				title: "No Coexpression Search Results"
			});
			
			
			coexResultsTabPanel.add(emptyPanel);
			//Ext.DomHelper.overwrite('analysis-results-search-form-messages', result.errorState);
			//if (knownGeneGrid) {knownGeneGrid.getStore().removeAll();}
			coexResultsTabPanel.show();
			coexResultsTabPanel.doLayout();
			emptyPanel.show();
			
			return;
		}
		
		
		
		var knownGeneGrid = new Gemma.CoexpressionGrid({
			title: "Coexpressed genes",
			user: this.user,
			tabPanelViewFlag: true
		});
		
		var cytoscapePanel = new Gemma.CytoscapePanel({
			id : "cytoscaperesults",
			title: "Cytoscape",
			queryGenes: result.queryGenes,
			knownGeneResults: result.knownGeneResults,
			coexCommand: searchForm.getLastCoexpressionSearchCommand(),
			coexGridRef: knownGeneGrid,
			searchPanelRef: searchForm,
			width: 850,
			hideMode: 'visibility'
		
		});

		searchPanel.collapse();
		
		coexResultsTabPanel.add(knownGeneGrid);
		coexResultsTabPanel.add(cytoscapePanel);
		coexResultsTabPanel.show();
		coexResultsTabPanel.doLayout();
		
		knownGeneGrid.cytoscapeRef=cytoscapePanel;
		knownGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
				result.knownGeneDatasets, result.knownGeneResults, Gemma.CoexValueObjectUtil.getCurrentQueryGeneIds(result.queryGenes));
			
		knownGeneGrid.show();
				
	}
});
