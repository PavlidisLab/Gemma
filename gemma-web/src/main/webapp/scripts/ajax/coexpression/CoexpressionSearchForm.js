
/**
 * The input for coexpression searches. This form has two main parts: a GeneChooserPanel, and the coexpression search
 * parameters.
 * 
 * Coexpression search has three main settings, plus an optional part that appears if the user is doing a 'custom'
 * analysis: Stringency, "Among query genes" checkbox, and the "scope".
 * 
 * If scope=custom, a DatasetSearchField is shown.
 * 
 * @authors Luke, Paul, klc
 * 
 * @version $Id$
 */

Gemma.CoexpressionSearchForm = Ext.extend(Ext.Panel, {

	layout : 'border',
	width : 390,
	height : 480,
	frame : true,
	stateful : true,
	stateEvents : ["beforesearch"],
	taxonComboReady : false,
	eeSetReady : false,

	// share state with main page...
	stateId : "Gemma.CoexpressionSearch",

	defaults : {
		collapsible : true,
		bodyStyle : "padding:10px"
	},

	applyState : function(state, config) {
		if (state) {
			this.csc = state;
		}
	},

	getState : function() {
		var currentState = this.getCoexpressionSearchCommand();
		delete currentState.eeIds;
		return currentState;

	},

	restoreState : function() {

		if (this.eeSetReady && this.taxonComboReady) {
			this.loadMask.hide();

			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Searching for coexpressions ..."
					});

			var queryStart = document.URL.indexOf("?");
			if (queryStart > -1) {
				this.initializeFromQueryString(document.URL.substr(queryStart + 1));
			} else if (this.csc && queryStart < 0) {
				this.initializeFromCoexpressionSearchCommand(this.csc);
			}
		}

	},

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
					geneIds : this.geneChooserPanel.getGeneIds(),
					stringency : Ext.getCmp('stringencyfield').getValue(),
					forceProbeLevelSearch : Ext.getCmp('forceProbeLevelSearch').getValue(),
					useMyDatasets : Ext.getCmp('forceUseMyDatasets').getValue(),
					taxonId : this.geneChooserPanel.getTaxonId(),
					queryGenesOnly : Ext.getCmp('querygenesonly').getValue()
				});

		if (this.currentSet) {
			newCsc.eeIds = this.getActiveEeIds();
			newCsc.eeSetName = this.currentSet.get("name");
			newCsc.eeSetId = this.currentSet.get("id");
			newCsc.dirty = this.currentSet.dirty; // modified without save
		}
		return newCsc;
	},

	/**
	 * Restore state from the URL (e.g., bookmarkable link)
	 * 
	 * @param {}
	 *            query
	 * @return {}
	 */
	getCoexpressionSearchCommandFromQuery : function(query) {
		var param = Ext.urlDecode(query);
		var eeQuery = param.eeq || "";

		var csc = {
			geneIds : param.g ? param.g.split(',') : [],
			stringency : param.s || Gemma.MIN_STRINGENCY,
			eeQuery : param.eeq,
			taxonId : param.t
		};
		if (param.q) {
			csc.queryGenesOnly = true;
		}

		if (param.ees) {
			csc.eeIds = param.ees.split(',');
		}

		if (param.dirty) {
			csc.dirty = true;
		}

		if (param.a) {
			csc.eeSetId = param.a;
		} else {
			csc.eeSetId = -1;
		}

		if (param.an) {
			csc.eeSetName = param.an;
		}

		if (param.setName) {
			csc.eeSetName = param.setName;
		}

		return csc;
	},

	initializeFromQueryString : function(query) {
		this.csc = this.getCoexpressionSearchCommandFromQuery(query);
		this.initializeFromCoexpressionSearchCommand(this.csc, true);
	},

	initializeGenes : function(csc, doSearch) {
		if (csc.geneIds.length > 1) {
			// load into table.
			this.geneChooserPanel.loadGenes(csc.geneIds, this.maybeDoSearch.createDelegate(this, [csc, doSearch]));
		} else {
			// show in combobox.
			this.geneChooserPanel.setGene(csc.geneIds[0], this.maybeDoSearch.createDelegate(this, [csc, doSearch]));
		}
	},

	/**
	 * make the form look like it has the right values; this will happen asynchronously... so do the search after it is
	 * done
	 */
	initializeFromCoexpressionSearchCommand : function(csc, doSearch) {
		this.geneChooserPanel = Ext.getCmp('gene-chooser-panel');

		if (csc.dirty) {
			/*
			 * Need to add a record to the eeSetChooserPanel.
			 */
		}

		if (csc.taxonId) {
			this.geneChooserPanel.getTopToolbar().taxonCombo.setTaxon(csc.taxonId);
		}

		if (csc.eeSetName) {
			this.currentSet = this.eeSetChooserPanel.selectByName(csc.eeSetName);
			if (this.currentSet) {
				csc.eeSetId = this.currentSet.get("id");
			}
		} else if (csc.eeSetId >= 0) {
			this.eeSetChooserPanel.selectById(csc.eeSetId, false);
		}

		if (csc.stringency) {
			Ext.getCmp('stringencyfield').setValue(csc.stringency);
		}
		
		if (csc.geneIds.length>1) {
			Ext.getCmp("querygenesonly").enable();
		}
		
		if (csc.queryGenesOnly) {
			Ext.getCmp("querygenesonly").setValue(true);
		}

		// Keep this last. When done loading genes might start coexpression query
		this.initializeGenes(csc, doSearch);
	},

	maybeDoSearch : function(csc, doit) {
		if (doit) {
			this.doSearch(csc);
		}
	},

	/**
	 * Create a URL that can be used to query the system.
	 * 
	 * @param {}
	 *            csc
	 * @return {}
	 */
	getBookmarkableLink : function(csc) {
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

		// Putting eeids in the bookmarkable link make them somewhat unusable.
		// if (csc.eeIds) {
		// url += String.format("&ees={0}", csc.eeIds.join(","));
		// }

		if (csc.dirty) {
			url += "&dirty=1";
		}

		return url;
	},

	doSearch : function(csc) {
		if (!csc) {
			csc = this.getCoexpressionSearchCommand();
		}
		this.clearError();

		var msg = this.validateSearch(csc);
		if (msg.length === 0) {
			if (this.fireEvent('beforesearch', this, csc) !== false) {
				this.loadMask.show();
				var errorHandler = this.handleError.createDelegate(this, [], true);
				ExtCoexpressionSearchController.doSearch(csc, {
							callback : this.returnFromSearch.createDelegate(this),
							errorHandler : errorHandler
						});
			}
			if (typeof pageTracker !== 'undefined') {
				pageTracker._trackPageview("/Gemma/coexpressionSearch.doSearch");
			}
		} else {
			this.handleError(msg);
		}
	},

	handleError : function(msg, e) {
		// console.log(e); // this contains the full stack.
		Ext.DomHelper.overwrite("coexpression-messages", {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});
		Ext.DomHelper.append("coexpression-messages", {
					tag : 'span',
					html : "&nbsp;&nbsp;" + msg
				});
		this.returnFromSearch({
					errorState : msg
				});

	},

	clearError : function() {
		Ext.DomHelper.overwrite("coexpression-messages", "");
	},

	validateSearch : function(csc) {
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
		} else {
			return "";
		}
	},

	returnFromSearch : function(result) {
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);
	},

	/**
	 * 
	 * @param {}
	 *            datasets The selected datasets (ids)
	 * @param {}
	 *            eeSet The ExpressionExperimentSet that was used (if any) - it could be just as a starting point.
	 */
	updateDatasetsToBeSearched : function(datasets, eeSetName, dirty) {

		var numDatasets = 0;

		if (!datasets) {
			if (this.currentSet)
				numdatasets = this.currentSet.get("expressionExperimentIds").length;
		} else
			numDatasets = datasets.length;

		if (numDatasets !== 0)
			Ext.getCmp('stringencyfield').maxValue = numDatasets;

		Ext.getCmp('analysis-options').setTitle(String.format("Analysis options - Up to {0} datasets will be analyzed",
				numDatasets));
	},

	getActiveEeIds : function() {
		if (this.currentSet) {
			return this.currentSet.get("expressionExperimentIds");
		}
		return [];
	},

	searchForGene : function(geneId) {
		this.geneChooserPanel.setGene.call(this.geneChooserPanel, geneId, this.doSearch.createDelegate(this));
	},

	initComponent : function() {

		this.geneChooserPanel = new Gemma.GeneGrid({
					height : 400,
					width : 230,
					region : 'center',
					id : 'gene-chooser-panel'
				});

		Ext.apply(this.geneChooserPanel.getTopToolbar().taxonCombo, {
					stateId : "",
					stateful : false,
					stateEvents : []
				});

		/*
		 * Shows the combo box and 'edit' button
		 */
		this.eeSetChooserPanel = new Gemma.DatasetGroupComboPanel();

		/*
		 * Filter the EESet chooser based on gene taxon.
		 */
		this.geneChooserPanel.on("taxonchanged", function(taxon) {
					this.eeSetChooserPanel.filterByTaxon(taxon);
				}.createDelegate(this));

		this.eeSetChooserPanel.on("select", function(combo, eeSetRecord, index) {

					if (eeSetRecord === null || eeSetRecord === undefined) {
						return;
					}

					this.currentSet = eeSetRecord;

					this.updateDatasetsToBeSearched(eeSetRecord.get("expressionExperimentIds"), eeSetRecord);

					/*
					 * Detect a change in the taxon via the EESet chooser.
					 */
					this.geneChooserPanel.taxonChanged({
								id : this.currentSet.get("taxonId"),
								name : this.currentSet.get("taxonName")
							});
				}.createDelegate(this));

		this.eeSetChooserPanel.on("ready", function() {
					this.eeSetReady = true;
					this.restoreState();
				}.createDelegate(this));

		Ext.apply(this, {

			title : "Search configuration",
			items : [this.geneChooserPanel, {
				xtype : 'panel',
				title : 'Analysis options',
				// collapsedTitle : '[Analysis options]',
				id : 'analysis-options',
				region : 'south',
				frame : true,
				cmargins : '5 0 0 0 ',
				margins : '5 0 0 0 ',
				// plugins : new Ext.ux.CollapsedPanelTitlePlugin(),
				width : 250,
				height : 230,
				items : [{
					xtype : 'fieldset',
					autoHeight : true,
					height : 90,
					items : [{
								xtype : 'numberfield',
								id : 'stringencyfield',
								allowBlank : false,
								allowDecimals : false,
								allowNegative : false,
								minValue : Gemma.MIN_STRINGENCY,
								maxValue : 999,
								fieldLabel : 'Stringency',
								invalidText : "Minimum stringency is " + Gemma.MIN_STRINGENCY,
								value : 2,
								width : 60,
								tooltip : "The minimum number of datasets that must show coexpression for a result to appear"
							}, {
								xtype : 'checkbox',
								id : 'forceProbeLevelSearch',
								fieldLabel : 'Force Probe query',
								disabled : !this.admin,
								hidden : !this.admin,
								hideLabel : !this.admin,
								tooltip : "Always do the query at the level of probes. May be slower but always gets most current information from newly-processed data sets."
							}, {
								xtype : 'checkbox',
								id : 'forceUseMyDatasets',
								fieldLabel : 'Use my data',
								disabled : !this.user,
								hidden : !this.user,
								hideLabel : !this.user,
								tooltip : "Add your data sets to the search, if available (and for the selected taxon)"
							}, {
								xtype : 'checkbox',
								id : 'querygenesonly',
								fieldLabel : 'My genes only',
								disabled : true,
								tooltip : "Restrict the output to include only links among the listed query genes"
							}, this.eeSetChooserPanel]
				}]
			}],
			buttons : [{
				text : "Find coexpressed genes",
				handler : this.doSearch.createDelegate(this, [], false)
					// pass
					// no
					// parameters!
				}]
		});
		Gemma.CoexpressionSearchForm.superclass.initComponent.call(this);
		this.addEvents('beforesearch', 'aftersearch');		
		
		this.geneChooserPanel.on("addgenes", function(geneids) {
					if (this.geneChooserPanel.getGeneIds().length > 1) {
						var cmp = Ext.getCmp("querygenesonly");
						cmp.enable();
					}

				}, this);

		this.geneChooserPanel.on("removegenes", function() {
					if (this.geneChooserPanel.getGeneIds().length < 2) {
						var cmp = Ext.getCmp("querygenesonly");
						cmp.setValue(false);
						cmp.disable();
					} else {
						// console.log(this.geneChooserPanel.getGeneIds().length);
					}
				}, this);

		this.on('afterrender', function() {
					Ext.apply(this, {
								loadMask : new Ext.LoadMask(this.getEl(), {
											msg : "Preparing Coexpression Interface  ..."
										})
							});

					this.loadMask.show();
				});

		/*
		 * This horrible mess. We listen to taxon ready event and filter the presets on the taxon.
		 */
		this.geneChooserPanel.getTopToolbar().taxonCombo.on("ready", function(taxon) {
					this.taxonComboReady = true;
					this.restoreState(this);
				}.createDelegate(this), this);
	}

});
