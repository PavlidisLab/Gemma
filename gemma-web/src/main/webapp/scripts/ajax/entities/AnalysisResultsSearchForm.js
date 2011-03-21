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
		
	PREVIEW_SIZE : 3,
	
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
	activeEeIds : [],

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
		//console.log("in do search");
		
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
					taxonId : this.getTaxon().id
				});

		if (this.currentSet) {
			newCsc.eeIds = this.getActiveEeIds();
			newCsc.eeSetName = this.currentSet.get("name");
			newCsc.eeSetId = this.currentSet.get("id");
			newCsc.dirty = this.currentSet.dirty; // modified without save
		}
		return newCsc;
	},

	doCoexpressionSearch : function(csc) {
		//console.log("in do search");
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
		if (typeof pageTracker != 'undefined') {
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
		url += String.format("?g={0}&s={1}&t={2}", csc.geneIds.join(","), csc.stringency, csc.taxonId);
		if (csc.queryGenesOnly) {
			url += "&q";
		}

		if (csc.eeSetId >= 0)
			url += String.format("&a={0}", csc.eeSetId);

		if (csc.eeSetName)
			url += String.format("&an={0}", csc.eeSetName);

		if (csc.dirty) {
			url += "&dirty=1";
		}

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
					taxonId : this.getTaxon().id
				});

		if (this.currentSet) {
			newDsc.eeIds = this.getActiveEeIds();
			newDsc.eeSetName = this.currentSet.get("name");
			newDsc.eeSetId = this.currentSet.get("id");
			newDsc.dirty = this.currentSet.dirty; // modified without save
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
		if (!this.currentSet) {
			Ext.Msg.alert("Warning", "You must select an expression experiment set before choosing factors. Scope must be specified.");
		} else if (this.getActiveEeIds().length == 0) {
			Ext.Msg.alert("Warning", "You should select at least one experiment to analyze");
		} else {
			var eeIds = this.getActiveEeIds();
			this.efChooserPanel.show(eeIds);
		}
	},
	doDifferentialExpressionSearch : function(dsc) {
		if ((dsc && !dsc.selectedFactors) || (!dsc && !this.efChooserPanel.eeFactorsMap)) {
			this.efChooserPanel.on("factors-chosen", function(efmap){
				this.doDifferentialExpressionSearch();
			}, this, {
				single: true
			});
			this.chooseFactors();
		}
		else {
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
			if (typeof pageTracker != 'undefined') {
				pageTracker._trackPageview("/Gemma/differentialExpressionSearch.doSearch");
			}
		}
	},
	getDiffExBookmarkableLink : function(dsc) {
		if (!dsc) {
			dsc = this.getDiffSearchCommand();
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		url += String.format("?g={0}&thres={1}&t={2}", dsc.geneIds.join(","), dsc.threshold, dsc.taxonId);

		if (dsc.eeSetId >= 0) {
			url += String.format("&a={0}", dsc.eeSetId);
		}

		// Makes bookmarkable links somewhat unusable (too long)
		// if (dsc.eeIds) {
		// url += String.format("&ees={0}", dsc.eeIds.join(","));
		// }

		if (dsc.eeSetName) {
			url += String.format("&setName={0}", dsc.eeSetName);
		}

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
		//console.log("in return from diffSearch");
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);
		this.fireEvent('showDiffExResults', this, result);
	},

	getActiveEeIds : function() {
		if(this.activeEeIds){
			return this.activeEeIds; 
		}
		return [];
	},
	
	initComponent : function() {
		
		/**get components**/
		

		/****** EE COMBO ****************************************************************************/
		
		// Shows the combo box for EE groups 
		this.experimentCombo = new Gemma.ExperimentAndExperimentGroupCombo({
							typeAhead: false,
							width : 210,
							
						});
		this.experimentCombo.on('select', function(combo, record, index) {
										
										// if the EE has changed taxon, reset the gene combo
										this.taxonChanged(record.get("taxon"));
										
										// store the eeid(s) selected and load some EE into the previewer
										// store the taxon associated with selection
										var query = combo.lastQuery;
										this.loadExperimentOrGroup(record, query);
										
										// once an experiment has been selected, cue the user that the gene select is now active
										Ext.get('visGeneGroupCombo').setStyle('background','white');
										this.currentSet=record;
										
									},this);

		/****** EE PREVIEW **************************************************************************/
			
		this.datasetMembersDataView = new Gemma.ExpressionExperimentDataView({
			id:'datasetMembersDataView'
		});

		this.experimentPreview = new Ext.Panel({
				width:210,
				id:'experimentPreview',
				html:'<div style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview</div>',
				tpl: new Ext.XTemplate(
				'<tpl for="."><div style="padding-bottom:7px;"><a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
				'{id}"',
				' ext:qtip="{shortName}">{shortName}</a>&nbsp; {name}</div></tpl>'),
				tplWriteMode: 'append',
				frame:true
		});		
		
		/****** GENE COMBO ******************************************************************************/
		/*this.geneCombo = new Gemma.MyEXTGeneAndGeneGroupCombo;*/
		this.geneCombo = new Gemma.GeneAndGeneGroupCombo({
					id : "visGeneGroupCombo",
					width:187,
					hideTrigger: false,
					typeAhead: false,
					style:'background:Gainsboro;',
					//editable : false, //true to enable search of combo values
					listeners : {
						'select' : {
							fn : function(combo, record, index) {
								var query = combo.lastQuery;
								this.loadGeneOrGroup(record, query);
							},
							scope : this
						},
						'focus' : {
							fn : function(cb, rec, index) {
								if(!this.currentSet){
									Ext.Msg.alert("Missing info","Please first select an experiment or experiment group.");
								}
							},
							scope:this
						}
					}
				});
				
			/*
//add listeners here (load,loadexception,add,beforeload,clear,datachanged,remove,update)	
		this.experimentCombo.getStore().on('beforeload',function(options){console.log('Store listener fired beforeload, arguments=',options);});
		this.experimentCombo.getStore().on('load',function(){console.log('Store listener fired load, arguments=',arguments);});
		this.experimentCombo.getStore().on('loadexception',function(misc){console.log('Store listener fired exception, arguments=',misc);});
		this.experimentCombo.getStore().on('exception',function(misc){console.log('Store listener fired exception, arguments=',misc);});
		this.experimentCombo.getStore().on('add',function(store, records, index){console.log('Store listener fired add, arguments=',records);});
		this.experimentCombo.on('focus',function(field){console.log('Combo listener fired focus, arguments=',arguments);});
		this.experimentCombo.on('blur',function(field){console.log('Combo listener fired blur, arguments=',arguments);});
		this.experimentCombo.on('collapse',function(combo){console.log('Combo listener fired collapse, arguments=',arguments);});
		this.experimentCombo.on('select',function(combo,record,index){console.log('Combo listener fired select, arguments=',arguments);});
		this.geneCombo.on('change',function(field,newVal,oldVal){console.log('Combo listener fired change, arguments=',arguments);});
		this.experimentCombo.on('beforequery',function(queryEvent){console.log('Combo listener fired beforequery, arguments=',arguments);});
*/
				
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

							if(!this.getTaxon() || isNaN(this.getTaxon().id)){
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
			console.log('geneListModified event received, newGeneIds: '+newGeneIds+" name:"+groupName);
			//this.geneSelectionEditorWindow.hide();
			if(newGeneIds){
				// geneids are passed for testing
				this.loadGenes(newGeneIds);
			} if(groupName){
				this.geneCombo.setRawValue(groupName);
				
				//this.geneCombo.setValue(groupName); // this should select the new group?
				
				// if groupId is passed, that means group has been created or updated and genes should be loaded from there
				// by selecting the group with the gene combo
			}
		},this);
		this.geneSelectionEditor.on('doneModification', function(){
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
		 
		/****** GENE PREVIEW ****************************************************************************/
		
		//use this.genePreviewContent.update("one line of gene text"); to write to this panel
		this.genePreviewContent = new Ext.Panel({
				//height:100,
				width:207,
				id:'genePreview',
				html:'<div style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview</div>',
				tpl: new Ext.Template('<div style="padding-bottom:7px;"><a href="/Gemma/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName}</div>'),
				tplWriteMode: 'append', // use this to append to content when calling update instead of replacing
				//tplWriteMode: 'overwrite',
				//frame:true,
				//items:this.geneSelectionEditorBtn,
		});
		this.genePreview = new Ext.Panel({
				//height:100,
				width:219,
				//layout:'hbox',
				//id:'genePreview',
				frame:true,
				items:[this.genePreviewContent,this.geneSelectionEditorBtn],
		});
		
		/******* BUTTONS ********************************************************************************/

		this.coexToggle = new Ext.Button({
							text: "<span style=\"font-size:1.3em\">Coexpression</span>",
							style:'padding-bottom:0.4em',
							scale: 'medium',
							width:150,
							enableToggle:true,
							pressed:true,
						});
		this.diffExToggle = new Ext.Button({
							text: "<span style=\"font-size:1.3em\">Differential Expression</span>",
							style:'padding-bottom:0.4em',
							scale: 'medium',
							width:150,
							enableToggle:true,
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
					/*{
						rowspan: 5,
						items: [{html:"<br><br>",border:false},new Ext.Button({
							text: "<span style=\"font-size:1.1em\">Go</span>",
							handler: this.doSearch.createDelegate(this, [], false),
							style:'padding-bottom:0.4em; padding-top:0.8em',
							//scale: 'medium',
							width:35,
						})]
					},*/
			
			
					{html:'Search for ', style:'text-align:center;vertical-align:middle;font-size:1.7em;', rowspan:1},
					{html: ' in ', style:'text-align:center;vertical-align:middle;font-size:1.7em;', rowspan:1},
					this.experimentCombo,
					{html: ' based on ', style:'text-align:center;vertical-align:middle;font-size:1.7em;', rowspan:1},
					this.geneCombo,this.symbolListButton, //,{border:false,html:'<a style="text-align:right">Paste symbol list</a>'}
					new Ext.Button({
							text: "<span style=\"font-size:1.1em\">Go!</span>",
							handler: this.doSearch.createDelegate(this, [], false),
							width:35,
						}),
					
					{},{},this.experimentPreview,{},{items: this.genePreview,colspan: 2},{},
					
					{
						//items: [this.geneSelectionEditorBtn],
						colspan: 6
					} 
					
					],
		});
		
		/* factor chooser for differential expression*/
		this.efChooserPanel = new Gemma.ExperimentalFactorChooserPanel({
					modal : true
				});
		
		Gemma.AnalysisResultsSearchForm.superclass.initComponent.call(this);
		
		this.addEvents('beforesearch', 'aftersearch','showDiffExResults','showCoexResults');		

		/*this.on('afterrender', function() {
					Ext.apply(this, {
								loadMask : new Ext.LoadMask(this.getEl(), {
											msg : "Preparing Search Interface  ..."
										})
							});

					this.loadMask.show();
				});
	*/
		this.doLayout();
		
	},
	
	getTaxon: function(){
		return this.taxon;
	},
	setTaxon: function(taxon){
		this.taxon = taxon;
	},

	/**
	 * Check if the taxon needs to be changed, and if so, update the geneAndGroupCombo and reset the gene preivew
	 * @param {} taxon
	 */
	taxonChanged : function(taxon) {

		// if the 'new' taxon is the same as the 'old' taxon for the experiment combo, don't do anything
		if (taxon && this.getTaxon() && (this.getTaxon().id === taxon.id) ) {
			return;
		}
		// if the 'new' and 'old' taxa are different, reset the gene preview and filter the geneCombo
		else if(taxon){
			this.resetGenePreview();
			this.geneCombo.setTaxon(taxon);
			this.setTaxon(taxon);
			this.geneCombo.reset();
		}

		this.fireEvent("taxonchanged", taxon);
	},
	collapsePreviews: function(){
		Ext.DomHelper.overwrite(Ext.getCmp('genePreview').body, {cn: '<span style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview </span><span align:"right"><a style="float:right">View</a></span>'});
		Ext.DomHelper.overwrite(Ext.getCmp('experimentPreview').body, {cn: '<span style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview </span><span align:"right"><a style="float:right">View</a></span>'});
	},
	resetGenePreview: function(){
		Ext.DomHelper.overwrite(Ext.getCmp('genePreview').body, {cn: '<div style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview </div>'});
//		this.genePreview.add(this.geneSelectionEditorBtn);
		this.geneSelectionEditorBtn.disable();
		this.geneSelectionEditorBtn.hide();
//		this.genePreview.doLayout();
	},
	resetExperimentPreview: function(){
		Ext.DomHelper.overwrite(Ext.getCmp('experimentPreview').body, {cn: '<div style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview </div>'});
	},
	showGenePreview: function(){
		this.genePreview.loadMask.hide();
		//this.genePreview.add(this.geneSelectionEditorBtn);
		//this.genePreview.doLayout();
		this.geneSelectionEditorBtn.enable();
		this.geneSelectionEditorBtn.show();
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
		
		var gids = this.geneIds;
		
		if(!this.geneIds || gids.length == 0){
			gids = [57412,57347,57397,57435]; // FOR TESTING
		}

		this.geneSelectionEditorWindow = new Ext.Window({
			closeAction: 'hide',
			closable: 'false',
			layout: 'fit',
			items: this.geneSelectionEditor,
			title: 'Edit Your Gene Selection'
		});
		this.geneSelectionEditorWindow.show();

		this.geneSelectionEditor.loadMask = new Ext.LoadMask(this.geneSelectionEditor.getEl(), {
							msg: "Loading genes ..."
						});
		this.geneSelectionEditor.loadMask.show();
		var newGroupName = 'Edited \"'+this.geneCombo.selectedGeneGroup.name+'\" group';
		Ext.apply(this.geneSelectionEditor, {geneGroupId: this.geneGroupId, newGroupName: newGroupName});
		this.geneSelectionEditor.loadGenes(gids, function(){Ext.getCmp('geneSelectionEditor').loadMask.hide();});
	},
	/**
	 * Show the selected eeset members 
	 */
	loadExperimentOrGroup : function(record, query, callback, args) {
			//console.log("in loadExperimentOrGroup, record:"+record+", args:"+args);
				
				var id = record.get("id");
				var isGroup = record.get("isGroup");
				var type = record.get("type");
				
				var taxon = record.get("taxon");
				this.setTaxon(taxon);
				this.geneCombo.setTaxon(taxon);
								
				// load preview of group if group was selected
				if (isGroup) {
					if (type == "experimentSet" || type == "usersExperimentSet") {
						// get number of experiments in group
						var groupSize = record.get("size");
						
						this.maskExperimentPreview();
						// get the group's member ids to store for searching and load a few for previewing
						ExpressionExperimentSetController.getExperimentIdsInSet(id, function(ids){
						
							//store selected ids for searching
							this.activeEeIds = ids;
							
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
									Ext.getCmp('experimentPreview').update(ees[j]);
								}
								Ext.DomHelper.append(Ext.getCmp('experimentPreview').body, {
									cn: '<div style="text-align:right"><a>' + (ids.size() - limit) + ' more<a></div>'
								});
								Ext.getCmp('experimentPreview').loadMask.hide();
							}.createDelegate(this));
							
							
						}.createDelegate(this));
					}
					else 
						if (type == "freeText") {
							// want to use all the experiments returned (both those returned individually and in groups) 

							// get number of experiments in group
						var groupSize = record.get("size");
						
						this.maskExperimentPreview();
						
						// search for the group's member ids to store for searching and load a few for previewing
						ExpressionExperimentController.searchExperimentsAndExperimentGroupsGetIds(query, taxon.id, function(ids){
						
								//store selected ids for searching
								this.activeEeIds = ids;
								
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
										Ext.getCmp('experimentPreview').update(ees[j]);
									}
									Ext.DomHelper.append(Ext.getCmp('experimentPreview').body, {
										cn: '<div style="text-align:right"><a>' + (ids.size() - limit) + ' more<a></div>'
									});
									Ext.getCmp('experimentPreview').loadMask.hide();
								}.createDelegate(this));
								
							
						}.createDelegate(this));
						}
				}
				//load single experiment if experiment was selected
				else {
					this.activeEeIds = [id];
					//console.log("id:"+id+", record:"+record+" name:"+record.get("name")+" desc"+record.get("description")+" taxon:"+record.get("taxon"));
					// reset the experiment preview panel content
					this.resetExperimentPreview();
					
					// update the gene preview panel content
					this.experimentPreview.update({
						shortName: record.get("name"),
						name: record.get("description")
					});
					Ext.DomHelper.append(Ext.getCmp('experimentPreview').body, {
						cn: '<div style="text-align:right"><a>0 more<a></div>'
					});
				}
			},
		loadGeneOrGroup : function(record, query, callback, args) {
				var id = record.get("id");
				var isGroup = record.get("isGroup");
				var type = record.get("type");
				var size = record.get("size");
				var taxon = this.getTaxon();
				var geneIds = [];
								
				// load preview of group if group was selected
				if (isGroup) {
										
					if (type == "geneSet" || type == "usersgeneSet" || type == "usersGeneSet" ) {
						// get number of genes in group
						var groupSize = record.get("size");
						this.geneGroupId = id;
						// get a few geneIds from the group
						// get just a few genes for the preview
						GeneSetController.getGenesInGroup(id, function(genes){
							if(genes == null){
								return
							}
							this.maskGenePreview();
							
							var geneIds = [];
							for (var i = 0; i < genes.length; i++) {
								geneIds.push(genes[i].id);
							}
							// reset the gene preview panel content
							this.resetGenePreview();
							
							var limit = (genes.size() < this.PREVIEW_SIZE) ? genes.size() : this.PREVIEW_SIZE;
							for (var i = 0; i < limit; i++) {
								this.genePreviewContent.update(genes[i]);
							}
							
							this.geneSelectionEditorBtn.setText('<a>'+(genes.size() - limit) + ' more - Edit</a>');
								
							this.showGenePreview();
							this.geneIds = geneIds;
							
							/*
							 * FIXME this can result in the same gene listed twice. This is taken care of at the server
							 * side but looks funny.
							 
							if (callback) {
								callback(args);
							}*/
						}.createDelegate(this));
					}else 
						if (type == "usersgeneSetSession") {
							
						 	geneIds = record.get('memberIds');
							if(geneIds == null || geneIds.length == 0){
								return;
							}
							this.loadGenes(geneIds);
							
						}
					else 
						if (type == "GOgroup") {
							// GO groups aren't persistent groups (they don't have Ids)
							this.geneGroupId = null;
							
							// get number of genes in group
							var groupSize = record.get("size");
							
							// get the gene set(s) that match(es) the go ID (should only be one)
							
							
							// if no taxon has been selected, warn user that this won't work
							if (!this.getTaxon() || isNaN(this.getTaxon().id)) {
								Ext.Msg.alert("Error", "You must select a taxon before selecting a GO group.");
								return;
							}
							
							this.maskGenePreview();
							
							GenePickerController.getGenesByGOId(record.get("name"), this.getTaxon().id, function(genes){
							
								if (genes != null) {
								
									// store gene Ids for use in the search later 
									this.geneIds = [];
									for (var i = 0; i < genes.length; i++) {
										this.geneIds.push(genes[i].id);
									}
									
									this.resetGenePreview();
									
									// update preview panel content
									var limit = (genes.size() < this.PREVIEW_SIZE) ? genes.size() : this.PREVIEW_SIZE;
									for (var i = 0; i < limit; i++) {
										this.genePreviewContent.update(genes[i]);
									}
									this.geneSelectionEditorBtn.setText('<a>'+(genes.size() - limit) + ' more - Edit</a>');
									this.showGenePreview();
								}
								
								
							}.createDelegate(this));
						}
						else 
							if (type == "freeText") {
								// want to use all the genes returned (both those returned individually and in groups) 
								
								this.geneGroupId = null;
								this.maskGenePreview();
								
								// search for the group's member ids to store for searching and load a few for previewing
								GenePickerController.searchGenesAndGeneGroupsGetIds(query, taxon.id, function(ids){
																	
									//store selected ids for searching
									this.geneIds = ids;
									
									// reset the gene preview panel content
									this.resetGenePreview();
									// load some genes for previewing
									var limit = (ids.size() < this.PREVIEW_SIZE) ? ids.size() : this.PREVIEW_SIZE;
									var idsToPreview = [];
									for (var i = 0; i < limit; i++) {
										idsToPreview[i] = ids[i];
									}
									this.geneSelectionEditorBtn.setText('<a>'+(ids.size() - limit) + ' more - Edit</a>');
									GenePickerController.getGenes(idsToPreview, function(genes){
										if(genes == null){
											return
										}
										for (var j = 0; j < genes.size(); ++j) {
											Ext.getCmp('genePreview').update(genes[j]);
										}
										
										this.showGenePreview();
									}.createDelegate(this));
									
								}.createDelegate(this));
							}
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
						officialName: record.get("description")
					});
					this.geneSelectionEditorBtn.setText('0 more');
					this.geneSelectionEditorBtn.disable();
				}
				this.geneSelectionEditorBtn.show();
			},
			
			/**
			 * update the contents of the gene preview box and the this.geneIds value using a list of gene Ids
			 * @param geneIds an array of geneIds to use
			 */
			loadGenes : function(ids) {
				// store the ids
				this.geneIds = ids;
				
				// load some genes to display 
				var limit = (ids.size() < this.PREVIEW_SIZE) ? ids.size() : this.PREVIEW_SIZE;
				var previewIds = ids.slice(0,limit);
				GenePickerController.getGenes(previewIds, function(genes){
						
							this.maskGenePreview();

							// reset the gene preview panel content
							this.resetGenePreview();
							for (var i = 0; i < genes.size(); i++) {
								this.genePreviewContent.update(genes[i]);
							}
							this.geneSelectionEditorBtn.setText('<a>'+(ids.size() - limit) + ' more - Edit</a>');
							this.showGenePreview();
							
						}.createDelegate(this));
						
			},
			
			loadGeneGroup : function(groupName) {
				var geneIds = [];
								
						// get a few geneIds from the group
						// get just a few genes for the preview
						GeneSetController.getGenesInGroup(id, function(genes){
						
							this.maskGenePreview();
							
							var geneIds = [];
							for (var i = 0; i < genes.length; i++) {
								geneIds.push(genes[i].id);
							}
							// reset the gene preview panel content
							this.resetGenePreview();
							
							var limit = (genes.size() < this.PREVIEW_SIZE) ? genes.size() : this.PREVIEW_SIZE;
							for (var i = 0; i < limit; i++) {
								this.genePreviewContent.update(genes[i]);
							}
							this.geneSelectionEditorBtn.setText('<a>'+(genes.size() - limit) + ' more - Edit</a>');
							this.showGenePreview();
							this.geneIds = geneIds;
							
						}.createDelegate(this));
						
			},
			
			/**
			 * Given text, search Gemma for matching genes. Used to 'bulk load' genes from the GUI.
			 * 
			 * @param {} e
			 */
			getGenesFromList : function(e, taxon) {

				var taxonId;
				if (!taxon && this.getTaxon()) {
					taxonId = this.getTaxon().id;
				}else{
					taxonId = taxon.id;
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
								
								// for each query
								for (var query in queryToGenes) {
									var genes = queryToGenes[query];
									// for each result of that query
									
									// if a query matched more than one result, store for notifying user
									if(genes.length>1){queriesWithMoreThanOneResult.push(query)}
									
									// if a query matched no results, store for notifying user
									if(genes.length==0){queriesWithNoResults.push(query)}
									
									for(var i = 0; i<genes.length; i++){
										// store some genes for previewing 
										if (genesToPreview.length < this.PREVIEW_SIZE) {
											genesToPreview.push(genes[i]);
										}
										// store all ids
										geneIds.push(genes[i].id)
									}
								}
								
								// reset the gene preview panel content
								this.resetGenePreview();
								
								for (var i=0; i<genesToPreview.length;i++) {
									this.genePreviewContent.update(genesToPreview[i]);
								}
								
								this.geneSelectionEditorBtn.setText('<a>'+(geneIds.length-genesToPreview.length) + ' more - Edit</a>');
								this.showGenePreview();

								this.geneIds=geneIds;								

								var msgMany =""; var msgNone ="";
								// if some genes weren't found or some gene matches were inexact, tell the user
								if(queriesWithMoreThanOneResult.length>0){
									msgMany = queriesWithMoreThanOneResult.length+((queriesWithMoreThanOneResult.length==1)?" query":" queries")+"  returned more than one gene, all were added to the results: ";
									// for each query
									var query = '';
									for (var i = 0; i< queriesWithMoreThanOneResult.length; i++) {
										query = queriesWithMoreThanOneResult[i];
										msgMany += "<br> - \""+query+"\" matched: ";
										var genes = queryToGenes[query];
										// for each result of that query
										for(var j = 0; j<genes.length; j++){
											msgMany += genes[j].officialSymbol;
											msgMany += (j+1<genes.length)? ", ":".";
										}
									}
									msgMany+= '<br><br>';
								}
								if(queriesWithNoResults.length>0){
									msgNone = queriesWithNoResults.length+((queriesWithNoResults.length==1)?" query":" queries")+" didn't match any genes in Gemma:<br>";
									// for each query									
									var query = '';
									for (var i = 0; i< queriesWithNoResults.length; i++) {
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
	ctCls: "transparent-btn", // no button image
});
