/*
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

Gemma.MIN_STRINGENCY = 2;

Gemma.CoexpressionSearchForm = Ext.extend(Ext.Panel, {

	layout : 'border',
	width : 390,
	height : 300,
	frame : true,
	stateful : true,
	stateEvents : ["beforesearch"],

	// share state with main page...
	stateId : "Gemma.CoexpressionSearch",

	defaults : {
		collapsible : true,
		// split : true,
		bodyStyle : "padding:10px"
	},

	applyState : function(state, config) {
		if (state) {
			this.csc = state;
		}
	},

	getState : function() {
		return this.getCoexpressionSearchCommand();
	},

	onRender : function() {
		Gemma.CoexpressionSearchForm.superclass.onRender.apply(this, arguments);

		Ext.apply(this, {
					loadMask : new Ext.LoadMask(this.getEl(), {
								msg : "Searching  ..."
							})
				});
	},

	restoreState : function() {
		// console.log("restoreState");
		var queryStart = document.URL.indexOf("?");
		if (queryStart > -1) {
			// console.log("Loading from url= " + document.URL);
			this.initializeFromQueryString(document.URL.substr(queryStart + 1));
		} else if (this.csc && queryStart < 0) {
			this.initializeFromCoexpressionSearchCommand(this.csc);
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

		if (param.setName) {
			csc.eeSetName = param.setName;
		}

		return csc;
	},

	initializeFromQueryString : function(query) {
		this.initializeFromCoexpressionSearchCommand(this.getCoexpressionSearchCommandFromQuery(query), true);
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
		this.stringencyField = Ext.getCmp('stringencyfield');

		if (csc.dirty) {
			/*
			 * Need to add a record to the eeSetChooserPanel.
			 */
		}

		if (csc.taxonId) {
			this.geneChooserPanel.toolbar.taxonCombo.setState(csc.taxonId);
		}

		this.initializeGenes(csc, doSearch);

		if (csc.eeSetId >= 0) {
			this.eeSetChooserPanel.setState(csc.eeSetId);
		} else if (csc.eeSetName) {
			this.eeSetChooserPanel.setState(csc.eeSetName); // FIXME this won't work, expects id.
			this.updateDatasetsToBeSearched(csc.eeIds, csc.eeSetName, csc.dirty);
		}

		if (csc.stringency) {
			this.stringencyField.setValue(csc.stringency);
		}

		if (csc.queryGenesOnly) {
			this.queryGenesOnly.setValue(true);
		}
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

		if (csc.eeSetId) {
			url += String.format("&a={0}", csc.eeSetId);
		}

		if (csc.eeIds) {
			url += String.format("&ees={0}", csc.eeIds.join(","));
		}

		if (csc.dirty) {
			url += "&dirty=1";
		}

		if (csc.eeSetName) {
			// url += String.format("&setName={0}", csc.eeSetName);
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
			if (pageTracker) {
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
		this.loadMask.hide();
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
			return "Please select an analysis";
		} else {
			return "";
		}
	},

	returnFromSearch : function(result) {
		this.loadMask.hide();
		this.fireEvent('aftersearch', this, result);
		if (typeof pageTracker == 'function') {
			// console.log("yay");
			pageTracker._trackPageview("/pagefilename1"); // google analytics.
		}
	},

	/**
	 * 
	 * @param {}
	 *            datasets The selected datasets (ids)
	 * @param {}
	 *            eeSet The ExpressionExperimentSet that was used (if any) - it could be just as a starting point.
	 */
	updateDatasetsToBeSearched : function(datasets, eeSetName, dirty) {
		var numDatasets = datasets.length;
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

	afterRender : function() {

		Gemma.CoexpressionSearchForm.superclass.afterRender.call(this);
		// if (this.geneChooserPanel.toolbar.taxonCombo) {

		// }
	},

	searchForGene : function(geneId) {
		this.geneChooserPanel.setGene.call(this.geneChooserPanel, geneId, this.doSearch.createDelegate(this));
	},

	initComponent : function() {

		this.geneChooserPanel = new Gemma.GeneChooserPanel({
					height : 100,
					width : 230,
					region : 'center',
					id : 'gene-chooser-panel'
				});

		this.eeSetChooserPanel = new Gemma.ExpressionExperimentSetPanel({
					isAdmin : this.admin,
					store : new Gemma.ExpressionExperimentSetStore()
				});

		this.geneChooserPanel.on("taxonchanged", function(taxon) {
					this.eeSetChooserPanel.filterByTaxon(taxon);
				}.createDelegate(this));

		this.eeSetChooserPanel.on("set-chosen", function(eeSetRecord) {
					this.currentSet = eeSetRecord;
					this.updateDatasetsToBeSearched(eeSetRecord.get("expressionExperimentIds"), eeSetRecord);
					this.geneChooserPanel.taxonChanged(this.currentSet.get("taxon"));
				}.createDelegate(this));

		this.eeSetChooserPanel.combo.on("ready", this.restoreState.createDelegate(this));
		// this.eeSetChooserPanel.combo.on("ready", function() {
		// alert("yay");
		// }.createDelegate(this));

		Ext.apply(this, {

			title : "Search configuration",
			items : [this.geneChooserPanel, {
				xtype : 'panel',
				title : 'Analysis options',
				collapsedTitle : '[Analysis options]',
				id : 'analysis-options',
				region : 'south',
				cmargins : '5 0 0 0 ',
				margins : '5 0 0 0 ',
				plugins : new Ext.ux.CollapsedPanelTitlePlugin(),
				width : 250,
				height : 140,
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
						Ext.getCmp("querygenesonly").enable();
					}
				}, this);

		this.geneChooserPanel.on("removegenes", function() {
					if (this.geneChooserPanel.getGeneIds().length < 2) {
						Ext.getCmp("querygenesonly").disable();
					} else {
						// console.log(this.geneChooserPanel.getGeneIds().length);
					}
				}, this);

		/*
		 * This horrible mess. We listen to taxon ready event and filter the presets on the taxon.
		 */
		this.geneChooserPanel.toolbar.taxonCombo.on("ready", function(taxon) {
					// console.log("setting up filtering of combo");
					if (taxon) {
						if (this.eeSetChooserPanel.store.getRange().length > 0) {
							// console.log("Load was done, filtering");
							this.eeSetChooserPanel.filterByTaxon(taxon);
						} else {
							this.eeSetChooserPanel.store.on("load", function() {
										// console.log("Filtering after load");
										this.eeSetChooserPanel.filterByTaxon(taxon);
									}, this);
						}
					}
				}, this);

	}

});
