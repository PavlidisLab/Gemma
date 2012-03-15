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
 * These methods are used to run a search from the AnalysisResultsSearchForm
 * 
 * It's an Ext.util.Observable so that it can fire events
 * (needed to keep form and UI in sync with steps of searching)
 *  
 * @author thea
 * @version $Id: AnalysisResultsSearchMethods.js,v 1.0 2012/01/12 04:02:25 thea
 *          Exp $
 */
Gemma.AnalysisResultsSearchMethods = Ext.extend(Ext.util.Observable, {

	taxonId: null,

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
 * 
 * @param {Object} form needed to throw events being listened for from page, TODO there's a better way to do this...
 * @param {Object} geneSetValueObjects
 * @param {Object} experimentSetValueObjects
 */
	searchCoexpression: function( geneSetValueObjects, experimentSetValueObjects ){
		this.runningCoex = true;
		this.runningDiffEx = false;
		this.geneSetValueObjects = geneSetValueObjects;
		this.experimentSetValueObjects = experimentSetValueObjects;
		this.validateSearch( geneSetValueObjects, experimentSetValueObjects );
	},
	/**
	 * 
	 * @param {Object} form needed to throw events being listened for from page, TODO there's a better way to do this...
	 * @param {Object} geneSetValueObjects
	 * @param {Object} experimentSetValueObjects
	 */
	searchDifferentialExpression: function( geneSetValueObjects, experimentSetValueObjects ){
		this.runningCoex = false;
		this.runningDiffEx = true;
		this.geneSetValueObjects = geneSetValueObjects;
		this.experimentSetValueObjects = experimentSetValueObjects;
		this.validateSearch( geneSetValueObjects, experimentSetValueObjects );
	},
	
	getGeneIds : function() {
		var geneIds = [];
		var i;
		var j;
		var selectedVOs = this.geneSetValueObjects;
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
	getExperimentIds : function() {
		var eeIds = [];
		var i;
		var j;
		var selectedVOs = this.experimentSetValueObjects;
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
			this.fireEvent('searchAborted');
			return;
		}

		if (experimentSetValueObjects.length === 0) {
			Ext.Msg.alert("Error", "Experiment(s) must be selected before continuing.");
			this.fireEvent('searchAborted');
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
		if( (!this.runningCoex && !this.showClassicDiffExResults) && ( geneCount > maxNumGenes  || experimentCount > maxNumExperiments ) ){
			Ext.getBody().mask();
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
						this.fireEvent('beforesearch');
						this.doSearch( geneSetValueObjects, experimentSetValueObjects );
						warningWindow.close();
						return;
					},
					scope: this
				}, {
					text: 'Don\'t trim',
					tooltip:'Continue with your search as is',
					handler: function(){
						this.fireEvent('beforesearch');
						this.doSearch(geneSetValueObjects, experimentSetValueObjects);
						warningWindow.close();
						return;
					},
					scope:this
				}, {
					text: 'Cancel',
					handler: function(){
						warningWindow.close();
						return;
					},
					scope:this
				}]
			});
			warningWindow.show();
			warningWindow.on('close', function(){
				Ext.getBody().unmask();
			});
			
		}else{
			this.fireEvent('beforesearch');
			this.doSearch(geneSetValueObjects, experimentSetValueObjects);
			return;
		}
		
	},
	
	/**
	 * get the recommended maximum number of genes to run the search with
	 * may be different for coexpression and differential expression
	 */
	getMaxNumGenes:function(){
		
		return (this.runningCoex)?Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY :
				(this.showClassicDiffExResults)? Gemma.MAX_GENES_PER_CLASSIC_DIFFEX_QUERY : 
					Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY;
	},
	/**
	 * get the recommended maximum number of experiments to run the search with
	 * may be different for coexpression and differential expression
	 */
	getMaxNumExperiments:function(){
		return (this.runningCoex)?Gemma.MAX_EXPERIMENTS_PER_CO_EX_VIZ_QUERY : Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY;	
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
				// addSessionGroups() takes a geneSetValueObject (and boolean for isModificationBased)
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
			GeneSetController.addSessionGroups(geneSetValObjsToRegister, false, function(geneSets) {
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
				// addSessionGroups() takes an experimentSetValueObject (and boolean for isModificationBased)

				experimentGroupsToRegister.push(esvo);
			} else {
				experimentGroupsAlreadyRegistered.push(esvo);
			}
		}
		if (experimentGroupsToRegister.length > 0) {
			experimentSetValueObjects = experimentGroupsAlreadyRegistered;
			this.waitingForExperimentSessionGroupBinding = true;
			ExpressionExperimentSetController.addSessionGroups(experimentGroupsToRegister, false,
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
		if (this.runningDiffEx && this.runningCoex) {
			this.doDifferentialExpressionSearch();
			this.efChooserPanel.on("factors-chosen", function() {
						this.doCoexpressionSearch();
					}, this);
		}*/
		// if differential expression button is depressed, do a differential
		// search
		if (this.runningDiffEx) {
			if(this.showClassicDiffExResults){
				this.doDifferentialExpressionSearch();
			}else{
				var data = this.getDataForDiffVisualization(geneSetValueObjects, experimentSetValueObjects);
				this.fireEvent('showDiffExResults', null, data);
			}
			
			
		}

		// if coexpression button is depressed, do a coexpression search
		else if (this.runningCoex) {
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
					taxonId : this.taxonId
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
 * @private
 * @param {Object} csc
 */
	doCoexpressionSearch : function(csc) {
		if (!csc) {
			csc = this.getCoexpressionSearchCommand();
		}
		
		this.fireEvent('clearError');
		
		var msg = this.validateCoexSearch(csc);
		if (msg.length === 0) {
			var errorHandler = function(msg, e){
				this.fireEvent('error', msg, e);	
			};
			this.restrictCoexSearch(csc);
			this.lastCSC = csc;
			ExtCoexpressionSearchController.doSearchQuick2(csc, {
						callback : this.returnFromCoexSearch.createDelegate(this),
						errorHandler : errorHandler.createDelegate(this)
					});
		} else {
			this.fireEvent('error', msg);
		}
		if (typeof pageTracker !== 'undefined') {
			pageTracker._trackPageview("/Gemma/coexpressionSearch.doCoexpressionSearch");
		}
	},
	getLastCSC : function(){
		return this.lastCSC;
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
			this.fireEvent('warning',"You can only perform a coexpression search with up to " + Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY +
					" genes. Please note that your list(s) of genes have been trimmed automatically.<br>");
			return "";

		} else {
			return "";
		}
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
		if (!this.efChooserPanel) {
			this.initChooserPanel();
		}
		var efMap = this.efChooserPanel.eeFactorsMap;

		Ext.apply(newDsc, {
					geneIds : this.getGeneIds(),
					selectedFactors : efMap,
					threshold : Gemma.DEFAULT_THRESHOLD,
					taxonId : this.taxonId
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
	
	initChooserPanel: function(){
		/* factor chooser for differential expression */
		this.efChooserPanel = new Gemma.ExperimentalFactorChooserPanel({
			modal: true
		});
		
	},

	/**
	 * Show the user interface for choosing factors. This happens
	 * asynchronously, so listen for the factors-chosen event.
	 */
	chooseFactors : function() {

		if (this.getExperimentIds().length === 0) {
			Ext.Msg.alert("Warning", "You should select at least one experiment to analyze");
		} else {
			var eeIds = this.getExperimentIds();
			
		if (!this.efChooserPanel) {
			this.initChooserPanel();
		}
			this.efChooserPanel.show(eeIds);
		}
	},
	// need to run chooseFactors() first!
	doDifferentialExpressionSearch : function(dsc) {

		this.fireEvent('clearError');
		if (!dsc) {
			dsc = this.getDiffSearchCommand();
		}
		var msg = this.validateDiffExSearch(dsc);
		if (msg.length !== 0) {
			this.fireEvent('error', msg);
			return;
		} else {
		
			this.chooseFactors();
			this.efChooserPanel.on("factors-chosen", function(efmap){
			
				dsc.selectedFactors = efmap;
				
				var errorHandler = function(msg, e){
					this.fireEvent('error', msg, e);	
				}
				
				DifferentialExpressionSearchController.getDiffExpressionForGenes(dsc, {
					callback: this.returnFromDiffExSearch.createDelegate(this),
					errorHandler: errorHandler.createDelegate(this)
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
			this.fireEvent('error', msg);
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

			this.fireEvent('warning', String.format(Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.autoTrimmingText, 
								Gemma.MAX_GENES_PER_CLASSIC_DIFFEX_QUERY, 'genes'));
			return "";

		} else {
			return "";
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
			geneNames : geneNames,
			datasetNames : experimentNames,
			taxonId : this.taxonId,
			taxonName : this.taxonName,
			pvalue : Gemma.DEFAULT_THRESHOLD,
			datasetCount : experimentCount
		};
		return data;
	},

	returnFromCoexSearch : function(result) {
		this.doneCoex = true;
		// if both coex and diff ex searches were called, don't hide load mask
		// until both have returned
		if (this.runningDiffEx && this.runningCoex) {
			if (!this.doneDiffEx) {
				return;
			} else {
				//var data = this.getDataForDiffVisualization();
				this.fireEvent('showDiffExResults', result, data);
			}
		}
		this.fireEvent('aftersearch', result);
		this.fireEvent('showCoexResults', result);
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
		if (this.runningDiffEx && this.runningCoex) {
			if (!this.doneCoex) {
				return;
			} else {
				this.fireEvent('showCoexResults', result);
			}
		}
		this.fireEvent('aftersearch', result);

		var data;
		if(!this.showClassicDiffExResults){
			//data = this.getDataForDiffVisualization();
		}else{
			data = null;
		}
		this.fireEvent('showDiffExResults', result, data);
	},
		
	initComponent: function(){
		Gemma.AnalysisResultsSearchMethods.superclass.initComponent.call(this);
	},
			
	constructor: function(configs){
		if(typeof configs !== 'undefined'){
			Ext.apply(this, configs);
		}
		Gemma.AnalysisResultsSearchMethods.superclass.constructor.call(this);
	}

});
