
Ext.namespace('Gemma');

/**
 * The input form to run coexpression and differential expression searches. This
 * form has three main parts: a search mode chooser, an experiment (group) searcher, and a
 * gene (group) searcher
 *  
 * @author thea
 * @version $Id: AnalysisResultsSearchForm.js,v 1.34 2011/05/06 04:02:25 paul
 *          Exp $
 */
Gemma.AnalysisResultsSearchForm = Ext.extend(Ext.FormPanel, {


	exampleQueries:{
		diffEx: [{
			goId: "GO_0021766",
			eeSetId: '6112',
			taxonId: '1',
			backupGeneIds : [175764, 57412, 33449, 22652, 172517, 365527, 154351, 164380, 163012, 36178, 258329, 325340, 119501, 161166, 169774, 43145, 12948, 74699, 203063, 120960, 33479, 322804, 88959, 12966, 7187, 136503, 33369, 57883, 73088, 174546, 74174, 57397, 36158]
		},{
			goId: "GO_0021879",
			eeSetId: '6110',
			taxonId: '2',
			backupGeneIds : [500611, 534025, 574982, 633950, 550316, 534368, 537487, 574759, 556740, 583115, 634211, 534401, 500595]
		}],
		coex:[{
			goId: "GO_0051302",
			eeSetId: '6115',
			taxonId: '11',
			backupGeneIds : [7678763, 7678783, 7676882, 7694443, 7685764, 7667629, 7672893, 7673265, 7686100, 7697083, 7670169, 7692953]
		},{
			goId: "GO_0035418",
			eeSetId: '737',
			taxonId: '1',
			backupGeneIds : [269935, 194669, 232747, 36104, 316763]
			}]
	},

	// collapsible:true,
	layout : 'table',
	layoutConfig : {
		columns : 5
	},
	width : 900,
	// height : 200,
	frame : false,
	border : false,
	bodyBorder : false,
	bodyStyle : "backgroundColor:white",
	defaults : {
		border : false
	},
	ctCls : 'titleBorderBox',

	stateful : false,
	stateEvents : ["beforesearch"],
	eeSetReady : false,
	taxonId: null,

	defaultIsDiffEx: true,
	hidingExamples: false,
	
	geneIds : [],
	geneGroupId : null, // keep track of what gene group has been selected
	experimentIds : [],

	
	handleWarning : function(msg, e) {
		if (Ext.get("analysis-results-search-form-messages")) {
			Ext.DomHelper.overwrite("analysis-results-search-form-messages", {
				tag: 'img',
				src: '/Gemma/images/icons/warning.png'
			});
			
			if (!(msg.length === 0)) {
				Ext.DomHelper.append("analysis-results-search-form-messages", {
					tag: 'span',
					html: "&nbsp;&nbsp;" + msg
				});
			}
			else {
				Ext.DomHelper.append("analysis-results-search-form-messages", {
					tag: 'span',
					html: "&nbsp;&nbsp;Error retrieving results."
				});
			}
		}
		else {
			if (!(msg.length === 0)) {
				this.fireEvent("handleError", msg);
			}
			else {
				this.fireEvent("handleError", "Error retrieving results.");
			}
		}
	},

	handleError : function(msg, e) {
		this.handleWarning(msg,e);
		
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, e);
		if (e && !(msg.length === 0)) {
			Ext.Msg.alert("Error", e + "/n" + msg);
		}
	},

	clearError : function() {
		if (Ext.get("analysis-results-search-form-messages")) {
			Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
		}
	},

	wereSelectionsModified: function(){
		var wereModified = false;
		this.geneChoosers.items.each(function(){
			if ( this instanceof Gemma.GeneSearchAndPreview && this.getSelectedGeneOrGeneSetValueObject()) {
				if( this.listModified){
					wereModified = true;
				}
			}
		});
		if(!wereModified){
			this.experimentChoosers.items.each(function(){
				if ( this instanceof Gemma.ExperimentSearchAndPreview && this.getSelectedExperimentOrExperimentSetValueObject()) {
					if( this.listModified){
						wereModified = true;
					}
				}
			});
		}
		
		return wereModified;
	},
	getGeneSessionGroupQueries: function(){
		var queries = [];
		this.geneChoosers.items.each(function(){
			if ( this instanceof Gemma.GeneSearchAndPreview && this.getSelectedGeneOrGeneSetValueObject()) {
				if( this.queryUsedToGetSessionGroup !== null ){
					queries.push(this.queryUsedToGetSessionGroup);
				}
			}
		});
		return queries;
	},
	getExperimentSessionGroupQueries: function(){
		var queries = [];
		this.experimentChoosers.items.each(function(){
			if ( this instanceof Gemma.ExperimentSearchAndPreview && this.getSelectedExperimentOrExperimentSetValueObject()) {
				if( this.queryUsedToGetSessionGroup !== null ){
					queries.push(this.queryUsedToGetSessionGroup);
				}
			}
		});
		return queries;
	},
	getSelectedGeneAndGeneSetValueObjects : function() {
		var selectedVOs = [];
		this.geneChoosers.items.each(function(){
			if ( this instanceof Gemma.GeneSearchAndPreview && this.getSelectedGeneOrGeneSetValueObject() ) {
				selectedVOs.push(this.getSelectedGeneOrGeneSetValueObject());
			}
		});
		return selectedVOs;
	},
	/**
	 * get selections as geneSetValueObjects (selected single genes will be wrapped as single-member geneSets)
	 */
	getSelectedAsGeneSetValueObjects : function() {
		var selectedVOs = this.getSelectedGeneAndGeneSetValueObjects();
		var selectedAsGeneSets = [];
		var i;
		for(i = 0; i<selectedVOs.length;i++){
			if(selectedVOs[i] instanceof GeneValueObject){
				var gene = selectedVOs[i];
				var singleGeneSet = new SessionBoundGeneSetValueObject();
				singleGeneSet.id = null;
				singleGeneSet.geneIds = [gene.id];
				singleGeneSet.name = gene.officialSymbol;
				singleGeneSet.description = gene.officialName;
				singleGeneSet.size = gene.size;
				singleGeneSet.taxonName = gene.taxonCommonName;
				singleGeneSet.taxonId = gene.taxonId;
				singleGeneSet.modified = false;
				selectedAsGeneSets.push(singleGeneSet);
			}else{
				selectedAsGeneSets.push(selectedVOs[i]);
			}
		}
		return selectedAsGeneSets;
	},

	getSelectedExperimentAndExperimentSetValueObjects : function() {
		var selectedVOs = [];
		this.experimentChoosers.items.each(function() {
			if ( this instanceof Gemma.ExperimentSearchAndPreview  && this.getSelectedExperimentOrExperimentSetValueObject() ) {
				selectedVOs.push(this.getSelectedExperimentOrExperimentSetValueObject());
			}
		});
		return selectedVOs;
	},
	/**
	 * get selections as expressionExperimentSetValueObjects (selected single genes will be wrapped as single-member experimentSets)
	 */
	getSelectedAsExperimentSetValueObjects : function() {
		var selectedVOs = this.getSelectedExperimentAndExperimentSetValueObjects();
		var selectedAsExperimentSets = [];
		var i;
		for(i = 0; i<selectedVOs.length;i++){
			if(selectedVOs[i] instanceof ExpressionExperimentValueObject){
				var ee = selectedVOs[i];
				// maybe this should be a call to the backend?
				var singleExperimentSet = new SessionBoundExpressionExperimentSetValueObject();
				singleExperimentSet.id = null;
				singleExperimentSet.expressionExperimentIds = [ee.id];
				singleExperimentSet.name = ee.shortName;
				singleExperimentSet.description = ee.name;
				singleExperimentSet.size = ee.numExperiments;
				singleExperimentSet.taxonName = ee.taxon;
				singleExperimentSet.taxonId = ee.taxonId;
				singleExperimentSet.modified = false;
				selectedAsExperimentSets.push(singleExperimentSet);
			}else{
				selectedAsExperimentSets.push(selectedVOs[i]);
			}
		}
		return selectedAsExperimentSets;
	},

	getExperimentIds : function() {
		var eeIds = [];
		var i;
		var j;
		var selectedVOs = this.getSelectedExperimentAndExperimentSetValueObjects();
		for (i = 0; i < selectedVOs.length; i++) {
			var vo = selectedVOs[i];
			if ( vo instanceof ExpressionExperimentValueObject) {
				eeIds.push(vo.id);
			} else if (vo instanceof ExpressionExperimentSetValueObject) {
				eeIds = eeIds.concat(vo.expressionExperimentIds);
			}
		}
		return eeIds;
	},

	getGeneIds : function() {
		var geneIds = [];
		var i;
		var j;
		var selectedVOs = this.getSelectedGeneAndGeneSetValueObjects();
		for (i = 0; i < selectedVOs.length; i++) {
			var vo = selectedVOs[i];
			if (vo instanceof GeneValueObject) {
				geneIds.push(vo.id);
			} else if (vo instanceof GeneSetValueObject) {
				geneIds = geneIds.concat(vo.geneIds);
			}
		}
		return geneIds;
	},
	
	runSearch :function(){
		
		this.searchMethods = new Gemma.AnalysisResultsSearchMethods({
			taxonId: this.getTaxonId(),
			taxonName: this.getTaxonName(),
			showClassicDiffExResults: this.showClassicDiffExResults
		});
		
		this.searchMethods.on('beforesearch', function(){
			this.fireEvent('beforesearch', this);
		}, this);	
				
		this.searchMethods.on('showDiffExResults', function(result, data){
			Ext.apply(data,{
				geneSessionGroupQueries : this.getGeneSessionGroupQueries(),
				experimentSessionGroupQueries : this.getExperimentSessionGroupQueries(),
				selectionsModified : this.wereSelectionsModified(),
				showTutorial: this.runningExampleQuery
			});
			this.fireEvent('showDiffExResults', this, result, data);
		}, this);
				
		this.searchMethods.on('showCoexResults', function(result){
			this.fireEvent('showCoexResults', this, result, this.runningExampleQuery);
		}, this);
				
		this.searchMethods.on('aftersearch', function(result){
			this.loadMask.hide();
			this.fireEvent('aftersearch', this, result);
		}, this);

		this.searchMethods.on('warning', function(msg, e){
			this.handleWarning(msg, e);
		}, this);
				
		this.searchMethods.on('error', function(msg, e){
			this.handleError(msg, e);
		}, this);
		
		this.searchMethods.on('clearerror', function(){
			this.clearError();
		}, this);
		
		
		this.collapsePreviews();
		this.toggleHidingExamples();
		
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : Gemma.StatusText.Searching.analysisResults,
						msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
					});
		}
		this.loadMask.show();
		if (this.coexToggle.pressed) {
			this.searchMethods.searchCoexpression(this.getSelectedAsGeneSetValueObjects(), this.getSelectedAsExperimentSetValueObjects());
		} else {
			this.searchMethods.searchDifferentialExpression(this.getSelectedAsGeneSetValueObjects(), this.getSelectedAsExperimentSetValueObjects());
		}
	},
		
	/**
	 * public method to re-run the previous search with different options
	 * used if the user changes an option (ex stringency)
	 * applies new options to csc and makes a call to doCoexpressionSearch
	 * 
	 * no null or undefined parameters!
	 * 
	 * @param {Object} stringency must be an integer
	 * @param {Object} probeLevel must be true or false
	 * @param {Object} queryGenesOnly must be true or false
	 */
	redoRecentCoexpressionSearch: function(stringency, probeLevel, queryGenesOnly){
		if (!this.searchMethods || this.searchMethods.getLastCSC() === null) {
			return "No search to repeat";
		}
		
		this.clearError();
		var lastCSC = this.searchMethods.getLastCSC();
		Ext.apply(lastCSC, {
			stringency: stringency,
			forceProbeLevelSearch: probeLevel,
			queryGenesOnly: queryGenesOnly
		});
		this.searchMethods.doCoexpressionSearch(lastCSC);
		return "";
	},
	getLastCoexpressionSearchCommand: function(){
		return (this.searchMethods)?this.searchMethods.getLastCoexpressionSearchCommand():null;
	},
	initComponent : function() {

		/** get components* */
		// experiment chooser panels
		this.experimentChoosers = new Ext.Panel({
					// width: 319,
					frame : false,
						bodyStyle:'background-color:transparent',
					defaults : {
						border : false,
						bodyStyle:'background-color:transparent'
					},
					style : 'padding-bottom: 10px',
					autoDestroy : true
				});
		this.experimentChooserIndex = -1;
		this.addExperimentChooser();

		// gene chooser panels
		this.geneChoosers = new Ext.Panel({
					// width: 319,
					frame : false,
					defaults : {
						border : false
					},
					style : 'padding-bottom: 10px;',
					autoDestroy : true
				});
		this.geneChooserIndex = -1;
		this.addGeneChooser();

		/**
		 * ***** BUTTONS
		 * *******************************************************************************
		 */
		this.coexToggle = new Ext.Button({
					text : "<span style=\"font-size:1.3em\">Coexpression</span>",
					cls:'highlightToggle',
					scale : 'medium',
					width : 150,
					enableToggle : true,
					pressed : !this.defaultIsDiffEx
				});
		this.coexToggle.on('click', function() {
			this.coexToggle.toggle(true);
			this.diffExToggle.toggle(false);
		}, this);
		this.diffExToggle = new Ext.Button({
			text: "<span style=\"font-size:1.3em\">Differential Expression</span>",
			scale: 'medium',
			cls: 'highlightToggle',
			width: 150,
			enableToggle: true,
			pressed: this.defaultIsDiffEx
		});
		this.diffExToggle.on('click', function() {
			this.diffExToggle.toggle(true);
			this.coexToggle.toggle(false);
			
		}, this);

		this.searchBar = new Ext.Panel({

					border : false,
					layout : 'table',
					layoutConfig:{
						columns:5
					},
					width : 490,
					style : 'margin: 0 7px',
					defaults : {
						border : false
						
					},
					items : [{
								html : 'Test for ',
								style : 'white-space: nowrap;text-align:center;vertical-align:middle;font-size:1.7em;margin-top:7px'
							}, this.coexToggle, {
								html : 'or',
								style : 'white-space: nowrap;text-align:center;vertical-align:middle;font-size:1.7em;margin-top:7px'
							}, this.diffExToggle, {
								html : 'of:',
								style : 'white-space: nowrap;text-align:center;vertical-align:middle;font-size:1.7em;margin-top:7px'
							}]

				});

		/*************** TEXT *********************/

				this.theseExperimentsPanel = new Ext.Panel({
					html: 'these experiments',
					style: 'text-align:center;font-size:1.4em;',
					tpl: new Ext.XTemplate('these <span class="blue-text-not-link" style="font-weight:bold " ',
						 'ext:qtip="'+Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.taxonModeTT+'">',
						 '{taxonCommonName} </span> experiments ', 
						 '<img src="/Gemma/images/icons/question_blue.png" title="'+
						 Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.taxonModeTT+'"/> '),
					tplWriteMode: 'overwrite'
				});
				this.theseGenesPanel = new Ext.Panel({
					html: 'these genes',
					style: 'text-align:center;font-size:1.4em;',
					tpl: new Ext.XTemplate('these <span class="blue-text-not-link" style="font-weight:bold " ', 
						'ext:qtip="'+Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.taxonModeTT+'">', 
						'{taxonCommonName}</span> genes ', 
						'<img src="/Gemma/images/icons/question_blue.png" title="'+
						Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.taxonModeTT+'"/> '),
					tplWriteMode: 'overwrite'
				});

				this.searchExamples = new Ext.Panel({
					ref: 'searchExamples',
					colspan:4,
					cls : 'left-align-btn transparent-btn transparent-btn-link',
					items:[{
						ref: 'examplesTitle',
						tpl: 'Example Queries: <a href="javascript:void(0);">[ {sign} ]</a>',
						data: {sign:'-'},
						border: false,
						hidingExamples: false,
						listeners: {
							'render': function(){
									this.body.on('click', function(e){
										e.stopEvent();
										this.fireEvent('toggleHideExamples');
									}, this, {
										delegate: 'a'
									});
							},
							'toggleHideExamples': {
								fn: this.toggleHidingExamples,
								scope: this
							}
						}
					},{
						ref: 'diffExExamples',
						border:false,
						hidden: !this.defaultIsDiffEx,
						items: [{
							xtype: 'button',
							ref: 'diffExExample1',
							text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx1Text,
							tooltip: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx1TT,
							listeners: {
								click: function(){
									this.runExampleQuery(this.exampleQueries.diffEx[0]);
								},
								scope: this
							}
						
						}, {
							xtype: 'button',
							ref: 'diffExExample2',
							text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx2Text,
							tooltip: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx2TT,
							listeners: {
								click: function(){
									this.runExampleQuery(this.exampleQueries.diffEx[1]);
								},
								scope: this
							}
						
						}]
					},{
						ref: 'coexExamples',
						border: false,
						hidden: this.defaultIsDiffEx,
						items: [{
							xtype: 'button',
							ref: 'coexExample1',
							text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.coex1Text,
							tooltip: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.coex1TT,
							listeners: {
								click: function(){
									this.runExampleQuery(this.exampleQueries.coex[0]);
								},
								scope: this
							}
						
						}
//						,{
//							xtype: 'button',
//							ref: 'coexExample2',
//							text: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.coex2Text,
//							tooltip: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.coex2TT,
//							listeners: {
//								click: function(){
//									this.runExampleQuery(this.exampleQueries.coex[1]);
//								},
//								scope: this
//							}
						
//						}
					]
					}]
				});
				
				this.diffExToggle.on('toggle', function(){
					if (this.diffExToggle.pressed) {
						this.searchExamples.diffExExamples.show();
						this.searchExamples.coexExamples.hide();
					}
					else {
						this.searchExamples.diffExExamples.hide();
						this.searchExamples.coexExamples.show();
					}
				}, this);
		/*************** PUT ITEMS IN PANEL *********************/

		Ext.apply(this, {
			style : '',
			items: {
				xtype: 'fieldset',
				title: '&nbsp;',
				border: true,
				searchBar: this.searchBar,
				listeners: {
					render: function(c){
						var floatType = Ext.isIE ? 'styleFloat' : 'cssFloat'; // work
						// around
						// Ext
						// bug
						c.header.child('span').applyStyles(floatType + ':left;padding:5px 5px 0 0');
						this.searchBar.render(c.header, 1);
						// this.searchBar.wrap.applyStyles(floatType + ':left');
						c.on('destroy', function(){
							this.searchBar.destroy();
						}, c, {
							single: true
						});
					}
				},
				items: [{
					layout: 'table', // needs to be table so panel stretches with content growth
					layoutConfig:{
						columns:4
					},
					width: 850,
					border: false,
					defaults: {
						border: false,
						bodyStyle: 'padding: 0px;margin:0px'
					},
					items: [{
						defaults: {
							border: false
						},
						items: [this.theseGenesPanel, this.geneChoosers]
					}, {
						html: 'in ',
						style: 'white-space: nowrap;font-size:1.7em;padding-top: 32px;padding-right:5px;'
					}, {
						defaults: {
							border: false
						},
						items: [this.theseExperimentsPanel, this.experimentChoosers]
					}, {
						style:'padding:20 0 0 0px;margin:0px;',
						items: [{
							xtype: 'button',
							text: "<span style=\"font-size:1.3em;padding-top:15px\">Go!</span>",
							width: 55,
							tooltip:'Run the search',
							scale: 'medium',
							listeners: {
								click: function(){
									this.runningExampleQuery = false;
									this.runSearch();
								}.createDelegate(this, [], false)
							}
						
						}, {
							xtype: 'button',
							width: 55,
							icon: '/Gemma/images/icons/arrow_refresh_small.png',
							style: 'margin-top: 8px',
							text:'Reset',
							tooltip:'Clear all selections and reset the taxon mode ',
							handler: this.reset.createDelegate(this)
						}]
					},this.searchExamples]
				}]
			}
		});

		Gemma.AnalysisResultsSearchForm.superclass.initComponent.call(this);

		this.addEvents('beforesearch', 'aftersearch', 'showDiffExResults', 'showCoexResults');

		this.doLayout();

	},
	
	reset: function(){
		
		// remove all experiment and gene choosers
		this.geneChoosers.removeAll();
		this.experimentChoosers.removeAll();
		
		//reset taxon
		this.taxonId = null;
		
		this.addGeneChooser();
		this.addExperimentChooser();
		
		// reset taxon id and titles
		Ext.DomHelper.overwrite(this.theseGenesPanel.body, {
					cn : 'these genes'
				});
		Ext.DomHelper.overwrite(this.theseExperimentsPanel.body, {
					cn : 'these experiments'
				});
	},

	getTaxonId : function() {
		return this.taxonId;
	},
	setTaxonId : function(taxonId) {
		this.taxonId = taxonId;
		// set taxon for ALL geneChooser elements
		this.geneChoosers.items.each(function() {
					if (this instanceof Gemma.GeneSearchAndPreview ) {
						this.geneCombo.setTaxonId(taxonId);
					}
				});
		this.experimentChoosers.items.each(function() {
					if (this instanceof Gemma.ExperimentSearchAndPreview ) {
						this.experimentCombo.setTaxonId(taxonId);
					}
				});
				
	},
	getTaxonName : function() {
		return this.taxonName;
	},
	setTaxonName : function(taxonName) {
		this.taxonName = taxonName;
		
		this.theseExperimentsPanel.update({taxonCommonName:taxonName});
		this.theseGenesPanel.update({taxonCommonName:taxonName});
	},
	/**
	 * Check if the taxon needs to be changed, and if so, update the
	 * geneAndGroupCombo and reset the gene preivew
	 * 
	 * @param {}
	 *            taxonId
	 */
	taxonChanged : function(taxonId, taxonName) {

		// if the 'new' taxon is the same as the 'old' taxon for the experiment
		// combo, don't do anything
		if (taxonId && this.getTaxonId() && (this.getTaxonId() === taxonId)) {
			return;
		}
		// if the 'new' and 'old' taxa are different, reset the gene preview and
		// filter the geneCombo
		else if (taxonId) {
			this.setTaxonId(taxonId);
			this.setTaxonName(taxonName);
		}

		this.fireEvent("taxonchanged", taxonId);
	},

	// collapse all gene and experiment previews
	collapsePreviews : function() {
		this.collapseGenePreviews();
		this.collapseExperimentPreviews();
	},

	collapseGenePreviews : function() {
		if (typeof this.geneChoosers.items !== 'undefined') {
			this.geneChoosers.items.each(function() {
						if (this instanceof Gemma.GeneSearchAndPreview ) {
							this.collapsePreview(false);
						}
					});
		}
	},

	collapseExperimentPreviews : function() {
		if (typeof this.experimentChoosers.items !== 'undefined') {
			this.experimentChoosers.items.each(function() {
						if (this instanceof Gemma.ExperimentSearchAndPreview ) {
							this.collapsePreview(false);
						}
					});
		}

	},

	addGeneChooser : function( ) {
		this.geneChooserIndex++;
		
		var chooser = new Gemma.GeneSearchAndPreview({
			searchForm: this,
				style: 'padding-top:10px;',
				id: 'geneChooser' + this.geneChooserIndex,
				taxonId: this.taxonId,
				listeners: {
					madeFirstSelection: function(){
						// allows multiple set selections,
						// functionality removed for now
						// this.searchForm.addGeneChooser();
					},
					removeGene: function(){
						this.searchForm.removeGeneChooser(this.getId());
					}
				}
		});

		this.geneChoosers.add(chooser);
		
		// change previous button to 'remove'
		if (typeof Ext.getCmp('geneChooser' + (this.geneChooserIndex - 1) + 'Button') !== 'undefined') {
			Ext.getCmp('geneChooser' + (this.geneChooserIndex - 1) + 'Button').show()
					.setIcon('/Gemma/images/icons/delete.png').setTooltip('Remove this gene or group from your search')
					.setHandler(this.removeGeneChooser.createDelegate(this, ['geneChooserPanel' +
									 (this.geneChooserIndex - 1)], false));
		}
		this.geneChoosers.doLayout();
		
		return chooser;
	},

	removeGeneChooser : function(panelId) {
		this.geneChoosers.remove(panelId, true);
		this.geneChoosers.doLayout();
		
		if(this.getSelectedExperimentAndExperimentSetValueObjects().length === 0 && this.getSelectedGeneAndGeneSetValueObjects().length === 0){
			this.reset();
		}
	},

	addExperimentChooser : function() {
		this.experimentChooserIndex++;

		this.experimentChoosers.add({
			xtype: 'experimentSearchAndPreview',
			searchForm: this,
			taxonId: this.taxonId,
			style: 'padding-top:10px;',
			id: 'experimentChooser' + this.experimentChooserIndex,
			listeners: {
				madeFirstSelection: function(){
					// allows multiple set selections,
					// functionality removed for now
					// this.searchForm.addExperimentChooser();
				},
				removeExperiment: function(){
					this.searchForm.removeExperimentChooser(this.getId());
				}
			}
		});

		this.experimentChoosers.doLayout();
	},

	removeExperimentChooser : function(panelId) {
		this.experimentChoosers.remove(panelId, true);
		this.experimentChoosers.doLayout();
		
		if(this.getSelectedExperimentAndExperimentSetValueObjects().length === 0 && this.getSelectedGeneAndGeneSetValueObjects().length === 0){
			this.reset();
		}

	},

	wereSelectionsModified: function(){
		var wereModified = false;
		this.geneChoosers.items.each(function(){
			if ( this instanceof Gemma.GeneSearchAndPreview && this.getSelectedGeneOrGeneSetValueObject()) {
				if( this.listModified){
					wereModified = true;
				}
			}
		});
		if(!wereModified){
			this.experimentChoosers.items.each(function(){
				if ( this instanceof Gemma.ExperimentSearchAndPreview && this.getSelectedExperimentOrExperimentSetValueObject()) {
					if( this.listModified){
						wereModified = true;
					}
				}
			});
		}
		
		return wereModified;
	},
	/**
	 * hide the example queries
	 */
	toggleHidingExamples : function(){
		
		//this.searchExamples.hide();
		//var toggleHidingExamples = function(){
					var h = !this.hidingExamples;
					this.searchExamples.diffExExamples.diffExExample1.setVisible(h);
					this.searchExamples.diffExExamples.diffExExample2.setVisible(h);
					this.searchExamples.coexExamples.coexExample1.setVisible(h);
					//this.searchExamples.coexExamples.coexExample2.setVisible(h);
					if (h) {
						this.searchExamples.examplesTitle.update({
							sign: '-'
						});
					} else {
						this.searchExamples.examplesTitle.update({
							sign: '+'
						});
					}
				this.hidingExamples = !this.hidingExamples; 
		//		};
	},
	/**
	 * hide the example queries
	 */
	showExampleQueries : function(){
		this.searchExamples.show();
	},
	/**
	 * set the first experiment chooser to have chosen a set and show its preview
	 * @param setId must be a valid id for a database-backed experimetn set
	 */
	addExperimentSet: function( setId ){
		
		// get the chooser to inject
		var chooser = this.experimentChoosers.getComponent(0);
		
		// allows multiple set selections,
		// functionality removed for now
		//this.addExperimentChooser();
		var myscope = this;	
			
		// make a gene combo record for the db-backed experimentSetValueObject
		ExpressionExperimentSetController.load( setId , function(eeSet){
				
			var record = new Gemma.ExperimentAndExperimentGroupComboRecord({
				name : eeSet.name,
				description: eeSet.descrption,
				isGroup : true,
				size: eeSet.expressionExperimentIds.length,
				taxonId : eeSet.taxonId,
				taxonName :eeSet.taxonName,
				memberIds : eeSet.expressionExperimentIds,
				resultValueObject : eeSet,
				userOwned : false
			});
			
			// get the chooser's gene combo
			var eeCombo = chooser.experimentCombo;
			
			// insert record into gene combo's store
			eeCombo.getStore().insert( 0, record );
			
			// tell gene combo the GO group was selected
			//eeCombo.select(0);
			eeCombo.fireEvent('select', eeCombo, record, 0);
			myscope.fireEvent('eeExampleReady');
		});
		
	},
	/**
	 * set the gene chooser to have chosen a go group and show its preview
	 * @param geneSetId must be a valid id for a database-backed gene set
	 */
	addGOGeneSet: function(goName, taxonId, backupGeneIds){
	
		// get the chooser to inject
		var chooser = this.geneChoosers.getComponent(0);
		
		
		// allows multiple set selections,
		// functionality removed for now
		//this.addGeneChooser();
		var myscope = this;
		
		// make a gene combo record for the db-backed experimentSetValueObject
		GenePickerController.getGeneSetByGOId(goName, taxonId, function(geneSet){
		
			// if the GO id failed to match a set, use the hard coded back up list of genes
			// (this might happen if Berkeleybop is down, see bug 2534)
			if ( geneSet === null) {
				geneSet = chooser.makeSessionBoundGeneSet(backupGeneIds, taxonId, 'Backup gene list for '+goName, 'GO database unavailable, using backup list');
			}
			
			var record = new Gemma.GeneAndGeneGroupComboRecord({
				name: geneSet.name,
				description: geneSet.descrption,
				isGroup: true,
				size: geneSet.geneIds.length,
				taxonId: geneSet.taxonId,
				taxonName: geneSet.taxonName,
				memberIds: geneSet.geneIds,
				resultValueObject: geneSet,
				comboText: geneSet.name + ": " + geneSet.description,
				userOwned: false
			});
			// get the chooser's gene combo
			var geneCombo = chooser.geneCombo;
			
			// tell gene combo the GO group was selected
			geneCombo.fireEvent('select', geneCombo, record, 0);
			myscope.fireEvent('geneExampleReady');
			
		});
	},
	
	runExampleQuery: function(exampleConfig){
		
		var goName = exampleConfig.goId;
		var eeSetId = exampleConfig.eeSetId;
		var taxonId = exampleConfig.taxonId;
		var backupGeneIds = exampleConfig.backupGeneIds;
		
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : Gemma.StatusText.Searching.analysisResults,
						msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
					});
		}
		this.loadMask.show();
		// reset all the choosers
		this.reset();
		
		// taxon needs to be set before gene set is chosen because otherwise there will be >1 choice provided
		this.setTaxonId(taxonId);
		
		this.addExperimentSet(eeSetId);
		// set the gene chooser
		this.addGOGeneSet(goName, taxonId, backupGeneIds);
		
		var queryRun = false;
		var geneExampleReady = false;
		var eeExampleReady = false;
		this.on('geneExampleReady',function(){
			geneExampleReady = true;
			if(eeExampleReady && !queryRun){
				queryRun = true;
				this.runningExampleQuery = true;
				this.runSearch();
			}
		});
				
		this.on('eeExampleReady',function(){
			eeExampleReady = true;
			if (geneExampleReady && !queryRun) {
				queryRun = true;
				this.runningExampleQuery = true;
				this.runSearch();
			}
		});
							
	}

});

Ext.reg('analysisResultsSearchForm', Gemma.AnalysisResultsSearchForm);
