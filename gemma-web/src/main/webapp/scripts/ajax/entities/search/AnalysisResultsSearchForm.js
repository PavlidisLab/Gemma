
Ext.namespace('Gemma');
Gemma.MIN_STRINGENCY = 2;

// this is the value used for CLASSIC coexpression and
// diff expression searches
Gemma.MAX_GENES_PER_CLASSIC_DIFFEX_QUERY = 20; 

// max suggested number of elements to use for a diff ex viz query
Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY = 100;
Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY = 100;
Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY = 20; // this is a hard limit
Gemma.MAX_EXPERIMENTS_CO_DIFF_EX_VIZ_QUERY = 100000; // effectively no limit

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

	// defaults for coexpression
	/**
	 * @cfg
	 * DEFAULT_STRINGENCY for coexpression
	 */
	DEFAULT_STRINGENCY : 2,
	/**
	 * @cfg
	 */
	DEFAULT_forceProbeLevelSearch : false,
	/**
	 * @cfg
	 */
	DEFAULT_useMyDatasets : false,
	/**
	 * @cfg
	 */
	DEFAULT_queryGenesOnly : false,

	// defaults for differential expression
	// using Gemma.DEFAULT_THRESHOLD, Gemma.MIN_THRESHOLD, Gemma.MAX_THRESHOLD (defined elsewhere)

	geneIds : [],
	geneGroupId : null, // keep track of what gene group has been selected
	experimentIds : [],

	hidingExamples: false,

	//***************************************************************************
	// * * SEARCH **
	// **************************************************************************/

	/**
	 * get the recommended maximum number of genes to run the search with
	 * may be different for coexpression and differential expression
	 */
	getMaxNumGenes:function(){
		
		return (this.coexToggle.pressed)?Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY :
				(this.showClassicDiffExResults)? Gemma.MAX_GENES_PER_CLASSIC_DIFFEX_QUERY : 
					Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY;
	},
	/**
	 * get the recommended maximum number of experiments to run the search with
	 * may be different for coexpression and differential expression
	 */
	getMaxNumExperiments:function(){
		return (this.coexToggle.pressed)?Gemma.MAX_EXPERIMENTS_PER_CO_EX_VIZ_QUERY : Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY;	
	},
	/**
	 * @private
	 * check that there are some experiments and genes to run on
	 * if there are too many experiments or genes, warn the user and offer to trim
	 * 
	 * after optional trimming, call the search function (doSearch)
	 * 
	 * only called by "Go!" button and example query buttons 
	 * 
	 * @param {GeneSetValueObject[]} geneSetValueObjects
	 * @param {experimentSetValueObject[]} experimentSetValueObjects
	 * @return 
	 */
	validateSearch: function(geneSetValueObjects, experimentSetValueObjects){
		if (geneSetValueObjects.length === 0) {
			Ext.Msg.alert("Error", "Gene(s) must be selected before continuing.");
			return;
		}

		if (experimentSetValueObjects.length === 0) {
			Ext.Msg.alert("Error", "Experiment(s) must be selected before continuing.");
			return;
		}
		//get the total number of genes 
		var i; var vo;
		var geneCount = 0;
		for(i = 0; i< geneSetValueObjects.length; i++){
			vo = geneSetValueObjects[i];
			if(vo.geneIds){
				geneCount += vo.geneIds.length;
			}
		}
		//get the total number of experiments 
		var experimentCount = 0;
		for(i = 0; i< experimentSetValueObjects.length; i++){
			vo = experimentSetValueObjects[i];
			if(vo.expressionExperimentIds){
				experimentCount += vo.expressionExperimentIds.length;
			}
		}
		var stateText = "";
		var maxText = "";
		var maxNumGenes = this.getMaxNumGenes();
		var maxNumExperiments = this.getMaxNumExperiments();
		if(geneCount > maxNumGenes && experimentCount > maxNumExperiments ){
			stateText = geneCount + " genes and "+ experimentCount + " experiments";
			maxText = maxNumGenes + " genes and "+maxNumExperiments +" experiments";
		}
		else if(experimentCount > maxNumExperiments){
			stateText = experimentCount + " experiments";
			maxText = maxNumExperiments +" experiments";
		}
		else if(geneCount > maxNumGenes){
			stateText = geneCount + " genes";
			maxText = maxNumGenes + " genes";
		}
		
		// coex uses a hard limit so trimming isn't optional and is handled elsewhere
		if( (!this.coexToggle.pressed && !this.showClassicDiffExResults) && ( geneCount > maxNumGenes  || experimentCount > maxNumExperiments ) ){
			this.getEl().mask();
			var warningWindow = new Ext.Window({
				width:450,
				height:200,
				closable:false,
				bodyStyle:'padding:7px;background: white; font-size:1.1em',
				title: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.trimmingWarningTitle,
				html: String.format(Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.trimmingWarningText, stateText, maxText),
				//icon: Ext.Msg.WARNING,
				buttons: [{
					text: 'Trim',
					tooltip:'Your query will be trimmed to '+maxText,
					handler: function(){
						if(geneCount > this.getMaxNumGenes()){
							geneSetValueObjects = this.trimGeneValObjs(geneSetValueObjects, this.getMaxNumGenes());
						}
						if(experimentCount > this.getMaxNumExperiments() ){
							experimentSetValueObjects = this.trimExperimentValObjs(experimentSetValueObjects, this.getMaxNumExperiments());
						}
						this.fireEvent('beforesearch', this);
						this.doSearch( geneSetValueObjects, experimentSetValueObjects );
						warningWindow.close();
						return;
					},
					scope: this
				}, {
					text: 'Don\'t trim',
					tooltip:'Continue with your search as is',
					handler: function(){
						this.fireEvent('beforesearch', this);
						this.doSearch(geneSetValueObjects, experimentSetValueObjects);
						warningWindow.close();
						return;
					},
					scope:this
				}, {
					text: 'Cancel',
					handler: function(){
						warningWindow.close();
						this.getEl().unmask();
						return;
					},
					scope:this
				}]
			});
			warningWindow.show();
		}else{
			this.fireEvent('beforesearch', this);
			this.doSearch(geneSetValueObjects, experimentSetValueObjects);
			return;
		}
		
	},
	/**
	 * returns a subset of the param list of valueObjects, with one set potentially trimmed 
	 * @param {Object} valueObjects
	 * @param {Object} max
	 */
	trimGeneValObjs: function(valueObjects, max){
		var runningCount = 0;
		var i; var valObj;
		var trimmedValueObjects = [];
		for(i = 0; i< valueObjects.length; i++){
			valObj = valueObjects[i];
			if(valObj.geneIds && (runningCount+valObj.geneIds.length)<max){
				runningCount += valObj.geneIds.length;
				trimmedValueObjects.push(valObj);
			}else if(valObj.geneIds){
				var trimmedIds = valObj.geneIds.slice(0, (max - runningCount));
				// clone the object so you don't effect the original
				var trimmedValObj = Object.clone(valObj);
				trimmedValObj.geneIds = trimmedIds;
				trimmedValObj.id = null;
				trimmedValObj.name = "Trimmed " + valObj.name;
				trimmedValObj.description = "Trimmed " + valObj.name+" for search";
				trimmedValObj.modified = true;
				trimmedValueObjects.push(trimmedValObj);
				return trimmedValueObjects;
			}
		}
		return trimmedValueObjects;
	},
	/**
	 * returns a subset of the param list of valueObjects, with one set potentially trimmed 
	 * @param {Object} valueObjects
	 * @param {Object} max
	 */
	trimExperimentValObjs: function(valueObjects, max){
		var runningCount = 0;
		var i; var valObj;
		var trimmedValueObjects = [];
		for(i = 0; i< valueObjects.length; i++){
			valObj = valueObjects[i];
			if(valObj.expressionExperimentIds && (runningCount+valObj.expressionExperimentIds.length)<max){
				runningCount += valObj.expressionExperimentIds.length;
				trimmedValueObjects.push(valObj);
			}else if(valObj.expressionExperimentIds){
				var trimmedIds = valObj.expressionExperimentIds.slice(0, (max - runningCount));
				// clone the object so you don't effect the original
				var trimmedValObj = Object.clone(valObj);
				trimmedValObj.expressionExperimentIds = trimmedIds;
				trimmedValObj.id = null;
				trimmedValObj.name = "Trimmed " + valObj.name;
				trimmedValObj.description = "Trimmed " + valObj.name+" for search";
				trimmedValObj.modified = true;
				trimmedValueObjects.push(trimmedValObj);
				return trimmedValueObjects;
			}
		}
		return trimmedValueObjects;
	},

	/**
	 * @private
	 * Run the search with entity set value objects.<br>
	 * If any of the parameter sets have a null, undefined, or negative id 
	 * then they are not "saved" anywhere (in the db or in the session-bound set list).
	 * Before the search runs, these sets are "saved" to the session-bound list. 
	 * 
	 * @param {GeneSetValueObject[]} geneSetValueObjects
	 * @param {ExpressionExperimentSetValueObject[]} experimentSetValueObjects
	 */
	doSearch : function(geneSetValueObjects, experimentSetValueObjects) {		
		
		this.collapsePreviews();
		this.toggleHidingExamples();
		
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : Gemma.StatusText.Searching.analysisResults,
						msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
					});
		}
		this.loadMask.show();
		// reset flags marking if searches are done
		// only used if both searches are run at once
		this.doneCoex = false;
		this.doneDiffEx = false;

		// if using a GO group or 'all results' group for a search, register the geneSetValObj as a set in the session
		var geneSetValObjsToRegister = [];
		var geneSetValObjsAlreadyRegistered = [];
		var i;
		var gsvo;
		for (i = 0; i < geneSetValueObjects.length; i++) {
			gsvo = geneSetValueObjects[i];
			if (typeof gsvo !== 'undefined' && (gsvo.id === null || gsvo.id === -1)) {
				// addNonModificationBasedSessionBoundGroups() takes a
				// geneSetValueObject
				var gsvoClone = Object.clone(gsvo);	
				delete gsvoClone.memberIds;
				// no memberIds field in a geneSetValueObject 
				// but this object would have the field if it was a GO group object
				// (this is a short cut fix, a better fix would be to make a new GSVO from the fields)
				geneSetValObjsToRegister.push(gsvoClone);

			} else {
				geneSetValObjsAlreadyRegistered.push(gsvo);
			}
		}
		if (geneSetValObjsToRegister.length > 0) {
			geneSetValueObjects = geneSetValObjsAlreadyRegistered;
			this.waitingForGeneSessionGroupBinding = true;
			GeneSetController.addNonModificationBasedSessionBoundGroups(geneSetValObjsToRegister, function(geneSets) {
						// should be at least one geneset
						if (geneSets === null || geneSets.length === 0) {
							// TODO error message
							return;
						} else {
							for (j = 0; j < geneSets.length; j++) {
								geneSetValueObjects.push(geneSets[j]);
							}
						}
						this.waitingForGeneSessionGroupBinding = false;
						/*
						 * recurse so once all session-bound groups are made, search runs
						 */
						this.doSearch(geneSetValueObjects, experimentSetValueObjects);
						return;
					}.createDelegate(this));
			return;
		}
		var experimentGroupsToRegister = [];
		var experimentGroupsAlreadyRegistered = [];
		var esvo;
		for (i = 0; i < experimentSetValueObjects.length; i++) {
			esvo = experimentSetValueObjects[i];
			// if the group has a null value for id, then it hasn't been
			// created as a group in the database nor session
			if (typeof esvo !== 'undefined' && (esvo.id === -1 || esvo.id === null)) {
				// addNonModificationBasedSessionBoundGroups() takes an
				// experimentSetValueObject 	
				experimentGroupsToRegister.push(esvo);
			} else {
				experimentGroupsAlreadyRegistered.push(esvo);
			}
		}
		if (experimentGroupsToRegister.length > 0) {
			experimentSetValueObjects = experimentGroupsAlreadyRegistered;
			this.waitingForExperimentSessionGroupBinding = true;
			ExpressionExperimentSetController.addNonModificationBasedSessionBoundGroups(experimentGroupsToRegister,
					function(datasetSets) {
						// should be at least one datasetSet
						if (datasetSets === null || datasetSets.length === 0) {
							// TODO error message
							return;
						} else {
							for (j = 0; j < datasetSets.length; j++) {
								experimentSetValueObjects.push(datasetSets[j]);
							}
						}
						
						/*
						 * recurse so once all session-bound groups are made, search runs
						 */
						this.doSearch(geneSetValueObjects, experimentSetValueObjects); 
						return;
					}.createDelegate(this));
			return;
		}
		/* this is disabled
		if (this.diffExToggle.pressed && this.coexToggle.pressed) {
			this.doDifferentialExpressionSearch();
			this.efChooserPanel.on("factors-chosen", function() {
						this.doCoexpressionSearch();
					}, this);
		}*/
		// if differential expression button is depressed, do a differential
		// search
		if (this.diffExToggle.pressed) {
			if(this.showClassicDiffExResults){
				this.doDifferentialExpressionSearch();
			}else{
				var data = this.getDataForDiffVisualization(geneSetValueObjects, experimentSetValueObjects);
				this.fireEvent('showDiffExResults', this, null, data);
			}
			
			
		}

		// if coexpression button is depressed, do a coexpression search
		else if (this.coexToggle.pressed) {
			this.doCoexpressionSearch();
		}

	},

	//***************************************************************************
	// * * COEXPRESSION **
	// **************************************************************************/

	/**
	 * @private
	 * Construct the coexpression command object from the form, to be sent to
	 * the server.
	 * 
	 * @return newCsc
	 */
	getCoexpressionSearchCommand : function() {
		var newCsc = {};
		if (this.csc) {
			newCsc = this.csc;
		}

		Ext.apply(newCsc, {
					geneIds : this.getGeneIds(),
					// stringency : Ext.getCmp('stringencyfield').getValue(),
					stringency : this.DEFAULT_STRINGENCY,
					forceProbeLevelSearch : this.DEFAULT_forceProbeLevelSearch,
					useMyDatasets : this.DEFAULT_useMyDatasets,
					queryGenesOnly : this.DEFAULT_queryGenesOnly,
					taxonId : this.getTaxonId()
				});

		if (this.getExperimentIds().length > 0) {
			newCsc.eeIds = this.getExperimentIds();
			// only supply eeSetName and eeSetId if eeSet exists in db,
			// otherwise will cause error
			// doesn't look like this is needed, so not setting for simplicity
			newCsc.eeSetName = null;
			newCsc.eeSetId = null;

		}
		return newCsc;
	},
	/**
	 * @return csc
	 */
	getLastCoexpressionSearchCommand : function() {
		return this.lastCSC;
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
		if (!this.lastCSC) {
			return "No search to repeat";
		}
		
		this.clearError();
		Ext.apply(this.lastCSC, {
			stringency: stringency,
			forceProbeLevelSearch: probeLevel,
			queryGenesOnly: queryGenesOnly
		});
		this.doCoexpressionSearch(this.lastCSC);
		return "";
	},
/**
 * @private
 * @param {Object} csc
 */
	doCoexpressionSearch : function(csc) {
		if (!csc) {
			csc = this.getCoexpressionSearchCommand();
		}
		
		this.clearError();

		var msg = this.validateCoexSearch(csc);
		if (msg.length === 0) {
			this.loadMask.show();
			var errorHandler = this.handleError.createDelegate(this, [], true);
			this.restrictCoexSearch(csc);
			this.lastCSC = csc;
			ExtCoexpressionSearchController.doSearchQuick2(csc, {
						callback : this.returnFromCoexSearch.createDelegate(this),
						errorHandler : errorHandler
					});
		} else {
			this.handleError(msg);
		}
		if (typeof pageTracker !== 'undefined') {
			pageTracker._trackPageview("/Gemma/coexpressionSearch.doCoexpressionSearch");
		}
	},
	
	restrictCoexSearch : function(csc) {
		
		var k = 50;
		
		var numDatasets = csc.eeIds.length;
		
		var displayStringency = 2;
		var resultsStringency = 2;
		
		if (numDatasets > k){
		
			displayStringency = 2 + Math.round(numDatasets/k);
		
		}
		
		if (displayStringency > 20){
			displayStringency = 20;
		}
		
		if (displayStringency > 5){
			resultsStringency = displayStringency - Math.round(displayStringency/4);
		}
		
		
		/*
		if (displayStringency > 2){
			csc.stringency = displayStringency -1;
		}
		*/
		csc.displayStringency = displayStringency;	
		
		csc.stringency = resultsStringency;
		
		
		
		/*
		if (csc.geneIds.length>10){
			//make it 'my genes only'
			csc.queryGenesOnly = true;
		}
		
		if (!csc.queryGenesOnly && csc.eeIds.length>50){
		
			if (csc.eeIds.length>80){
				csc.stringency = 4;	
			}else if (csc.eeIds.length>50){
				csc.stringency = 3;
			}
		
		}
		*/
		
	},
	/**
	 * Do some more checks before running the coex search
	 * @private
	 * @param {Object} csc
	 */
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
		} else if (csc.geneIds.length > this.getMaxNumGenes()) {
			// if trying to search for more than the allowed limit of genes,
			// show error

			// prune the gene Ids
			csc.geneIds = csc.geneIds.slice(0, this.getMaxNumGenes());

			/*
			 * //update the previews var runningCount = 0; var i; var
			 * idsToRemove = []; for (i = 0; i <
			 * this.geneChoosers.items.items.length; i++) { var chooser =
			 * this.geneChoosers.items.items[i]; if (typeof chooser.geneIds !==
			 * 'undefined') {// if not a // blank combo if (runningCount +
			 * chooser.geneIds.length <= Gemma.MAX_GENES_PER_QUERY) {
			 * runningCount += chooser.geneIds.length; } else { if (runningCount >
			 * Gemma.MAX_GENES_PER_QUERY) { runningCount =
			 * Gemma.MAX_GENES_PER_QUERY; } //
			 * idsToRemove.push(chooser.getId()); chooser.geneIds =
			 * chooser.geneIds.slice(0, (Gemma.MAX_GENES_PER_QUERY -
			 * runningCount)); runningCount += chooser.geneIds.length;
			 * chooser.selectedGeneOrGroupRecord.geneIds = chooser.geneIds;
			 * chooser.selectedGeneOrGroupRecord.memberIds = chooser.geneIds;
			 * chooser.selectedGeneOrGroupRecord.type = 'usersgeneSetSession';
			 * chooser.selectedGeneOrGroupRecord.name = "Trimmed " +
			 * chooser.selectedGeneOrGroupRecord.name;
			 * chooser.geneCombo.setRawValue(chooser.selectedGeneOrGroupRecord.name);
			 * chooser.geneCombo.getStore().reload();
			 * 
			 * this.collapseGenePreviews(); chooser.loadGenePreview();
			 * chooser.genePreviewContent.expand(); } } } //for(idToRemove in
			 * idsToRemove){ // this.removeGeneChooser(idToRemove); //}
			 * 
			 * return "You can only search up to " + Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY + "
			 * genes. Please note that your list(s) of genes have been trimmed
			 * automatically. <br>"+ "Press 'Go' again to run the search with
			 * this trimmed list or re-enter your gene query(ies) and "+ "use
			 * the edit tool to manually trim your selection(s).";
			 */
			this.handleWarning("You can only perform a coexpression search with up to " + Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY +
					" genes. Please note that your list(s) of genes have been trimmed automatically.<br>");
			return "";

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
		url += String
				.format("?g={0}&s={1}&t={2}&ee={3}", csc.geneIds.join(","), csc.stringency, csc.taxonId, csc.eeIds);
		if (csc.queryGenesOnly) {
			url += "&q";
		}
		if (csc.dirty) {
			url += "&dirty=1";
		}
		url = url.replace("home", "searchCoexpression");

		return url;
	},

	//***************************************************************************
	// * * DIFFERENTIAL EXPRESSION **
	// **************************************************************************/
	/**
	 * Construct the differential command object from the form, to be sent to
	 * the server.
	 * 
	 * @private
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
					geneIds : this.getGeneIds(),
					selectedFactors : efMap,
					threshold : Gemma.DEFAULT_THRESHOLD,
					taxonId : this.getTaxonId()
				});

		if (this.getExperimentIds().length > 0) {
			newDsc.eeIds = this.getExperimentIds();
			// only supply eeSetName and eeSetId if eeSet exists in db,
			// otherwise will cause error
			// doesn't look like this is needed, so not setting for simplicity
			newDsc.eeSetName = null;
			newDsc.eeSetId = null;

		}
		return newDsc;

	},

	/**
	 * Show the user interface for choosing factors. This happens
	 * asynchronously, so listen for the factors-chosen event.
	 */
	chooseFactors : function() {
		if (this.getSelectedAsExperimentSetValueObjects().length <= 0) {
			Ext.Msg.alert("Warning",
					"You must select an expression experiment set before choosing factors. Scope must be specified.");
		} else if (this.getExperimentIds().length === 0) {
			Ext.Msg.alert("Warning", "You should select at least one experiment to analyze");
		} else {
			var eeIds = this.getExperimentIds();
			this.efChooserPanel.show(eeIds);
		}
	},
	// need to run chooseFactors() first!
	doDifferentialExpressionSearch : function(dsc) {

		this.clearError();
		if (!dsc) {
			dsc = this.getDiffSearchCommand();
		}
		var msg = this.validateDiffExSearch(dsc);
		if (msg.length !== 0) {
			this.handleError(msg);
			return;
		} else {
		
			this.chooseFactors();
			this.efChooserPanel.on("factors-chosen", function(efmap){
			
				dsc.selectedFactors = efmap;
				
				var errorHandler = this.handleError.createDelegate(this, [], true);
				
				DifferentialExpressionSearchController.getDiffExpressionForGenes(dsc, {
					callback: this.returnFromDiffExSearch.createDelegate(this),
					errorHandler: errorHandler
				});
				
				if (typeof pageTracker !== 'undefined') {
					pageTracker._trackPageview("/Gemma/differentialExpressionSearch.doSearch");
				}
			}, this, {
				single: true
			});
		}

	},

	/**
	 * 
	 * @param {}
	 *            dsc
	 * @return {}
	 */
	getDiffExBookmarkableLink : function(dsc) {
		if (!dsc) {
			dsc = this.getDiffSearchCommand();
		}
		var msg = this.validateDiffExSearch(dsc);
		if (msg.length !== 0) {
			this.handleError(msg);
			return;
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		url += String.format("?g={0}&thres={1}&t={2}", dsc.geneIds.join(","), dsc.threshold, dsc.taxonId);

		// Makes bookmarkable links somewhat unusable (too long)
		if (dsc.eeIds) {
			url += String.format("&ees={0}", dsc.eeIds.join(","));
		}

		// won't always have a set name or set id
		/*
		 * if (dsc.eeSetId >= 0) { url += String.format("&a={0}", dsc.eeSetId); }
		 * if (dsc.eeSetName) { url += String.format("&setName={0}",
		 * dsc.eeSetName); }
		 */

		if (dsc.selectedFactors) {
			url += "&fm=";
			var i;
			for (i in dsc.selectedFactors) {
				var o = dsc.selectedFactors[i];
				if (!o.eeId) {
					continue;
				}
				url += o.eeId + "." + o.efId + ",";
			}
		}

		url = url.replace("home", "diff/diffExpressionSearch");

		return url;
	},

	/**
	 * 
	 * @param {}
	 *            dsc
	 * @return {String}
	 */
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
		} else if (dsc.geneIds.length > Gemma.MAX_GENES_PER_CLASSIC_DIFFEX_QUERY) {
			// if trying to search for more than the allowed limit of genes,
			// show error

			// prune the gene Ids
			dsc.geneIds = dsc.geneIds.slice(0, Gemma.MAX_GENES_PER_CLASSIC_DIFFEX_QUERY);
			//this.loadGenes(dsc.geneIds); // TODO loadGenes isn't in this
			// class anymore

			this.handleWarning(String.format(Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.autoTrimmingText, 
												Gemma.MAX_GENES_PER_CLASSIC_DIFFEX_QUERY, 'genes'));
			return "";

		} else {
			return "";
		}
	},

	/** Shared methods * */
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

	getDataForDiffVisualization : function(geneSetValueObjects, experimentSetValueObjects) {
		var geneNames = [];
		var geneResultValueObjects = [];
		var i;
		if (geneSetValueObjects.length > 0) {
			for (i = 0; i < geneSetValueObjects.length; i++) {
				geneNames.push(geneSetValueObjects[i].name);
			}
		}
		var experimentNames = [];
		var experimentCount = 0;
		if (experimentSetValueObjects.length > 0) {
			for (i = 0; i < experimentSetValueObjects.length; i++) {
				experimentNames.push(experimentSetValueObjects[i].name);
				experimentCount += experimentSetValueObjects[i].expressionExperimentIds.size();
			}
		}
		var data = {
			experimentSetValueObjects: experimentSetValueObjects,
			geneSetValueObjects: geneSetValueObjects,
			geneSessionGroupQueries : this.getGeneSessionGroupQueries(),
			experimentSessionGroupQueries : this.getExperimentSessionGroupQueries(),
			geneNames : geneNames,
			datasetNames : experimentNames,
			taxonId : this.getTaxonId(),
			taxonName : this.getTaxonName(),
			pvalue : Gemma.DEFAULT_THRESHOLD,
			datasetCount : experimentCount,
			selectionsModified : this.wereSelectionsModified(),
			showTutorial: this.runningExampleQuery
		};
		return data;
	},

	returnFromCoexSearch : function(result) {
		this.doneCoex = true;
		// if both coex and diff ex searches were called, don't hide load mask
		// until both have returned
		if (this.diffExToggle.pressed && this.coexToggle.pressed) {
			if (!this.doneDiffEx) {
				return;
			} else {
				//var data = this.getDataForDiffVisualization();
				this.fireEvent('showDiffExResults', this, result, data);
			}
		}
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);
		this.fireEvent('showCoexResults', this, result, this.runningExampleQuery);
		var csc = this.lastCSC;
		/*
		 * take this out for now because of new coexSearchQuick2 call
		if((csc.stringency && csc.stringency !==  this.DEFAULT_STRINGENCY) ||
			(csc.forceProbeLevelSearch && csc.forceProbeLevelSearch !== this.DEFAULT_forceProbeLevelSearch) ||
			(csc.queryGenesOnly && csc.queryGenesOnly !== this.DEFAULT_queryGenesOnly)){
			this.fireEvent('showOptions', csc.stringency, csc.forceProbeLevelSearch, csc.queryGenesOnly);
		}
		*/
	},
	returnFromDiffExSearch : function(result) {
		this.doneDiffEx = true;
		// if both coex and diff ex searches were called, don't hide load mask
		// until both have returned
		if (this.diffExToggle.pressed && this.coexToggle.pressed) {
			if (!this.doneCoex) {
				return;
			} else {
				this.fireEvent('showCoexResults', this, result, this.runningExampleQuery);
			}
		}
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);

		var data;
		if(!this.showClassicDiffExResults){
			//data = this.getDataForDiffVisualization();
		}else{
			data = null;
		}
		this.fireEvent('showDiffExResults', this, result, data);
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
					style : 'padding-bottom: 10px',
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
								html : 'in:',//'in&hellip;',
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
					style: 'text-align:center;font-size:1.4em;padding:0px',
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
									var goName = "GO_0021766";
									//var goName = "GO_0045208";
									var eeSetId = '6112';
									var taxonId = '1';
									this.runExampleQuery(eeSetId, goName, taxonId);
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
									var goName = "GO_0021879";
									var eeSetId = '6110';
									var taxonId = '2';
									this.runExampleQuery(eeSetId, goName, taxonId);
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
									var goName = "GO_0051302";
									var eeSetId = '6115';
									var taxonId = '11';
									this.runExampleQuery(eeSetId, goName, taxonId);
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
//									var goName = "GO_0035418";
//									var eeSetId = '737';
//									var taxonId = '1';
//									this.runExampleQuery(eeSetId, goName, taxonId);
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
						items: [this.theseExperimentsPanel, this.experimentChoosers]
					}, {
						html: ' for ',
						style: 'white-space: nowrap;font-size:1.7em;padding-top: 32px;'
					}, {
						defaults: {
							border: false
						},
						items: [this.theseGenesPanel, this.geneChoosers]
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
									this.validateSearch(this.getSelectedAsGeneSetValueObjects(), this.getSelectedAsExperimentSetValueObjects());
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

		/* factor chooser for differential expression */
		this.efChooserPanel = new Gemma.ExperimentalFactorChooserPanel({
					modal : true
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
						// Ext.getCmp(this.getId()+'Button').enable();
						this.searchForm.addGeneChooser();
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
					// Ext.getCmp(this.getId()+'Button').enable();
					this.searchForm.addExperimentChooser();
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
		this.addExperimentChooser();
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
	addGOGeneSet: function( goName, taxonId ){

		// get the chooser to inject
		var chooser = this.geneChoosers.getComponent(0);
		this.addGeneChooser();
		var myscope = this;
					
		// make a gene combo record for the db-backed experimentSetValueObject
		GenePickerController.getGeneSetByGOId( goName, taxonId , function(geneSet){
			
			var record = new Gemma.GeneAndGeneGroupComboRecord({
				name : geneSet.name,
				description: geneSet.descrption,
				isGroup : true,
				size: geneSet.geneIds.length,
				taxonId : geneSet.taxonId,
				taxonName :geneSet.taxonName,
				memberIds : geneSet.geneIds,
				resultValueObject : geneSet,
				comboText: geneSet.name + ": " + geneSet.description,
				userOwned : false
			});
		
			// get the chooser's gene combo
			var geneCombo = chooser.geneCombo;
			
			// tell gene combo the GO group was selected
			geneCombo.fireEvent('select', geneCombo, record, 0);
			
			myscope.fireEvent('geneExampleReady');
		});
	},
	
	runExampleQuery: function(eeSetId, goName, taxonId){
		
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
		this.addGOGeneSet(goName, taxonId);
		
		var queryRun = false;
		var geneExampleReady = false;
		var eeExampleReady = false;
		this.on('geneExampleReady',function(){
			geneExampleReady = true;
			if(eeExampleReady && !queryRun){
				queryRun = true;
				this.runningExampleQuery = true;
				this.validateSearch(this.getSelectedAsGeneSetValueObjects(), this.getSelectedAsExperimentSetValueObjects());
			}
		});
				
		this.on('eeExampleReady',function(){
			eeExampleReady = true;
			if(geneExampleReady && !queryRun){
				queryRun = true;
				this.runningExampleQuery = true;
				this.validateSearch(this.getSelectedAsGeneSetValueObjects(), this.getSelectedAsExperimentSetValueObjects());
			}
		});
							
	}

});

Ext.reg('analysisResultsSearchForm', Gemma.AnalysisResultsSearchForm);
