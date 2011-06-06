/**
 * @author thea
 * @version $Id: AnalysisResultsSearchForm.js,v 1.34 2011/05/06 04:02:25 paul
 *          Exp $
 */
Ext.namespace('Gemma');

/**
 * The input for guided coexpression and differential expression searches. This
 * form has four main parts: a taxon chooser, an experiment (group) searcher, a
 * gene (group) searcher and links to perform searches
 * 
 * Coexpression search has an optional part that appears if the user is doing a
 * 'custom' analysis, I've used defaults for these options: Stringency = 2
 * "Force Probe query" = false "Use my data" = false "My genes only" = false
 * 
 * 
 */

Gemma.MAX_GENES_PER_QUERY = 20; // this is the value used for coexpression and
// diff expression searches

// max suggested number of elements to use for a diff ex viz query
Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY = 100;
Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY = 100;

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

	PREVIEW_SIZE : 5,

	// defaults for coexpression
	DEFAULT_STRINGENCY : 2,
	DEFAULT_forceProbeLevelSearch : false,
	DEFAULT_useMyDatasets : false,
	DEFAULT_queryGenesOnly : false,

	// defaults for differential expression
	// using Gemma.DEFAULT_THRESHOLD, Gemma.MIN_THRESHOLD, Gemma.MAX_THRESHOLD

	geneIds : [],
	geneGroupId : null, // keep track of what gene group has been selected
	experimentIds : [],

	listeners : {
		'ready' : function() {
			this.loadMask.hide();
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Searching for analysis results ..."
					});
		}
	},

	/***************************************************************************
	 * * SEARCH **
	 **************************************************************************/

	/**
	 * check that there are some experiments and genes to run on
	 * if there are too many experiments or genes, warn the user and offer to trim
	 * 
	 * after optional trimming, call the search function (doSearch)
	 * 
	 * @param {Object} geneRecords
	 * @param {Object} experimentRecords
	 * @return 
	 */
	validateSearch: function(geneRecords, experimentRecords){
		if (geneRecords.length === 0) {
			Ext.Msg.alert("Error", "Gene(s) must be selected before continuing.");
			return;
		}

		if (experimentRecords.length === 0) {
			Ext.Msg.alert("Error", "Experiment(s) must be selected before continuing.");
			return;
		}
		//get the total number of genes 
		var i; var rec;
		var geneCount = 0;
		for(i = 0; i< geneRecords.length; i++){
			rec = geneRecords[i];
			if(rec.memberIds){
				geneCount += rec.memberIds.length;
			}
		}
		//get the total number of experiments 
		var experimentCount = 0;
		for(i = 0; i< experimentRecords.length; i++){
			rec = experimentRecords[i];
			if(rec.memberIds){
				experimentCount += rec.memberIds.length;
			}
		}
		var stateText = "";
		var maxText = "";
		if(geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY && experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY ){
			stateText = geneCount + " genes and "+ experimentCount + " experiments";
			maxText = Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY + " genes and "+Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY +" experiments";
		}
		else if(experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY){
			stateText = experimentCount + " experiments";
			maxText = Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY +" experiments";
		}
		else if(geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY){
			stateText = geneCount + " genes";
			maxText = Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY + " genes";
		}
		
		if(geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY || experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY){
			this.getEl().mask();
			var warningWindow = new Ext.Window({
				width:450,
				height:200,
				bodyStyle:'padding:7px;background: white; font-size:1.1em',
				title: "Warning",
				html: "You are using " + stateText + " for your search. " +
					"Searching for more than " + maxText +
					" can take some time to load and can slow down your interactions with the search results. " +
					"You may also encounter error messages unless you are using the "+
					"<a target='_blank' href='http://www.google.com/chrome/'>Chrome</a> browser. <br><br>" +
					"We suggest you cancel this search and refine your selections or let us trim your query.",
				//icon: Ext.Msg.WARNING,
				buttons: [{
					text: 'Trim',
					tooltip:'Your query will be trimmed to '+maxText,
					handler: function(){
						if(geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY)
						geneRecords = this.trimRecordSelection(geneRecords, Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY);
						
						if(experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY)
						experimentRecords = this.trimRecordSelection(experimentRecords, Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY);
						
						this.doSearch(geneRecords, experimentRecords);
						warningWindow.close();
						return;
					},
					scope: this
				}, {
					text: 'Don\'t trim',
					tooltip:'Continue with your search as is',
					handler: function(){
						this.doSearch(geneRecords, experimentRecords);
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
				}],
				listeners:{
					close:function (){
						this.getEl().unmask();
					},
					scope:this
				}
			});
			warningWindow.show();
		}else{
			this.doSearch(geneRecords, experimentRecords);
			return;
		}
		
	},
	trimRecordSelection: function(records, max){
		var runningCount = 0;
		var i; var rec;
		var trimmedRecords = [];
		for(i = 0; i< records.length; i++){
			rec = records[i];
			if(rec.memberIds && (runningCount+rec.memberIds.length)<max){
				runningCount += rec.memberIds.length;
				trimmedRecords.push(rec);
			}else if(rec.memberIds){
				var trimmedIds = rec.memberIds.slice(0, (max - runningCount));
				rec.memberIds = trimmedIds;
			 	rec.reference = null;
				rec.type = null;
			 	rec.name = "Trimmed " + rec.name;
				trimmedRecords.push(rec);
			}
		}
		return trimmedRecords;
	},

	doSearch : function(geneRecords, experimentRecords) {
		
		this.fireEvent('beforesearch', this);
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Searching for analysis results ..."
					});
		}
		this.collapsePreviews();
		this.loadMask.show();

		// reset flags marking if searches are done
		// only used if both searches are run at once
		this.doneCoex = false;
		this.doneDiffEx = false;

		// if using a GO group or 'all results' group for a search, make a
		// session-bound copy of it
		// and store the new reference
		var geneGroupsToMake = [];
		var geneGroupsAlreadyMade = [];
		var i;
		var record;
		for (i = 0; i < geneRecords.length; i++) {
			record = geneRecords[i];
			if (typeof record !== 'undefined' && (!record.reference || record.reference.id === null)) {
				// addNonModificationBasedSessionBoundGroups() takes a
				// genesetvalueobject, so add needed field
				record.geneIds = record.memberIds;
				
				// no java bean properties to match these javascript properties 
				delete record.memberIds;
				delete record.comboText;
				delete record.isGroup;
				delete record.type;
				
				geneGroupsToMake.push(record);

			} else {
				geneGroupsAlreadyMade.push(record);
			}
		}
		if (geneGroupsToMake.length > 0) {
			geneRecords = geneGroupsAlreadyMade;
			this.waitingForGeneSessionGroupBinding = true;
			GeneSetController.addNonModificationBasedSessionBoundGroups(geneGroupsToMake, function(geneSets) {
						// should be at least one geneset
						if (geneSets === null || geneSets.length === 0) {
							// TODO error message
							return;
						} else {
							for (j = 0; j < geneSets.length; j++) {
								geneRecords.push(geneSets[j]);
							}
						}
						this.waitingForGeneSessionGroupBinding = false;
						this.doSearch(geneRecords, experimentRecords);// recurse
						// so
						// once
						// all
						// session-bound
						// groups
						// are
						// made,
						// search
						// runs
						return;
					}.createDelegate(this));
			return;
		}
		var experimentGroupsToMake = [];
		var experimentGroupsAlreadyMade = [];

		for (i = 0; i < experimentRecords.length; i++) {
			record = experimentRecords[i];
			// if the group has a null value for reference.id, then it hasn't
			// been
			// created as a group in the database nor session
			if (typeof record !== 'undefined' && (!record.reference || record.reference.id === null)) {
				// addNonModificationBasedSessionBoundGroups() takes an
				// experimentSetValueObject, so add needed field
				record.expressionExperimentIds = record.memberIds;
								
				// no java bean properties to match these javascript properties 
				delete record.memberIds;
				delete record.comboText;
				delete record.isGroup;
				delete record.type;
				
				experimentGroupsToMake.push(record);
			} else {
				experimentGroupsAlreadyMade.push(record);
			}
		}
		if (experimentGroupsToMake.length > 0) {
			experimentRecords = experimentGroupsAlreadyMade;
			this.waitingForExperimentSessionGroupBinding = true;
			ExpressionExperimentSetController.addNonModificationBasedSessionBoundGroups(experimentGroupsToMake,
					function(datasetSets) {
						// should be at least one datasetSet
						if (datasetSets === null || datasetSets.length === 0) {
							// TODO error message
							return;
						} else {
							for (j = 0; j < datasetSets.length; j++) {
								experimentRecords.push(datasetSets[j]);
							}
						}
						this.doSearch(geneRecords, experimentRecords); // recurse
						// so
						// once
						// all
						// session-bound
						// groups
						// are
						// made,
						// search
						// runs
						return;
					}.createDelegate(this));
			return;
		}

		if (this.diffExToggle.pressed && this.coexToggle.pressed) {
			this.doDifferentialExpressionSearch();
			this.efChooserPanel.on("factors-chosen", function() {
						this.doCoexpressionSearch();
					}, this);
		}
		// if differential expression button is depressed, do a differential
		// search
		else if (this.diffExToggle.pressed) {

			// this.doDifferentialExpressionSearch();
			var data = this.getDataForDiffVisualization(geneRecords, experimentRecords);
			// console.log(data);
			this.fireEvent('showDiffExResults', this, null, data);
			// this.loadMask.show(); // can't get it to hide
		}

		// if coexpression button is depressed, do a coexpression search
		else if (this.coexToggle.pressed) {
			this.doCoexpressionSearch();
		}

	},

	/***************************************************************************
	 * * COEXPRESSION **
	 **************************************************************************/

	/**
	 * Construct the coexpression command object from the form, to be sent to
	 * the server.
	 * 
	 * @return {}
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
		} else {
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
		} else if (csc.geneIds.length > Gemma.MAX_GENES_PER_QUERY) {
			// if trying to search for more than the allowed limit of genes,
			// show error

			// prune the gene Ids
			csc.geneIds = csc.geneIds.slice(0, Gemma.MAX_GENES_PER_QUERY);

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
			 * chooser.selectedGeneOrGroupRecord.reference = null;
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
			 * return "You can only search up to " + Gemma.MAX_GENES_PER_QUERY + "
			 * genes. Please note that your list(s) of genes have been trimmed
			 * automatically. <br>"+ "Press 'Go' again to run the search with
			 * this trimmed list or re-enter your gene query(ies) and "+ "use
			 * the edit tool to manually trim your selection(s).";
			 */
			this.handleError("You can only perform a coexpression search with up to " + Gemma.MAX_GENES_PER_QUERY +
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
		url = url.replace("home2", "searchCoexpression");

		return url;
	},

	/***************************************************************************
	 * * DIFFERENTIAL EXPRESSION **
	 **************************************************************************/
	/**
	 * Construct the differential command object from the form, to be sent to
	 * the server.
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
	 * 
	 * 
	 */
	chooseFactors : function() {
		if (this.getSelectedExperimentRecords().length <= 0) {
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
			this.efChooserPanel.on("factors-chosen", function(efmap) {

						if (!dsc) {
							dsc = this.getDiffSearchCommand();
						} else {
							dsc.selectedFactors = efmap;
						}
						this.clearError();
						var msg = this.validateDiffExSearch(dsc);
						if (msg.length === 0) {
							this.loadMask.show();
							var errorHandler = this.handleError.createDelegate(this, [], true);
							DifferentialExpressionSearchController.getDiffExpressionForGenes(dsc, {
										callback : this.returnFromDiffExSearch.createDelegate(this),
										errorHandler : errorHandler
									});
						} else {
							this.handleError(msg, e);
						}
						if (typeof pageTracker !== 'undefined') {
							pageTracker._trackPageview("/Gemma/differentialExpressionSearch.doSearch");
						}
					}, this, {
						single : true
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

		url = url.replace("home2", "diff/diffExpressionSearch");

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
		} else if (dsc.geneIds.length > Gemma.MAX_GENES_PER_QUERY) {
			// if trying to search for more than the allowed limit of genes,
			// show error

			// prune the gene Ids
			dsc.geneIds = dsc.geneIds.slice(0, Gemma.MAX_GENES_PER_QUERY);
			this.loadGenes(dsc.geneIds); // TODO loadGenes isn't in this
			// class anymore

			return "You can only search up to " + Gemma.MAX_GENES_PER_QUERY +
					 " genes. Please note that your list of genes has been trimmed automatically. <br>" +
					 "Press 'Go' again to run the search with this trimmed list or re-enter your gene query and " +
					 "use the edit tool to manually trim your selection.";

		} else {
			return "";
		}
	},

	/** Shared methods * */

	handleError : function(msg, e) {
		Ext.DomHelper.overwrite("analysis-results-search-form-messages", {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});

		if (!(msg.length === 0)) {
			Ext.DomHelper.append("analysis-results-search-form-messages", {
						tag : 'span',
						html : "&nbsp;&nbsp;" + msg
					});
		} else {
			Ext.DomHelper.append("analysis-results-search-form-messages", {
						tag : 'span',
						html : "&nbsp;&nbsp;Error retrieving results."
					});
		}
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, e);
		if (e && !(msg.length === 0)) {
			Ext.Msg.alert("Error", e + "/n" + msg);
		}
	},

	clearError : function() {
		Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
	},

	getDataForDiffVisualization : function(geneRecords, experimentRecords) {
		var geneRecordReferences = [];
		var geneNames = [];
		var i;
		if (geneRecords.length > 0) {
			for (i = 0; i < geneRecords.length; i++) {
				geneRecordReferences.push(geneRecords[i].reference);
				geneNames.push(geneRecords[i].name);
			}
		}
		var experimentRecordReferences = [];
		var experimentNames = [];
		var experimentCount = 0;
		if (experimentRecords.length > 0) {
			for (i = 0; i < experimentRecords.length; i++) {
				experimentRecordReferences.push(experimentRecords[i].reference);
				experimentNames.push(experimentRecords[i].name);
				experimentCount += experimentRecords[i].memberIds.size();
			}
		}
		var data = {
			geneReferences : geneRecordReferences,
			geneSessionGroupQueries : this.getGeneSessionGroupQueries(),
			experimentSessionGroupQueries : this.getExperimentSessionGroupQueries(),
			datasetReferences : experimentRecordReferences,
			geneNames : geneNames,
			datasetNames : experimentNames,
			taxonId : this.getTaxonId(),
			taxonName : this.getTaxonName(),
			pvalue : Gemma.DEFAULT_THRESHOLD,
			datasetCount : experimentCount,
			selectionsModified : this.wereSelectionsModified()
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
				var data = this.getDataForDiffVisualization();
				this.fireEvent('showDiffExResults', this, result, data);
			}
		}
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);
		this.fireEvent('showCoexResults', this, result);

	},
	returnFromDiffExSearch : function(result) {
		this.doneDiffEx = true;
		// if both coex and diff ex searches were called, don't hide load mask
		// until both have returned
		if (this.diffExToggle.pressed && this.coexToggle.pressed) {
			if (!this.doneCoex) {
				return;
			} else {
				this.fireEvent('showCoexResults', this, result);
			}
		}
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);

		var data = this.getDataForDiffVisualization();
		this.fireEvent('showDiffExResults', this, result, data);
	},
	wereSelectionsModified: function(){
		var wereModified = false;
		this.geneChoosers.items.each(function(){
			if (this.xtype === 'geneSearchAndPreview' && typeof this.selectedGeneOrGroupRecord !== 'undefined') {
				if( this.listModified){
					wereModified = true;
				}
			}
		});
		if(!wereModified){
			this.experimentChoosers.items.each(function(){
				if (this.xtype === 'experimentSearchAndPreview' && typeof this.selectedExperimentOrGroupRecord !== 'undefined') {
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
			if (this.xtype === 'geneSearchAndPreview' && typeof this.selectedGeneOrGroupRecord !== 'undefined') {
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
			if (this.xtype === 'experimentSearchAndPreview' && typeof this.selectedExperimentOrGroupRecord !== 'undefined') {
				if( this.queryUsedToGetSessionGroup !== null ){
					queries.push(this.queryUsedToGetSessionGroup);
				}
			}
		});
		return queries;
	},
	getSelectedGeneRecords : function() {
		var selectedGeneRecords = [];
		this.geneChoosers.items.each(function(){
			if (this.xtype === 'geneSearchAndPreview' && typeof this.selectedGeneOrGroupRecord !== 'undefined') {
				selectedGeneRecords.push(this.selectedGeneOrGroupRecord);
			}
		});
		return selectedGeneRecords;
	},

	getSelectedExperimentRecords : function() {
		var selectedExperimentRecords = [];
		this.experimentChoosers.items.each(function() {
					if (this.xtype === 'experimentSearchAndPreview' &&
						typeof this.selectedExperimentOrGroupRecord !== 'undefined') {
						selectedExperimentRecords.push(this.selectedExperimentOrGroupRecord);
					}
				});
		return selectedExperimentRecords;
	},
	getExperimentIds : function() {
		var eeIds = [];
		var i;
		var j;
		var records = this.getSelectedExperimentRecords();
		for (i = 0; i < records.length; i++) {
			var record = records[i];
			for (j = 0; j < record.memberIds.length; j++) {
				eeIds.push(record.memberIds[j]);
			}
		}
		return eeIds;
	},

	getGeneIds : function() {
		var i;
		var j;
		var geneIds = [];
		var records = this.getSelectedGeneRecords();
		for (i = 0; i < records.length; i++) {
			var record = records[i];
			for (j = 0; j < record.memberIds.length; j++) {
				geneIds.push(record.memberIds[j]);
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
					defaults : {
						border : false
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

		this.coexRadio = new Ext.form.Radio({
					name:'searchMode',
					boxLabel: "<span style=\"font-size:1.7em\">Coexpression</span>",
					//style : 'padding-bottom:0.4em',
					checked : false
				});		

		this.diffExRadio = new Ext.form.Radio({
					name:'searchMode',
					boxLabel: "<span style=\"font-size:1.7em\">Differential Expression</span>",
					style : 'padding:0.4em',
					checked : true
				});		

		this.coexToggle = new Ext.Button({
					text : "<span style=\"font-size:1.3em\">Coexpression</span>",
					cls:'highlightToggle',
					style:'margin-top:5px',
					scale : 'medium',
					width : 150,
					enableToggle : true,
					pressed : false
				});
		this.coexToggle.on('click', function() {
					this.diffExToggle.toggle();
				}, this);
		this.diffExToggle = new Ext.Button({
					text : "<span style=\"font-size:1.3em\">Differential Expression</span>",
					scale : 'medium',
					style:'margin-top:5px',
					cls:'highlightToggle',
					width : 150,
					enableToggle : true,
					pressed : true
				});
		this.diffExToggle.on('click', function() {
					this.coexToggle.toggle();
				}, this);

		this.searchBar = new Ext.Panel({

					border : false,
					layout : 'hbox',
					width : 490,
					style : 'padding: 0 7px',
					defaults : {
						border : false
					},
					items : [{
								html : 'Search for ',
								style : 'text-align:center;vertical-align:middle;font-size:1.7em;padding-top:10px;padding-right: 7px'
							}, this.coexToggle, {
								html : 'or',
								style : 'text-align:center;vertical-align:middle;font-size:1.7em;padding-top:10px;padding: 7px'
							}, this.diffExToggle, {
								html : 'in&hellip;',
								style : 'text-align:center;vertical-align:middle;font-size:1.7em;padding-top:10px;padding-left: 7px'
							}]

				});

		/*************** TEXT *********************/

				this.theseExperimentsPanel = new Ext.Panel({
					html: 'these experiments',
					style: 'text-align:center;font-size:1.4em;',
					tpl: new Ext.XTemplate('these <span class="blue-text-not-link" style="font-weight:bold " ',
					 'ext:qtip="Searches are limited to one taxon, if you want to change the taxon, click the reset button.">',
					 '{taxonCommonName} </span> experiments ', 
					 '<img src="/Gemma/images/icons/question_blue.png" ext:qtip="Searches are limited to one taxon, ' +
					'if you want to change the taxon, click the reset button on the right."/> '),
					tplWriteMode: 'overwrite'
				});
				this.theseGenesPanel = new Ext.Panel({
					html: 'these genes',
					style: 'text-align:center;font-size:1.4em;padding:0px',
					tpl: new Ext.XTemplate('these <span class="blue-text-not-link" style="font-weight:bold " ', 
					'ext:qtip="Searches are limited to one taxon, if you want to change the taxon, click the reset button.">', 
					'{taxonCommonName}</span> genes ', 
					'<img src="/Gemma/images/icons/question_blue.png" ext:qtip="Searches are limited to one taxon, ' +
					'if you want to change the taxon, click the reset button on the right."/> '),
					tplWriteMode: 'overwrite'
				});

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
					width: 850,
					border: false,
					defaults: {
						border: false,
						style: 'padding: 3px'
					},
					items: [{
						defaults: {
							border: false
						},
						items: [this.theseExperimentsPanel, this.experimentChoosers, {
							width: 340,
							html: 'Example: search for Alzheimer\'s and select all human experiments <br> '
						}]
					}, {
						html: ' based on ',
						style: 'font-size:1.7em;padding-top: 32px;margin-left: -15px;margin-right: -12px;'
					}, {
						defaults: {
							border: false
						},
						items: [this.theseGenesPanel, this.geneChoosers, {
							html: '<div style="width:340px ; padding-left:10px">Example: search for "map kinase" and select a GO group<br>'
						}]
					}, {
						style:'padding:20 0 0 0px;margin:0px;',
						items: [{
							xtype: 'button',
							text: "<span style=\"font-size:1.3em;padding-top:15px\">Go!</span>",
							width: 55,
							scale: 'medium',
							listeners: {
								click: function(){
									this.validateSearch(this.getSelectedGeneRecords(), this.getSelectedExperimentRecords());
								}.createDelegate(this, [], false)
							}
						
						}, {
							xtype: 'button',
							width: 55,
							icon: '/Gemma/images/icons/arrow_refresh_small.png',
							style: 'padding-top: 8px',
							text:'Reset',
							handler: this.reset.createDelegate(this)
						}]
					}]
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
					if (this.xtype === 'geneSearchAndPreview') {
						this.geneCombo.setTaxonId(taxonId);
					}
				});
		this.experimentChoosers.items.each(function() {
					if (this.xtype === 'experimentSearchAndPreview') {
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
						if (this.xtype === 'geneSearchAndPreview') {
							this.geneSelectionEditorBtn.hide(); // needed here
							// in case 'go'
							// is pressed
							// before
							// preview is
							// done loading
							this.genePreviewContent.collapse();

						}
					});
		}
	},

	collapseExperimentPreviews : function() {
		if (typeof this.experimentChoosers.items !== 'undefined') {
			this.experimentChoosers.items.each(function() {
						if (this.xtype === 'experimentSearchAndPreview') {
							this.experimentPreviewContent.collapse();
						}
					});
		}

	},

	addGeneChooser : function() {
		this.geneChooserIndex++;

		this.geneChoosers.add(
				{
			xtype : 'geneSearchAndPreview',
			searchForm : this,
			style : 'padding-top:10px;margin-left:10px',
			id : 'geneChooser' + this.geneChooserIndex,
			taxonId : this.taxonId,
			listeners : {
				madeFirstSelection : function() {
					// Ext.getCmp(this.getId()+'Button').enable();
					this.searchForm.addGeneChooser();
					this.removeBtn.show();
				},
				removeGene : function() {
					this.searchForm.removeGeneChooser(this.getId());
				}
			}
				// }]
		});
		// change previous button to 'remove'
		if (typeof Ext.getCmp('geneChooser' + (this.geneChooserIndex - 1) + 'Button') !== 'undefined') {
			Ext.getCmp('geneChooser' + (this.geneChooserIndex - 1) + 'Button').show()
					.setIcon('/Gemma/images/icons/delete.png').setTooltip('Remove this gene or group from your search')
					.setHandler(this.removeGeneChooser.createDelegate(this, ['geneChooserPanel' +
									 (this.geneChooserIndex - 1)], false));
		}
		this.geneChoosers.doLayout();
	},

	removeGeneChooser : function(panelId) {
		this.geneChoosers.remove(panelId, true);
		this.geneChoosers.doLayout();
		
		if(this.getSelectedExperimentRecords().length === 0 && this.getSelectedGeneRecords().length === 0){
			this.reset();
		}
	},

	addExperimentChooser : function() {
		this.experimentChooserIndex++;

		this.experimentChoosers.add({
			xtype: 'experimentSearchAndPreview',
			searchForm: this,
			taxonId: this.taxonId,
			style: 'padding-top:10px;margin-right:10px',
			id: 'experimentChooser' + this.experimentChooserIndex,
			listeners: {
				madeFirstSelection: function(){
					// Ext.getCmp(this.getId()+'Button').enable();
					this.searchForm.addExperimentChooser();
					this.removeBtn.show();
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
		
		if(this.getSelectedExperimentRecords().length === 0 && this.getSelectedGeneRecords().length === 0){
			this.reset();
		}

	}

});
// this makes the text in the button change colour on hover, like a normal link
Ext.LinkButton = Ext.extend(Ext.Button, {
	template : new Ext.Template(
			'<table cellspacing="0" class="x-btn {3}"><tbody class="{4}">',
			'<tr><td class="x-btn-tl"><i>&#160;</i></td><td class="x-btn-tc"></td><td class="x-btn-tr"><i>&#160;</i></td></tr>',
			'<tr><td class="x-btn-ml"><i>&#160;</i></td><td class="x-btn-mc"><em class="{5}" unselectable="on"><a href="{6}" target="{7}" class="x-btn-text {2}"><button>{0}</button></a></em></td><td class="x-btn-mr"><i>&#160;</i></td></tr>',
			'<tr><td class="x-btn-bl"><i>&#160;</i></td><td class="x-btn-bc"></td><td class="x-btn-br"><i>&#160;</i></td></tr>',
			'</tbody></table>').compile(),
	buttonSelector : 'a:first',
	ctCls : "transparent-btn" // no button image
});

Ext.reg('analysisResultsSearchForm', Gemma.AnalysisResultsSearchForm);
