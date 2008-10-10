/*
 * The input for differential expression searches. This form has two main parts: a GeneChooserPanel, and the
 * differential expression search parameters.
 * 
 * Differential expression search has one main setting, the threshold.
 * 
 * If scope=custom, a DatasetSearchField is shown.
 * 
 * @author keshav
 * 
 * @version $Id$
 */

Gemma.MIN_THRESHOLD = 0.00;
Gemma.MAX_THRESHOLD = 1.0;
Gemma.DEFAULT_THRESHOLD = 0.01;

Gemma.DiffExpressionSearchForm = Ext.extend(Ext.Panel, {

	title : "Search configuration",
	layout : 'border',
	defaults : {
		collapsible : true,
		// split : true,
		bodyStyle : "padding:10px"
	},

	height : 330,
	frame : true,
	stateful : true,
	stateEvents : ["beforesearch"],
	stateId : "Gemma.DiffExpressionSearch", // share state with main oage...
	// page...

	applyState : function(state, config) {
		if (state) {
			this.dsc = state;
		}
	},

	getState : function() {
		return this.getDiffSearchCommand();
	},

	afterRender : function(container, position) {
		Gemma.DiffExpressionSearchForm.superclass.afterRender.call(this, container, position);

		Ext.apply(this, {
					loadMask : new Ext.LoadMask(this.getEl(), {
								msg : "Searching  ..."
							})
				});

	},

	restoreState : function() {
		var queryStart = document.URL.indexOf("?");
		if (queryStart > -1) {
			// Ext.log("Loading from url= " + document.URL);
			this.initializeFromQueryString(document.URL.substr(queryStart + 1));
		} else if (this.dsc && queryStart < 0) {
			this.initializeFromDiffSearchCommand(this.dsc);
		}

	},

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
					geneIds : this.geneChooserPanel.getGeneIds(),
					selectedFactors : efMap,
					threshold : Ext.getCmp('thresholdField').getValue(),
					taxonId : this.geneChooserPanel.getTaxonId()
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
	 * Restore state from the URL (e.g., bookmarkable link)
	 * 
	 * @param {}
	 *            query
	 * @return {}
	 */
	getDiffSearchCommandFromQuery : function(query) {
		var param = Ext.urlDecode(query);

		var dsc = {
			geneIds : param.g ? param.g.split(',') : [],
			threshold : param.thres ? param.thres : Gemma.MIN_THRESHOLD,
			eeQuery : param.eeq ? param.eeq : "",
			taxonId : param.t
		};

		if (param.ees) {
			dsc.eeIds = param.ees.split(',');
		}

		if (param.a) {
			dsc.eeSetId = param.a;
		} else {
			dsc.eeSetId = -1;
		}

		if (param.setName) {
			dsc.eeSetName = param.setName;
		}

		// include the factors
		if (param.fm) {
			var fss = param.fm.split(",");
			var factorMap = [];
			for (var i in fss) {
				var fm = fss[i];
				if (typeof fm != 'string') {
					continue;
				}
				var m = fm.split(".");
				if (m.length == 2) {
					factorMap.push({
								eeId : m[0],
								efId : m[1]
							});
				}
			}
			dsc.selectedFactors = factorMap;
			/*
			 * Initialize the chooser panel with this data.
			 */
			this.efChooserPanel.eeFactorsMap = factorMap;
		}
		return dsc;
	},

	initializeFromQueryString : function(query) {
		this.initializeFromDiffSearchCommand(this.getDiffSearchCommandFromQuery(query), true);
	},

	initializeGenes : function(dsc, doSearch) {
		if (dsc.geneIds.length > 1) {
			// load into table.
			this.geneChooserPanel.loadGenes(dsc.geneIds, this.maybeDoSearch.createDelegate(this, [dsc, doSearch]));
		} else {
			// show in combobox.
			this.geneChooserPanel.setGene(dsc.geneIds[0], this.maybeDoSearch.createDelegate(this, [dsc, doSearch]));
		}
	},

	/**
	 * make the form look like it has the right values; this will happen asynchronously... so do the search after it is
	 * done
	 */
	initializeFromDiffSearchCommand : function(dsc, doSearch) {
		this.geneChooserPanel = Ext.getCmp('gene-chooser-panel');
		this.thresholdField = Ext.getCmp('thresholdField');

		if (dsc.taxonId) {
			this.geneChooserPanel.toolbar.taxonCombo.setState(dsc.taxonId);
		}

		this.initializeGenes(dsc, doSearch);

		if (dsc.eeSetId >= 0) {
			this.eeSetChooserPanel.setState(dsc.eeSetId);
		} else if (dsc.eeSetName) {
			this.eeSetChooserPanel.setState(dsc.eeSetName); // FIXME this won't
			// work, expects id.
		}

		if (dsc.threshold) {
			this.thresholdField.setValue(dsc.threshold);
		}

	},

	maybeDoSearch : function(dsc, doit) {
		if (doit) {
			this.doSearch(dsc);
		}
	},

	/**
	 * Show the user interface for choosing factors. This happens asynchronously, so listen for the factors-chosen
	 * event.
	 * 
	 * 
	 */
	chooseFactors : function() {
		if (!this.currentSet) {
			Ext.Msg.alert("Warning", "You must select an expression experiment set before choosing factors");
		} else if (this.currentSet.get("expressionExperimentIds").length == 0) {
			Ext.Msg.alert("Warning", "You should select at least one experiment to analyze");
		} else {
			var eeIds = this.currentSet.get("expressionExperimentIds");
			this.efChooserPanel.show(eeIds);
		}
	},

	/**
	 * Create a URL that can be used to query the system.
	 * 
	 * @param {}
	 *            dsc
	 * @return {}
	 */
	getBookmarkableLink : function(dsc) {
		if (!dsc) {
			dsc = this.getDiffSearchCommand();
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		url += String.format("?g={0}&thres={1}&t={2}", dsc.geneIds.join(","), dsc.threshold, dsc.taxonId);

		if (dsc.eeSetId) {
			url += String.format("&a={0}", dsc.eeSetId);
		}

		if (dsc.eeIds) {
			url += String.format("&ees={0}", dsc.eeIds.join(","));
		}

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

	doSearch : function(dsc) {
		if ((dsc && !dsc.selectedFactors) || (!dsc && !this.efChooserPanel.eeFactorsMap)) {
			this.efChooserPanel.on("factors-chosen", function(efmap) {
						this.doSearch();
					}, this, {
						single : true
					});
			this.chooseFactors();
		} else {
			if (!dsc) {
				dsc = this.getDiffSearchCommand();
			}
			this.clearError();
			var msg = this.validateSearch(dsc);
			if (msg.length === 0) {
				if (this.fireEvent('beforesearch', this, dsc) !== false) {
					this.loadMask.show();
					var errorHandler = this.handleError.createDelegate(this, [], true);
					DifferentialExpressionSearchController.getDiffExpressionForGenes(dsc, {
								callback : this.returnFromSearch.createDelegate(this),
								errorHandler : errorHandler
							});
				}
				if (pageTracker) {
					pageTracker._trackPageview("/Gemma/differentialExpressionSearch.doSearch");
				}
			} else {
				this.handleError(msg);
			}
		}
	},

	handleError : function(msg, e) {
		Ext.DomHelper.overwrite("diffExpression-messages", {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});
		Ext.DomHelper.append("diffExpression-messages", {
					tag : 'span',
					html : "&nbsp;&nbsp;" + msg
				});
		this.loadMask.hide();
	},

	clearError : function() {
		Ext.DomHelper.overwrite("diffExpression-messages", "");
	},

	validateSearch : function(dsc) {
		if (!dsc.geneIds || dsc.geneIds.length === 0) {
			return "We couldn't figure out which gene you want to query. Please use the search functionality to find genes.";
		} else if (dsc.threshold < Gemma.MIN_THRESHOLD) {
			return "Minimum threshold is " + Gemma.MIN_THRESHOLD;
		} else if (dsc.threshold > Gemma.MAX_THRESHOLD) {
			return "Maximum threshold is " + Gemma.MAX_THRESHOLD;
		} else if (dsc.eeIds && dsc.eeIds.length < 1) {
			return "There are no datasets that match your search terms";
		} else if (!dsc.eeIds && !dsc.eeSetId) {
			return "Please select an analysis";
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
	updateDatasetsToBeSearched : function(datasets, eeSet, dirty) {
		var numDatasets = datasets.length;
		Ext.getCmp('thresholdField').maxValue = numDatasets;
		Ext.getCmp('analysis-options-wrapper').setTitle(String.format(
				"Analysis options - Up to {0} datasets will be analyzed", numDatasets));
	},

	getActiveEeIds : function() {
		if (this.currentSet) {
			return this.currentSet.get("expressionExperimentIds");
		}
		return [];
	},

	initComponent : function() {

		var isAdmin = dwr.util.getValue("hasAdmin");

		this.geneChooserPanel = new Gemma.GeneChooserPanel({
					id : 'gene-chooser-panel',
					region : 'center',
					height : 100
				});

		this.eeSetChooserPanel = new Gemma.ExpressionExperimentSetPanel({
					isAdmin : isAdmin
				});

		/* factor chooser */
		this.efChooserPanel = new Gemma.ExperimentalFactorChooserPanel({
					modal : true
				});

		this.geneChooserPanel.on("taxonchanged", function(taxon) {
					this.eeSetChooserPanel.filterByTaxon(taxon);
				}.createDelegate(this));

		this.eeSetChooserPanel.on("set-chosen", function(eeSetRecord) {
					this.currentSet = eeSetRecord;
					this.updateDatasetsToBeSearched(eeSetRecord.get("expressionExperimentIds"), eeSetRecord);
					this.geneChooserPanel.taxonChanged(this.currentSet.get("taxon"));
					this.efChooserPanel.reset(eeSetRecord.get("name"));
				}.createDelegate(this));

		this.eeSetChooserPanel.combo.on("comboReady", this.restoreState.createDelegate(this));

		this.eeSetChooserPanel.on('commit', function(eeSetRecord) {
					if (!eeSetRecord) {
						return;
					}
					this.currentSet = eeSetRecord;
					this.chooseFactors();
				}.createDelegate(this));

		Ext.apply(this, {
					items : [this.geneChooserPanel, {
						xtype : 'panel',
						region : 'south',
						title : 'Analysis options',
						collapsedTitle : '[Analysis options]',
						id : 'analysis-options-wrapper',
						width : 250,
						height : 150,
						minSize : 90,
						cmargins : '5 5 5 5 ',
						margins : '5 5 5 5 ',
						plugins : new Ext.ux.CollapsedPanelTitlePlugin(),
						items : [{
							xtype : 'fieldset',
							defaults : {
								bodyStyle : 'padding:3px'
							},
							id : 'diff-ex-analysis-options',
							autoHeight : true,
							items : [{
								xtype : 'numberfield',
								id : 'thresholdField',
								allowBlank : false,
								allowDecimals : true,
								allowNegative : false,
								minValue : Gemma.MIN_THRESHOLD,
								maxValue : Gemma.MAX_THRESHOLD,
								fieldLabel : 'Threshold',
								invalidText : "Minimum threshold is " + Gemma.MIN_THRESHOLD + ".  Max threshold is "
										+ Gemma.MAX_THRESHOLD,
								value : Gemma.DEFAULT_THRESHOLD,
								width : 60,
								tooltip : "Only genes with a qvalue less than this threshold are returned."
							}, this.eeSetChooserPanel, {
								xtype : 'button',
								id : 'showFactorChooserButton',
								text : "Factor chooser",
								tooltip : "Show experimental factor chooser",
								handler : this.chooseFactors,
								scope : this
							}]
						}]
					}],
					buttons : [{
						text : "Find Differential Expression",
						handler : this.doSearch.createDelegate(this, [], false)
							// pass
							// no
							// parameters!
						}]
				});
		Gemma.DiffExpressionSearchForm.superclass.initComponent.call(this);
		this.addEvents('beforesearch', 'aftersearch');

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
