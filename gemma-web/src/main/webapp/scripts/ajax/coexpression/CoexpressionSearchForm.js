/*
 * The input for coexpression searches. This form has two main parts: a
 * GeneChooserPanel, and the coexpression search parameters.
 * 
 * Coexpression search has three main settings, plus an optional part that
 * appears if the user is doing a 'custom' analysis: Stringency, "Among query
 * genes" checkbox, and the "scope".
 * 
 * If scope=custom, a DatasetSearchField is shown.
 * 
 * @authors Luke, Paul
 * 
 * @version $Id$
 */

Ext.Gemma.MIN_STRINGENCY = 2;

Ext.Gemma.CoexpressionSearchForm = Ext.extend(Ext.FormPanel, {

	width : 550,
	frame : true,
	stateful : true,
	stateEvents : ["beforesearch"],
	stateId : "Ext.Gemma.CoexpressionSearch", // share state with main
	// page...

	applyState : function(state, config) {
		if (state) {
			this.csc = state;
		}
	},

	getState : function() {
		return this.getCoexpressionSearchCommand();
	},

	afterRender : function(container, position) {
		Ext.Gemma.CoexpressionSearchForm.superclass.afterRender.apply(this,
				arguments);

		Ext.apply(this, {
			loadMask : new Ext.LoadMask(this.getEl(), {
				msg : "Searching  ..."
			})
		});

	},

	restoreState : function() {

		var queryStart = document.URL.indexOf("?");
		Ext.log("restoring state, url= " + document.URL);

		if (queryStart > -1) {
			this.initializeFromQueryString(document.URL.substr(queryStart + 1));
		} else if (this.csc && queryStart < 0) {
			this.initializeFromCoexpressionSearchCommand(this.csc);
		}

	},

	/**
	 * Construct the coexpression command object from the form, to be sent to
	 * the server.
	 * 
	 * @return {}
	 */
	getCoexpressionSearchCommand : function() {
		var csc = {
			geneIds : this.geneChooserPanel.getGeneIds(),
			stringency : Ext.getCmp('stringencyfield').getValue(),
			taxonId : this.geneChooserPanel.getTaxonId(),
			queryGenesOnly : Ext.getCmp('querygenesonly').getValue()
		};
		csc.eeIds = this.eeIds;
		csc.cannedAnalysisId = this.analysisCombo.getValue();
		return csc;
	},

	/**
	 * Restore state from the URL (e.g., bookmarkable link)
	 * 
	 * @param {}
	 *            query
	 * @return {}
	 */
	getCoexpressionSearchCommandFromQuery : function(query) {
		Ext.log("Parse url");
		var param = Ext.urlDecode(query);
		var eeQuery = param.eeq || "";
		var ees;
		if (param.ees) {
			ees = param.ees.split(',');
		}

		var csc = {
			geneIds : param.g ? param.g.split(',') : [],
			stringency : param.s || Ext.Gemma.MIN_STRINGENCY,
			eeQuery : param.eeq,
			taxonId : param.t
		};
		if (param.q) {
			csc.queryGenesOnly = true;
		}
		if (param.ees) {
			csc.eeIds = param.ees.split(',');
			csc.cannedAnalysisId = -1;
		} else {
			csc.cannedAnalysisId = param.a;
		}

		return csc;
	},

	initializeFromQueryString : function(query) {
		this.initializeFromCoexpressionSearchCommand(this
				.getCoexpressionSearchCommandFromQuery(query), true);
	},

	initializeGenes : function(csc, doSearch) {
		if (csc.geneIds.length > 1) {
			this.geneChooserPanel.loadGenes(csc.geneIds, this.maybeDoSearch
					.createDelegate(this), doSearch);
		} else {
			this.geneChooserPanel.setGene(csc.geneIds[0], this.maybeDoSearch
					.createDelegate(this), doSearch);
		}
	},

	/**
	 * make the form look like it has the right values; this will happen
	 * asynchronously... so do the search after it is done
	 */
	initializeFromCoexpressionSearchCommand : function(csc, doSearch) {
		Ext.log("initialize");
		this.geneChooserPanel = Ext.getCmp('gene-chooser-panel');
		this.stringencyField = Ext.getCmp('stringencyfield');
		// this.eeSearchField = Ext.getCmp('eeSearchField');

		this.initializeGenes(csc, doSearch);

		if (csc.taxonId) {
			this.geneChooserPanel.taxonCombo.setState(csc.taxonId);
		}

		if (csc.cannedAnalysisId) {
			this.analysisCombo.setState(csc.cannedAnalysisId);
		}

		if (csc.stringency) {
			this.stringencyField.setValue(csc.stringency);
		}

		if (csc.queryGenesOnly) {
			this.queryGenesOnly.setValue(true);
		}

		if (csc.cannedAnalysisId === null || csc.cannedAnalysisId < 0) {
			this.updateDatasetsToBeSearched(csc.eeIds, csc.cannedAnalysisId,
					false);
		} else {

		}

	},

	maybeDoSearch : function(doit) {
		Ext.log("dosearch");
		if (doit) {
			this.doSearch();
		}
	},

	getBookmarkableLink : function(csc) {
		if (!csc) {
			csc = this.getCoexpressionSearchCommand();
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1
				? document.URL.substr(0, queryStart)
				: document.URL;
		url += String.format("?g={0}&s={1}&t={2}", csc.geneIds.join(","),
				csc.stringency, csc.taxonId);
		if (csc.queryGenesOnly) {
			url += "&q";
		}
		if (csc.eeIds) {
			url += String.format("&ees={0}", csc.eeIds.join(","));
		} else {
			url += String.format("&a={0}", csc.cannedAnalysisId);
		}

		if (csc.eeQuery) {
			url += "&eeq=" + csc.eeQuery;
		}

		return url;
	},

	doSearch : function() {

		var csc = this.getCoexpressionSearchCommand();

		this.clearError();
		var msg = this.validateSearch(csc);
		if (msg.length === 0) {
			if (this.fireEvent('beforesearch', this, csc) !== false) {
				this.loadMask.show();
				var errorHandler = this.handleError.createDelegate(this, [],
						true);
				ExtCoexpressionSearchController.doSearch(csc, {
					callback : this.returnFromSearch.createDelegate(this),
					errorHandler : errorHandler
				});
			}
		} else {
			this.handleError(msg);
		}
	},

	chooseDatasets : function() {
		if (!this.dcp) {
			this.dcp = new Ext.Gemma.DatasetChooserPanel();
			this.dcp.on("datasets-selected", function(e) {
				this.updateDatasetsToBeSearched(e.eeIds, e.eeSet, e.dirty);
			}, this);
		}
		this.eeSetViewButton.disable();
		this.dcp.show({
			eeSet : this.currentSet || this.analysisCombo.getAnalysis().id,
			eeIds : this.eeIds
		});

	},

	handleError : function(msg, e) {
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
		} else if (csc.stringency < Ext.Gemma.MIN_STRINGENCY) {
			return "Minimum stringency is " + Ext.Gemma.MIN_STRINGENCY;
		} else if (csc.eeIds && csc.eeIds.length < 1) {
			return "There are no datasets that match your search terms";
		} else if (!csc.eeIds && !csc.cannedAnalysisId) {
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
	 *            datasets The selected datasets
	 * @param {}
	 *            eeSet The ExpressionExperimentSet that was used (if any) - it
	 *            could be just as a starting point.
	 */
	updateDatasetsToBeSearched : function(datasets, eeSet, dirty) {

		if (datasets) {
			var numDatasets = datasets.length;
			Ext.getCmp('stringencyfield').maxValue = numDatasets;
			this.eeIds = datasets;
			Ext.getCmp('analysis-options').setTitle(String.format(
					"Analysis options - Up to {1} dataset{2} will be analyzed",
					datasets.toString(), numDatasets, numDatasets != 1
							? "s"
							: ""));
		}

		if (eeSet) {
			Ext.log("Got eeSet: " + eeSet);

			this.currentSet = eeSet;
			if (dirty) {
				this.analysisCombo.showCustom(true);
			} else {
				this.analysisCombo.setValue(eeSet);
			}
		} else {
			// FIXME keep this from happening when nothing is selected yet.
			Ext.log("Custom");
			this.analysisCombo.showCustom(true);
		}
		this.eeSetViewButton.enable();

	},

	taxonChanged : function(taxon) {
		if (!taxon) {
			return;
		}
		Ext.getCmp('analysis-select').taxonChanged(taxon);
		// this.eeSearchField.taxonChanged(taxon, false); // don't automatically
		// update the field.
		this.geneChooserPanel.taxonChanged(taxon); // endless loop if we're not
		// careful.
	},

	analysisChanged : function(analysis) {
		if (!analysis) {
			return;
		}
		this.currentSet = analysis.data.id;
		this.taxonChanged(analysis.data.taxon);
		this
				.updateDatasetsToBeSearched(analysis.data.datasets,
						this.currentSet);
	},

	getActiveEeIds : function() {
		return this.eeIds;
	},

	initComponent : function() {

		this.geneChooserPanel = new Ext.Gemma.GeneChooserPanel({
			id : 'gene-chooser-panel',
			listeners : {
				"taxonchanged" : {
					fn : this.taxonChanged.createDelegate(this)
				}
			}
		});

		this.analysisCombo = new Ext.Gemma.AnalysisCombo({
			fieldLabel : 'Search scope',
			id : 'analysis-select',
			showCustomOption : false,
			tooltip : "Restrict the list of datasets that will be searched for coexpression"
		});

		this.eeSetViewButton = new Ext.Button({
			text : "View/Edit datasets",
			handler : this.chooseDatasets.createDelegate(this)
		});

		this.analysisCombo
				.on('analysischanged', this.analysisChanged.createDelegate(
						this, [this.analysisCombo.getAnalysis()], true));

		this.analysisCombo.on('ready', this.restoreState.createDelegate(this));

		Ext.apply(this, {

			items : [{
				xtype : 'fieldset',
				title : 'Query gene(s)',
				autoHeight : true,
				items : [this.geneChooserPanel]
			}, {
				xtype : 'panel',
				title : 'Analysis options',
				id : 'analysis-options',
				autoHeight : true,
				items : [{
					xtype : 'fieldset',
					autoHeight : true,
					items : [{
						xtype : 'numberfield',
						id : 'stringencyfield',
						allowBlank : false,
						allowDecimals : false,
						allowNegative : false,
						minValue : Ext.Gemma.MIN_STRINGENCY,
						maxValue : 999,
						fieldLabel : 'Stringency',
						invalidText : "Minimum stringency is "
								+ Ext.Gemma.MIN_STRINGENCY,
						value : 2,
						width : 60,
						tooltip : "The minimum number of datasets that must show coexpression for a result to appear"
					}, {
						xtype : 'checkbox',
						id : 'querygenesonly',
						fieldLabel : 'My genes only',
						tooltip : "Restrict the output to include only links among the listed query genes"
					}, this.analysisCombo, this.eeSetViewButton]
				}]
			}],
			buttons : [{
				text : "Find coexpressed genes",
				handler : this.doSearch.createDelegate(this, [], true)
			}]
		});
		Ext.Gemma.CoexpressionSearchForm.superclass.initComponent.call(this);
		this.addEvents('beforesearch', 'aftersearch');
	}

});
