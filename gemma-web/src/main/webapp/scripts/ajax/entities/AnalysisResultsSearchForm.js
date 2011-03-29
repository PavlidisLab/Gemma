/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

/**
 * The input for guided coexpression and differential expression searches. 
 * This form has four main parts: a taxon chooser, an experiment (group) searcher, 
 * a gene (group) searcher and links to perform searches
 * 
 * Coexpression search has an optional part that appears if the user is doing a 'custom'
 * analysis, I've used defaults for these options:
 * Stringency = 2
 * "Force Probe query" = false
 * "Use my data" = false
 * "My genes only" = false
 * 
 * 
 */

Gemma.MAX_GENES_PER_QUERY = 20; // this is the value used for coexpression and diff expression searches 

Gemma.AnalysisResultsSearchForm = Ext.extend(Ext.Panel, {

	layout : 'table',
	layoutConfig:{ columns:8},
	width : 900,
	//height : 200,
	frame : false,
	stateful : false,
	stateEvents : ["beforesearch"],
	eeSetReady : false,
	border:true,
	bodyBorder:false,
	bodyStyle:"backgroundColor:white",
	defaults:{border:false},
		
	PREVIEW_SIZE : 5,
	
	// defaults for coexpression
	DEFAULT_STRINGENCY : 2,
	DEFAULT_forceProbeLevelSearch : false,
	DEFAULT_useMyDatasets : false,
	DEFAULT_queryGenesOnly: false,
	
	// defaults for differential expression
	// using Gemma.DEFAULT_THRESHOLD, Gemma.MIN_THRESHOLD, Gemma.MAX_THRESHOLD
	

	// share state with main page...
	stateId : "Gemma.CoexpressionSearch",
	geneIds : [],
	geneGroupId : null, // keep track of what gene group has been selected
	experimentIds : [],

	listeners: {
		'ready': function(){
			this.loadMask.hide();
			this.loadMask = new Ext.LoadMask(this.getEl(), {
				msg: "Searching for analysis results ..."
			});
		}
	},


	/**************************************************************
	 **                          SEARCH							 **
	 **************************************************************/

	doSearch : function() {
		
		this.fireEvent('beforesearch', this);
		if(!this.loadMask){
			this.loadMask = new Ext.LoadMask(this.getEl(), {
				msg: "Searching for analysis results ..."
			});
		}
		
		// if coexpression button is depressed, do a coexpression search
		if(this.coexToggle.pressed){
			this.doCoexpressionSearch();
		}
		
		// if differential expression button is depressed, do a differential search
		if(this.diffExToggle.pressed){
			this.doDifferentialExpressionSearch();
		}

	},


	/**************************************************************
	 **                       COEXPRESSION						 **
	 **************************************************************/

	/**
	 * Construct the coexpression command object from the form, to be sent to the server.
	 * 
	 * @return {}
	 */
	getCoexpressionSearchCommand : function() {
		var newCsc = {};
		if (this.csc) {
			newCsc = this.csc;
		}

		Ext.apply(newCsc, {
					geneIds : this.geneIds,
					//stringency : Ext.getCmp('stringencyfield').getValue(),
					stringency : this.DEFAULT_STRINGENCY,
					forceProbeLevelSearch : this.DEFAULT_forceProbeLevelSearch,
					useMyDatasets : this.DEFAULT_useMyDatasets,
					queryGenesOnly : this.DEFAULT_queryGenesOnly,
					taxonId : this.getTaxonId()
				});

		if (this.eeComboSelectedRecord) {
			newCsc.eeIds = this.getExperimentIds();
			// only supply eeSetName and eeSetId if eeSet exists in db, otherwise will cause error
			// doesn't look like this is needed, so not setting for simplicity
			newCsc.eeSetName = null;
			newCsc.eeSetId = null;
			
		}
		return newCsc;
	},

	doCoexpressionSearch : function(csc) {
		if (!csc) {
			csc = this.getCoexpressionSearchCommand();
		}
		this.clearError();

		var msg = this.validateCoexSearch(csc);
		if (msg.length === 0) {
			this.loadMask.show();
			var errorHandler = this.handleError.createDelegate(this, [], true);
			ExtCoexpressionSearchController.doSearch(csc, {
					callback : this.returnFromCoexSearch.createDelegate(this),
					errorHandler : errorHandler
				});
		}else {
			this.handleError(msg);
		}
		if (typeof pageTracker !== 'undefined') {
				pageTracker._trackPageview("/Gemma/coexpressionSearch.doCoexpressionSearch");
		}
	},

	validateCoexSearch : function(csc) {
		if (csc.queryGenesOnly && csc.geneIds.length < 2) {
			return "You must select more than one query gene to use 'search among query genes only'";
		} else if (!csc.geneIds || csc.geneIds.length === 0) {
			return "We couldn't figure out which gene you want to query. Please use the search functionality to find genes.";
		} else if (csc.stringency < Gemma.MIN_STRINGENCY) {
			return "Minimum stringency is " + Gemma.MIN_STRINGENCY;
		} else if (csc.eeIds && csc.eeIds.length < 1) {
			return "There are no datasets that match your search terms";
		} else if (!csc.eeIds && !csc.eeSetId) {
			return "Please select an analysis. Taxon, gene(s), and scope must be specified.";
		} else if(csc.geneIds.length > Gemma.MAX_GENES_PER_QUERY){
			// if trying to search for more than the allowed limit of genes, show error
			
			// prune the gene Ids
			csc.geneIds = csc.geneIds.slice(0, Gemma.MAX_GENES_PER_QUERY);
			this.loadGenes(csc.geneIds);
			
			return "You can only search up to " + Gemma.MAX_GENES_PER_QUERY +
					" genes. Please note that your list of genes has been trimmed automatically. <br>"+
					"Press 'Go' again to run the search with this trimmed list or re-enter your gene query and "+
					"use the edit tool to manually trim your selection.";
										
		} else {
			return "";
		}
	},
	
		
	/**
	 * Create a URL that can be used to query the system.
	 * 
	 * @param {}
	 *            csc
	 * @return {}
	 */
	getCoexBookmarkableLink : function(csc) {
		if (!csc) {
			csc = this.getCoexpressionSearchCommand();
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		url += String.format("?g={0}&s={1}&t={2}&ee={3}", csc.geneIds.join(","), csc.stringency, csc.taxonId, csc.eeIds);
		if (csc.queryGenesOnly) {
			url += "&q";
		}
		if (csc.dirty) {
			url += "&dirty=1";
		}
		url = url.replace("home2","searchCoexpression");

		return url;
	},
	
	
	/**************************************************************
	 **               DIFFERENTIAL EXPRESSION					 **
	 **************************************************************/
	/**
	 * Construct the differential command object from the form, to be sent to the server.
	 * 
	 * @return {}
	 */
	getDiffSearchCommand : function() {

		var newDsc = {};
		if (this.dsc) {
			newDsc = this.dsc;
		}

		var dsfcs = [];

		/*
		 * eeFactorsMap has to be populated ahead of time.
		 */
		var efMap = this.efChooserPanel.eeFactorsMap;

		Ext.apply(newDsc, {
					geneIds : this.geneIds,
					selectedFactors : efMap,
					threshold : Gemma.DEFAULT_THRESHOLD ,
					taxonId : this.getTaxonId()
				});


		if (this.eeComboSelectedRecord) {
			newDsc.eeIds = this.getExperimentIds();
			// only supply eeSetName and eeSetId if eeSet exists in db, otherwise will cause error
			// doesn't look like this is needed, so not setting for simplicity
			newDsc.eeSetName = null;
			newDsc.eeSetId = null;

		}
		return newDsc;

	},
	
	/**
	 * Show the user interface for choosing factors. This happens asynchronously, so listen for the factors-chosen
	 * event.
	 * 
	 * 
	 */
	chooseFactors : function() {
		if (!this.eeComboSelectedRecord) {
			Ext.Msg.alert("Warning", "You must select an expression experiment set before choosing factors. Scope must be specified.");
		} else if (this.getExperimentIds().length === 0) {
			Ext.Msg.alert("Warning", "You should select at least one experiment to analyze");
		} else {
			var eeIds = this.getExperimentIds();
			this.efChooserPanel.show(eeIds);
		}
	},
	doDifferentialExpressionSearch : function(dsc) {
		
		this.chooseFactors();
		this.efChooserPanel.on("factors-chosen", function(efmap){
			this.doDifferentialExpressionSearch();


			if (!dsc) {
				dsc = this.getDiffSearchCommand();
			}
			this.clearError();
			var msg = this.validateDiffExSearch(dsc);
			if (msg.length === 0) {
				this.loadMask.show();
				var errorHandler = this.handleError.createDelegate(this, [], true);
				DifferentialExpressionSearchController.getDiffExpressionForGenes(dsc, {
					callback: this.returnFromDiffExSearch.createDelegate(this),
					errorHandler: errorHandler
				});
			} else {
				this.handleError(msg, e);
			}
			if (typeof pageTracker !== 'undefined') {
				pageTracker._trackPageview("/Gemma/differentialExpressionSearch.doSearch");
			}
		}, this, {
			single: true
			}
		);
		
	},
	getDiffExBookmarkableLink : function(dsc) {
		if (!dsc) {
			dsc = this.getDiffSearchCommand();
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		url += String.format("?g={0}&thres={1}&t={2}", dsc.geneIds.join(","), dsc.threshold, dsc.taxonId);


		 //Makes bookmarkable links somewhat unusable (too long)
		 if (dsc.eeIds) {
			 url += String.format("&ees={0}", dsc.eeIds.join(","));
		 }

		// won't always have a set name or set id
		/*if (dsc.eeSetId >= 0) {
			url += String.format("&a={0}", dsc.eeSetId);
		}
		if (dsc.eeSetName) {
			url += String.format("&setName={0}", dsc.eeSetName);
		}*/

		if (dsc.selectedFactors) {
			url += "&fm=";
			for (var i in dsc.selectedFactors) {
				var o = dsc.selectedFactors[i];
				if (!o.eeId) {
					continue;
				}
				url += o.eeId + "." + o.efId + ",";
			}
		}
		
		url = url.replace("home2","diff/diffExpressionSearch");

		return url;
	},
	validateDiffExSearch : function(dsc) {
		if (!dsc.geneIds || dsc.geneIds.length === 0) {
			return "We couldn't figure out which gene you want to query. Please use the search functionality to find genes.";
		} else if (dsc.threshold < Gemma.MIN_THRESHOLD) {
			return "Minimum threshold is " + Gemma.MIN_THRESHOLD;
		} else if (dsc.threshold > Gemma.MAX_THRESHOLD) {
			return "Maximum threshold is " + Gemma.MAX_THRESHOLD;
		} else if (dsc.eeIds && dsc.eeIds.length < 1) {
			return "There are no datasets that match your search terms";
		} else if (!dsc.eeIds && !dsc.eeSetId) {
			return "Please select an analysis. Taxon, gene(s), and scope must be specified.";
		}  else if(dsc.geneIds.length > Gemma.MAX_GENES_PER_QUERY){
			// if trying to search for more than the allowed limit of genes, show error
			
			// prune the gene Ids
			dsc.geneIds = dsc.geneIds.slice(0, Gemma.MAX_GENES_PER_QUERY);
			this.loadGenes(dsc.geneIds);
			
			return "You can only search up to " + Gemma.MAX_GENES_PER_QUERY +
					" genes. Please note that your list of genes has been trimmed automatically. <br>"+
					"Press 'Go' again to run the search with this trimmed list or re-enter your gene query and "+
					"use the edit tool to manually trim your selection.";
										
		}  else {
			return "";
		}
	},

	/**   Shared methods   **/

	handleError : function(msg, e) {
		Ext.DomHelper.overwrite("analysis-results-search-form-messages", {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});
				
		if (!(msg.length === 0)) {
			Ext.DomHelper.append("analysis-results-search-form-messages", {
				tag: 'span',
				html: "&nbsp;&nbsp;" + msg
			});
		}else{
			Ext.DomHelper.append("analysis-results-search-form-messages", {
				tag: 'span',
				html: "&nbsp;&nbsp;Error retrieving results."
			});
		}
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, e);
		if(e && !(msg.length === 0)){
			Ext.Msg.alert("Error",e+"/n"+msg);
		}
	},

	clearError : function() {
		Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
	},
	
	returnFromCoexSearch : function(result) {
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);
		this.fireEvent('showCoexResults', this, result);
	},
	returnFromDiffExSearch : function(result) {
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);
		this.fireEvent('showDiffExResults', this, result);
	},

	getExperimentIds : function() {
		if(this.experimentIds){
			return this.experimentIds; 
		}
		return [];
	},
	
	initComponent : function() {
		
		/**get components**/
		

		/****** EE COMBO ****************************************************************************/
		
		// Shows the combo box for EE groups 
		this.experimentCombo = new Gemma.ExperimentAndExperimentGroupCombo({
							typeAhead: false,
							width : 220
						});
		this.experimentCombo.on('select', function(combo, record, index) {
										
										// if the EE has changed taxon, reset the gene combo
										this.taxonChanged(record.get("taxonId"), record.get("taxonName"));
										
										// store the eeid(s) selected and load some EE into the previewer
										// store the taxon associated with selection
										var query = combo.store.baseParams.query;
										this.loadExperimentOrGroup(record, query);
										
										// once an experiment has been selected, cue the user that the gene select is now active
										Ext.get('visGeneGroupCombo').setStyle('background','white');
										this.eeComboSelectedRecord=record;
										
									},this);
		/******* EXPERIMENT SELECTION EDITOR ******************************************************************/		
				
		this.experimentSelectionEditor = new Gemma.ExpressionExperimentMembersGrid( {
			id: 'experimentSelectionEditor',
			height : 200,
			//hidden: 'true',
			hideHeaders:true,
			frame:false
		});
		
		this.experimentSelectionEditor.on('experimentListModified', function(newExperimentIds, groupName){
			if(newExperimentIds){
				this.loadExperiments(newExperimentIds);
			} if(groupName){
				this.experimentCombo.setRawValue(groupName);
			}
			this.experimentCombo.getStore().reload();
		},this);
		
		this.experimentSelectionEditor.on('doneModification', function(){
			this.getEl().unmask();
			this.experimentSelectionEditorWindow.hide();
			},this);

		this.experimentSelectionEditorBtn = new Ext.LinkButton({
							handler: this.launchExperimentSelectionEditor,//.createDelegate(this, [], false),
							scope : this,
							style: 'float:right;text-align:right; ',
							width: '200px',
							tooltip : "Edit your selection",
							hidden : true,
							disabled: true,
							ctCls: 'right-align-btn transparent-btn'
		});
		
		this.experimentSelectionEditorWindow = new Ext.Window({
				id:'experimentSelectionEditorWindow',
				//closeAction: 'hide',
				closable: false,
				layout: 'fit',
				items: this.experimentSelectionEditor,
				title: 'Edit Your Experiment Selection'
			});
		
		 
		/****** EE PREVIEW **************************************************************************/

		this.experimentPreviewContent = new Ext.Panel({
				width:210,
				id:'experimentPreview',
				html:'<div style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview</div>',
				tpl: new Ext.XTemplate(
				'<tpl for="."><div style="padding-bottom:7px;"><a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
				'{id}"',
				' ext:qtip="{shortName}">{shortName}</a>&nbsp; {name} <span style="color:grey">({taxon})</span></div></tpl>'),
				tplWriteMode: 'append'
		});	
		this.experimentPreviewExpandBtn = new Ext.Button({
							handler:function(){
								this.experimentPreviewExpandBtn.disable().hide();
								this.loadExperimentPreview();
							}.createDelegate(this, [], true),
							scope : this,
							style: 'float:right;',
							tooltip : "View your selection",
							hidden : true,
							disabled: true,
							icon : "/Gemma/images/plus.gif",
							cls : "x-btn-icon"
		});
		
		this.experimentPreview = new Ext.Panel({
				//height:100,
				width:219,
				frame:true,
				items:[this.experimentPreviewExpandBtn,this.experimentPreviewContent,this.experimentSelectionEditorBtn]
		});
		
		/****** GENE COMBO ******************************************************************************/
		/*this.geneCombo = new Gemma.MyEXTGeneAndGeneGroupCombo;*/
		this.geneCombo = new Gemma.GeneAndGeneGroupCombo({
					id : "visGeneGroupCombo",
					width:187,
					hideTrigger: false,
					typeAhead: false,
					style:'background:Gainsboro;', // because it starts disabled
					listeners : {
						'select' : {
							fn : function(combo, record, index) {
									var query = combo.store.baseParams.query;
									this.loadGeneOrGroup(record, query);
							},
							scope : this
						},
						'focus' : {
							fn : function(cb, rec, index) {
								if(!this.eeComboSelectedRecord){
									Ext.Msg.alert("Missing information", "Please select an experiment or experiment group first.");
								}
							},
							scope:this
						}
					}
				});
			
			this.symbolList = new Gemma.GeneImportPanel({
						listeners : {
							'commit' : {
								fn : this.getGenesFromList.createDelegate(this),
								scope : this
							}
						}
					});

			this.symbolListButton = new Ext.Button({
						icon : "/Gemma/images/icons/page_upload.png",
						cls : "x-btn-icon",
						tooltip : "Import multiple genes",
						disabled : false,
						handler : function() {

							if(!this.getTaxonId() || isNaN(this.getTaxonId())){
								Ext.Msg.alert("Missing information", "Please select an experiment or experiment group first.");
								return;
							}

							this.geneCombo.reset();
							this.symbolList.show();
						}.createDelegate(this, [], true)
					});
		
		
		/******* GENE SELECTION EDITOR ******************************************************************/		
		this.geneSelectionEditor = new Gemma.GeneMembersGrid( {
			id: 'geneSelectionEditor',
			height : 200,
			//hidden: 'true',
			hideHeaders:true,
			frame:false
		});
		
		this.geneSelectionEditor.on('geneListModified', function(newGeneIds, groupName){
			if(newGeneIds){
				this.loadGenes(newGeneIds);
			} if(groupName){
				this.geneCombo.setValue(groupName);
			}
			this.geneCombo.getStore().reload();
		},this);
		
		this.geneSelectionEditor.on('doneModification', function(){
			this.getEl().unmask();
			this.geneSelectionEditorWindow.hide();
			},this);

		this.geneSelectionEditorBtn = new Ext.LinkButton({
							handler: this.launchGeneSelectionEditor,//.createDelegate(this, [], false),
							scope : this,
							style: 'float:right;text-align:right; ',
							width: '200px',
							tooltip : "Edit your selection",
							hidden : true,
							disabled: true,
							ctCls: 'right-align-btn transparent-btn'
		});
		
		this.geneSelectionEditorWindow = new Ext.Window({
				id:'geneSelectionEditorWindow',
				//closeAction: 'hide',
				closable: false,
				layout: 'fit',
				items: this.geneSelectionEditor,
				title: 'Edit Your Gene Selection'
			});
		
		/****** GENE PREVIEW ****************************************************************************/
		
		//use this.genePreviewContent.update("one line of gene text"); to write to this panel
		this.genePreviewContent = new Ext.Panel({
				//height:100,
				width:207,
				id:'genePreview',
				html:'<div style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview</div>',
				tpl: new Ext.Template('<div style="padding-bottom:7px;"><a href="/Gemma/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName} <span style="color:grey">({taxonCommonName})</span></div>'),
				tplWriteMode: 'append' // use this to append to content when calling update instead of replacing
		});
		
		this.genePreviewExpandBtn = new Ext.Button({
							handler:function(){
								this.genePreviewExpandBtn.disable().hide();
								this.loadGenePreview();
							}.createDelegate(this, [], true),
							scope : this,
							style: 'float:right;',
							tooltip : "View your selection",
							hidden : true,
							disabled: true,
							icon : "/Gemma/images/plus.gif",
							cls : "x-btn-icon"
		});
		
		this.genePreview = new Ext.Panel({
				//height:100,
				width:219,
				frame:true,
				items:[this.genePreviewExpandBtn,this.genePreviewContent,this.geneSelectionEditorBtn]
		});
		/******* BUTTONS ********************************************************************************/

		this.coexToggle = new Ext.Button({
							text: "<span style=\"font-size:1.3em\">Coexpression</span>",
							style:'padding-bottom:0.4em',
							scale: 'medium',
							width:150,
							enableToggle:true,
							pressed:true
						});
		this.diffExToggle = new Ext.Button({
							text: "<span style=\"font-size:1.3em\">Differential Expression</span>",
							style:'padding-bottom:0.4em',
							scale: 'medium',
							width:150,
							enableToggle:true
						});


		/** Display items in form panel **/

		Ext.apply(this, {
			style:'',
			items:[{},{
						rowspan: 5,
						items: [{html:"<br>",border:false},this.coexToggle, this.diffExToggle]
					},
					{},{html: 'these experiments', style:'text-align:center;font-size:1.4em;'},
					{},{html: 'these genes', style:'text-align:center;font-size:1.4em;padding:0px'},
					{},{},
					
					{html:'Search for ', style:'text-align:center;vertical-align:middle;font-size:1.7em;', rowspan:1},
					{html: ' in ', style:'text-align:center;vertical-align:middle;font-size:1.7em;', rowspan:1},
					this.experimentCombo,
					{html: ' based on ', style:'text-align:center;vertical-align:middle;font-size:1.7em;', rowspan:1},
					this.geneCombo,this.symbolListButton, //,{border:false,html:'<a style="text-align:right">Paste symbol list</a>'}
					new Ext.Button({
							text: "<span style=\"font-size:1.1em\">Go!</span>",
							handler: this.doSearch.createDelegate(this, [], false),
							width:35
						}),
					
					{},{},this.experimentPreview,{},{items: this.genePreview,colspan: 2},{}
					]
		});
		
		/* factor chooser for differential expression*/
		this.efChooserPanel = new Gemma.ExperimentalFactorChooserPanel({
					modal : true
				});
		
		Gemma.AnalysisResultsSearchForm.superclass.initComponent.call(this);
		
		this.addEvents('beforesearch', 'aftersearch','showDiffExResults','showCoexResults');		

		this.doLayout();
		
	},
	
	getTaxonId: function(){
		return this.taxonId;
	},
	setTaxonId: function(taxonId){
		this.taxonId = taxonId;
	},
	
	getTaxonName: function(){
		return this.taxonName;
	},
	setTaxonName: function(taxonId){
		this.taxonName = taxonId;
	},

	/**
	 * Check if the taxon needs to be changed, and if so, update the geneAndGroupCombo and reset the gene preivew
	 * @param {} taxonId
	 */
	taxonChanged : function(taxonId, taxonName) {

		// if the 'new' taxon is the same as the 'old' taxon for the experiment combo, don't do anything
		if (taxonId && this.getTaxonId() && (this.getTaxonId() === taxonId) ) {
			return;
		}
		// if the 'new' and 'old' taxa are different, reset the gene preview and filter the geneCombo
		else if(taxonId){
			this.resetGenePreview();
			this.geneCombo.setTaxonId(taxonId);
			this.setTaxonId(taxonId);
			this.setTaxonName(taxonName);
			this.geneCombo.reset();
		}

		this.fireEvent("taxonchanged", taxonId);
	},
	collapsePreviews: function(){
		Ext.DomHelper.overwrite(Ext.getCmp('genePreview').body, {cn: '<span style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview </span>'});
		this.genePreviewExpandBtn.enable().show();
		this.geneSelectionEditorBtn.disable().hide();
		Ext.DomHelper.overwrite(Ext.getCmp('experimentPreview').body, {cn: '<span style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview </span>'});
		this.experimentPreviewExpandBtn.enable().show();		
		this.experimentSelectionEditorBtn.disable().hide();
	},
	resetGenePreview: function(){
		Ext.DomHelper.overwrite(Ext.getCmp('genePreview').body, {cn: '<div style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview </div>'});
		this.genePreviewExpandBtn.disable().hide();
		this.geneSelectionEditorBtn.disable().hide();
	},
	resetExperimentPreview: function(){
		Ext.DomHelper.overwrite(Ext.getCmp('experimentPreview').body, {cn: '<div style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview </div>'});
		this.experimentPreviewExpandBtn.disable().hide();
		this.experimentSelectionEditorBtn.disable().hide();
	},
	showGenePreview: function(){
		this.genePreview.loadMask.hide();
		this.geneSelectionEditorBtn.enable();
		this.geneSelectionEditorBtn.show();
	},
	showExperimentPreview: function(){
		this.experimentPreview.loadMask.hide();
		this.experimentSelectionEditorBtn.enable();
		this.experimentSelectionEditorBtn.show();
	},
							
	maskGenePreview: function(){
		if (!this.genePreview.loadMask) {
			this.genePreview.loadMask = new Ext.LoadMask(this.genePreview.getEl(), {
				msg: "Loading Genes ..."
			});
		}
		this.genePreview.loadMask.show();
	},
	maskExperimentPreview: function(){
		if (!this.experimentPreview.loadMask) {
			this.experimentPreview.loadMask = new Ext.LoadMask(this.experimentPreview.getEl(), {
				msg: "Loading Experiments ..."
			});
		}
		this.experimentPreview.loadMask.show();
	},
	getGenesMoreLink: function(size, limit){
		return '<div style="text-align:right"><a onclick="this.launchGeneSelectionEditor; return false;">' + (size - limit) + ' more<a></div>';
	},
	launchGeneSelectionEditor: function(){
		
		if(!this.geneIds || this.geneIds ===null || this.geneIds.length === 0){
			return;
		}
		this.getEl().mask();

		this.geneSelectionEditorWindow.show();

		this.geneSelectionEditor.loadMask = new Ext.LoadMask(this.geneSelectionEditor.getEl(), {
							msg: "Loading genes ..."
						});
		this.geneSelectionEditor.loadMask.show();
		Ext.apply(this.geneSelectionEditor, {
			geneGroupId: this.geneGroupId,
			selectedGeneGroup: this.geneCombo.selectedGeneGroup, 
			groupName: (this.geneCombo.selectedGeneGroup)? this.geneCombo.selectedGeneGroup.name : null,
			taxonId: this.getTaxonId(),
			taxonName: this.getTaxonName()
		});
		this.geneSelectionEditor.loadGenes(this.geneIds, function(){
			Ext.getCmp('geneSelectionEditor').loadMask.hide();
		});
	},
	launchExperimentSelectionEditor: function(){
				
		this.getEl().mask();

		this.experimentSelectionEditorWindow.show();

		this.experimentSelectionEditor.loadMask = new Ext.LoadMask(this.experimentSelectionEditor.getEl(), {
							msg: "Loading experiments ..."
						});
		this.experimentSelectionEditor.loadMask.show();
		Ext.apply(this.experimentSelectionEditor, {
			experimentGroupId: this.experimentGroupId,
			selectedExperimentGroup: this.experimentCombo.getExpressionExperimentGroup(), 
			groupName: this.experimentCombo.getExpressionExperimentGroup().name
		});
		this.experimentSelectionEditor.loadExperiments(this.experimentIds, 
				function(){Ext.getCmp('experimentSelectionEditor').loadMask.hide();});
	},
	/**
	 * Show the selected eeset members 
	 */
	loadExperimentOrGroup : function(record, query) {
				
				var id = record.get("id");
				var isGroup = record.get("isGroup");
				var type = record.get("type");
				
				var taxonId = record.get("taxonId");
				this.setTaxonId(taxonId);
				var taxonName = record.get("taxonName");
				this.setTaxonName(taxonName);
				this.geneCombo.setTaxonId(taxonId);
								
				// load preview of group if group was selected
				if (isGroup) {
					eeIds = record.get('memberIds');
					if(!eeIds || eeIds === null || eeIds.length === 0){
						return;
					}
					this.loadExperiments(eeIds);
					
				}
				//load single experiment if experiment was selected
				else {
					this.experimentIds = [id];
					// reset the experiment preview panel content
					this.resetExperimentPreview();
					
					// update the gene preview panel content
					this.experimentPreviewContent.update({
						shortName: record.get("name"),
						name: record.get("description"),
						id: record.get("id"),
						taxon: record.get("taxonName")
					});
					this.experimentSelectionEditorBtn.setText('0 more');
					this.experimentSelectionEditorBtn.disable();
					this.experimentSelectionEditorBtn.show();
				}
			},
						
			/**
			 * update the contents of the gene preview box and the this.geneIds value using a list of gene Ids
			 * @param geneIds an array of geneIds to use
			 */
			loadExperiments : function(ids) {
						
				//store selected ids for searching
				this.experimentIds = ids;
								
				this.loadExperimentPreview();
								
			},
									
			/**
			 * update the contents of the epxeriment preview box using the this.experimentIds value
			 * @param geneIds an array of geneIds to use
			 */
			loadExperimentPreview : function() {
				
				this.maskExperimentPreview();
						
				//store selected ids for searching
				ids = this.experimentIds;
				
				if(!ids || ids === null || ids.length === 0){
					this.resetExperimentPreview();
					return;
				}
								
				// reset the experiment preview panel content
				this.resetExperimentPreview();
								
				// load some experiments for previewing
				var limit = (ids.size() < this.PREVIEW_SIZE) ? ids.size() : this.PREVIEW_SIZE;
				var idsToPreview = [];
				for (var i = 0; i < limit; i++) {
					idsToPreview[i]=ids[i];
				}
				ExpressionExperimentController.loadExpressionExperiments(idsToPreview,function(ees){
									
					for (var j = 0; j < ees.size(); j++) {
						this.experimentPreviewContent.update(ees[j]);
					}
					this.experimentSelectionEditorBtn.setText('<a>'+(ids.size() - limit) + ' more - Edit</a>');
					this.showExperimentPreview();
					
					if (ids.size() === 1) {
						this.experimentSelectionEditorBtn.setText('0 more');
						this.experimentSelectionEditorBtn.disable();
						this.experimentSelectionEditorBtn.show();
					}
				
					
							
				}.createDelegate(this));
								
			},
			
		loadGeneOrGroup : function(record, query) {
				var id = record.get("id");
				var sessionId = record.get("sessionId");
				var isGroup = record.get("isGroup");
				var type = record.get("type");
				var size = record.get("size");
				var taxonId = this.getTaxonId();
				var geneIds = [];
								
				// load preview of group if group was selected
				if (isGroup) {
					
						if (type === "GOgroup") {
							// if no taxon has been selected, warn user that this won't work
							if (!this.getTaxonId() || isNaN(this.getTaxonId())) {
								Ext.Msg.alert("Error", "You must select a taxon before selecting a GO group.");
								return;
							}
						}
						
						geneIds = record.get('memberIds');
						if(geneIds === null || geneIds.length === 0){
							return;
						}
						this.loadGenes(geneIds);
						this.geneGroupId = id;
								
				}
				//load single gene if gene was selected
				else {
					this.geneIds = [id];
					
					this.geneGroupId = null;
					
					// reset the gene preview panel content
					this.resetGenePreview();
					
					// update the gene preview panel content
					this.genePreviewContent.update({
						officialSymbol: record.get("name"),
						officialName: record.get("description"),
						id: record.get("id"),
						taxonCommonName: record.get("taxonName")
					});
					this.geneSelectionEditorBtn.setText('0 more');
					this.geneSelectionEditorBtn.disable();
					this.geneSelectionEditorBtn.show();
				}
			},
			
			/**
			 * update the contents of the gene preview box and the this.geneIds value using a list of gene Ids
			 * @param geneIds an array of geneIds to use
			 */
			loadGenes : function(ids) {
				
				this.maskGenePreview();
				
				// store the ids
				this.geneIds = ids;
				// load the preview
				this.loadGenePreview();
						
			},
			
			/**
			 * update the contents of the gene preview box with the this.geneIds value
			 * @param geneIds an array of geneIds to use
			 */
			loadGenePreview : function() {
				
				this.maskGenePreview();
				
				// grab ids to use
				ids = this.geneIds;
				
				// load some genes to display 
				var limit = (ids.size() < this.PREVIEW_SIZE) ? ids.size() : this.PREVIEW_SIZE;
				var previewIds = ids.slice(0,limit);
				GenePickerController.getGenes(previewIds, function(genes){
						
							// reset the gene preview panel content
							this.resetGenePreview();
							for (var i = 0; i < genes.size(); i++) {
								this.genePreviewContent.update(genes[i]);
							}
							this.geneSelectionEditorBtn.setText('<a>'+(ids.size() - limit) + ' more - Edit</a>');
							this.showGenePreview();
							
							if (ids.size() === 1) {
								this.geneSelectionEditorBtn.setText('0 more');
								this.geneSelectionEditorBtn.disable();
								this.geneSelectionEditorBtn.show();
							}
							
						}.createDelegate(this));
						
			},
			
			/**
			 * Given text, search Gemma for matching genes. Used to 'bulk load' genes from the GUI.
			 * 
			 * @param {} e
			 */
			getGenesFromList : function(e, taxonId) {

				if (!taxonId && this.getTaxonId()) {
					taxonId = this.getTaxonId();
				}
				
				if(isNaN(taxonId)){
					Ext.Msg.alert("Missing information", "Please select an experiment or experiment group first.");
					return;
				}

				var loadMask = new Ext.LoadMask(this.getEl(), {
							msg : "Loading genes..."
						});
				loadMask.show();
				var text = e.geneNames;
				GenePickerController.searchMultipleGenesGetMap(text, taxonId, {

							callback : function(queryToGenes) {
								var i;
								var geneData = [];
								var warned = false;
								if (i >= Gemma.MAX_GENES_PER_QUERY) {
									if (!warned) {
										Ext.Msg.alert("Too many genes", "You can only search up to " +
														Gemma.MAX_GENES_PER_QUERY +
														" genes, some of your selections will be ignored.");
											warned = true;
										}
								}
																	
								this.maskGenePreview();
								
								var geneIds = [];
								var genesToPreview = [];
								var queriesWithMoreThanOneResult = [];
								var queriesWithNoResults = [];
								var query; 
								
								// for each query
								for (query in queryToGenes) {
									var genes = queryToGenes[query];
									// for each result of that query
									
									// if a query matched more than one result, store for notifying user
									if(genes.length>1){queriesWithMoreThanOneResult.push(query);}
									
									// if a query matched no results, store for notifying user
									if(genes.length===0){queriesWithNoResults.push(query);}
									
									for(i = 0; i<genes.length; i++){
										// store some genes for previewing 
										if (genesToPreview.length < this.PREVIEW_SIZE) {
											genesToPreview.push(genes[i]);
										}
										// store all ids
										geneIds.push(genes[i].id);
									}
								}
								
								// reset the gene preview panel content
								this.resetGenePreview();
								
								for ( i=0; i<genesToPreview.length;i++) {
									this.genePreviewContent.update(genesToPreview[i]);
								}
								
								this.geneSelectionEditorBtn.setText('<a>'+(geneIds.length-genesToPreview.length) + ' more - Edit</a>');
								this.showGenePreview();

								this.geneIds=geneIds;								

								var msgMany =""; var msgNone ="";
								// if some genes weren't found or some gene matches were inexact, tell the user
								if(queriesWithMoreThanOneResult.length>0){
									msgMany = queriesWithMoreThanOneResult.length+((queriesWithMoreThanOneResult.length===1)?" query":" queries")+"  returned more than one gene, all were added to the results: ";
									// for each query
									query = '';
									for ( i = 0; i< queriesWithMoreThanOneResult.length; i++) {
										query = queriesWithMoreThanOneResult[i];
										msgMany += "<br> - \""+query+"\" matched: ";
										genes = queryToGenes[query];
										// for each result of that query
										for(var j = 0; j<genes.length && j<10; j++){
											msgMany += genes[j].officialSymbol;
											msgMany += (j+1<genes.length)? ", ":".";
										}
										if(genes.length>10){msgMany += "...("+genes.length-20+" more)";}
									}
									msgMany+= '<br><br>';
								}
								if(queriesWithNoResults.length>0){
									msgNone = queriesWithNoResults.length+((queriesWithNoResults.length===1)?" query":" queries")+" didn't match any genes in Gemma:<br>";
									// for each query									
									query = '';
									for ( i = 0; i< queriesWithNoResults.length; i++) {
										query = queriesWithNoResults[i];
										msgNone += " - "+query+"<br>";
									}
								}
								if(queriesWithMoreThanOneResult.length>0 || queriesWithNoResults.length>0){
									Ext.Msg.alert("Query Result Details","Please note:<br><br>"+msgMany+msgNone);
								}
								loadMask.hide();

							}.createDelegate(this),

							errorHandler : function(e) {
								this.getEl().unmask();
								Ext.Msg.alert('There was an error', e);
							}
						});
				this.geneSelectionEditorBtn.show();
			}
});
// this makes the text in the button change colour on hover, like a normal link
Ext.LinkButton = Ext.extend(Ext.Button, {
    template: new Ext.Template(
        '<table cellspacing="0" class="x-btn {3}"><tbody class="{4}">',
        '<tr><td class="x-btn-tl"><i>&#160;</i></td><td class="x-btn-tc"></td><td class="x-btn-tr"><i>&#160;</i></td></tr>',
        '<tr><td class="x-btn-ml"><i>&#160;</i></td><td class="x-btn-mc"><em class="{5}" unselectable="on"><a href="{6}" target="{7}" class="x-btn-text {2}"><button>{0}</button></a></em></td><td class="x-btn-mr"><i>&#160;</i></td></tr>',
        '<tr><td class="x-btn-bl"><i>&#160;</i></td><td class="x-btn-bc"></td><td class="x-btn-br"><i>&#160;</i></td></tr>',
        '</tbody></table>').compile(),
    buttonSelector : 'a:first',
	ctCls: "transparent-btn" // no button image
});
