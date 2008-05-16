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

		// initialize from state
		if (this.csc) {
			this.initializeFromCoexpressionSearchCommand(this.csc);
		}

		// intialize from URL (overrides state)
		var queryStart = document.URL.indexOf("?");
		if (queryStart > -1) {
			this.initializeFromQueryString(document.URL.substr(queryStart + 1));
		}
	},

	getCoexpressionSearchCommand : function() {
		var csc = {
			geneIds : this.geneChooserPanel.getGeneIds(),
			stringency : Ext.getCmp('stringencyfield').getValue(),
			taxonId : this.geneChooserPanel.getTaxonId(),
			queryGenesOnly : Ext.getCmp('querygenesonly').getValue()
		};
		var analysisId = this.analysisCombo.getValue();
		if (analysisId < 0) {
			csc.eeIds = Ext.getCmp('eeSearchField').getEeIds();
			csc.eeQuery = Ext.getCmp('eeSearchField').getValue();
		} else {
			csc.cannedAnalysisId = analysisId;
		}
		return csc;
	},

	getCoexpressionSearchCommandFromQuery : function(query) {
		var param = Ext.urlDecode(query);
		var eeQuery = param.eeq || "";
		var ees;
		if (param.ees) {
			ees = param.ees.split(',');
		}

		var csc = {
			geneIds : param.g ? param.g.split(',') : [],
			stringency : param.s || Ext.Gemma.MIN_STRINGENCY,
			eeQuery : param.eeq
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

	initializeFromCoexpressionSearchCommand : function(csc, doSearch) {

		this.geneChooserPanel = Ext.getCmp('gene-chooser-panel');
		this.stringencyField = Ext.getCmp('stringencyfield');
		this.eeSearchField = Ext.getCmp('eeSearchField');

		/*
		 * make the form look like it has the right values; this will happen
		 * asynchronously...
		 */
		if (csc.taxonId) {
			this.geneChooserPanel.taxonCombo.setState(csc.taxonId);
		}
		if (csc.geneIds.length > 1) {
			this.geneChooserPanel.loadGenes(csc.geneIds);
		} else {
			this.geneChooserPanel.setGene(csc.geneIds[0]);
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
			this.customFs.show();
			this.eeSearchField.setValue(csc.eeQuery);
			this.updateDatasetsToBeSearched(csc.eeIds);
		} else {

		}

		/*
		 * perform the search with the specified values...
		 */
		if (doSearch) {
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
		url += String.format("?g={0}&s={1}", csc.geneIds.join(","),
				csc.stringency);
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

	showSelectedDatasets : function(eeids) {

		// Window shown when the user wants to see the experiments that are 'in
		// play'.

		if (!this.activeDatasetsWindow) {

			this.activeDatasetsGrid = new Ext.Gemma.ExpressionExperimentGrid({
				readMethod : ExpressionExperimentController.loadExpressionExperiments
						.createDelegate(this),
				editable : false,
				rowExpander : true,
				pageSize : 20
			});

			this.activeDatasetsWindow = new Ext.Window({
				title : eeids.size() + " active datasets",
				modal : true,
				layout : 'fit',
				autoHeight : true,
				width : 600,
				closeAction : 'hide',
				easing : 3,
				items : [this.activeDatasetsGrid],
				buttons : [{
					text : 'Close',
					handler : function() {
						this.hide.createDelegate(this.activeDatasetsWindow)
					}
				}]

			});
		}
		this.activeDatasetsGrid.getStore().removeAll();
		this.activeDatasetsGrid.expandedElements = [];
		this.activeDatasetsGrid.getStore().load({
			params : [eeids]
		});
		this.activeDatasetsWindow.show();
	},

	chooseDatasets : function() {
		if (!this.dcp) {
			this.dcp = new Ext.Gemma.DatasetChooserPanel();
			this.dcp.on("datasets-selected", function(e) {
				this.updateDatasetsToBeSearched(e.eeIds);
			}, this);
		}
		// todo: provide the current datasets.
		this.dcp.show( /* current datasets or analysis */);
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
		} else if (!csc.geneIds || csc.geneIds.length == 0) {
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

	updateDatasetsToBeSearched : function(datasets) {
		var numDatasets = datasets instanceof Array
				? datasets.length
				: datasets;
		Ext.getCmp('stringencyfield').maxValue = numDatasets;
		if (datasets instanceof Array) {
			this.eeIds = datasets;
		}

		Ext
				.getCmp('analysis-options')
				.setTitle(String
						.format(
								"Analysis options - Up to <a title='Click here to see dataset details' onclick='Ext.Gemma.CoexpressionSearchForm.showSelectedDatasets([{0}]);'>  {1} dataset{2} </a>  will be analyzed",
								datasets.toString(), numDatasets,
								numDatasets != 1 ? "s" : ""));
	},

	taxonChanged : function(taxon) {
		if (!taxon) {
			return;
		}
		Ext.getCmp('analysis-select').taxonChanged(taxon);
		this.eeSearchField.taxonChanged(taxon, false); // don't automatically
		// update the field.
		this.geneChooserPanel.taxonChanged(taxon); // endless loop if we're not
		// careful.
	},

	analysisChanged : function(analysis) {
		if (!analysis) {
			return;
		}
		if (analysis.data.id < 0) { // custom
			// analysis
			this.customAnalysis = true;
			Ext.getCmp('custom-analysis-options').show();
			this.updateDatasetsToBeSearched(this.eeSearchField.getEeIds());
			// this.eeSearchField.findDatasets(); // should we do this?
		} else {
			this.customAnalysis = false;
			Ext.getCmp('custom-analysis-options').hide();
			this.taxonChanged(analysis.data.taxon);
			this.updateDatasetsToBeSearched(analysis.data.datasets);
		}
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
			showCustomOption : true,
			tooltip : "Restrict the list of datasets that will be searched for coexpression"
		});

		this.analysisCombo
				.on('analysischanged', this.analysisChanged.createDelegate(
						this, [this.analysisCombo.getAnalysis()], true));

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
					}, this.analysisCombo, {
						xtype : 'fieldset',
						id : 'custom-analysis-options',
						title : 'Custom analysis options',
						autoHeight : true,
						hidden : true,
						autoWidth : true,
						items : [{
							xtype : 'datasetsearchfield',
							id : 'eeSearchField',
							fieldLabel : "Experiment keywords",
							tooltip : "Search only datasets that match these keywords",
							listeners : {
								'aftersearch' : {
									fn : function(field, results) {
										if (this.customAnalysis) {
											this
													.updateDatasetsToBeSearched(results);
										}
									}.createDelegate(this)
								}
							}

						}, {
							xtype : 'button',
							text : "Choose datasets interactively",
							handler : this.chooseDatasets.createDelegate(this)
						}]
					}]
				}]
			}],
			buttons : [{
				text : "Find coexpressed genes",
				handler : this.doSearch.createDelegate(this, [], true)
			}]
		});
		Ext.Gemma.CoexpressionSearchForm.superclass.initComponent.call(this);
		this.addEvents('beforesearch', 'aftersearch');

		// var stringencySpinner = new Ext.ux.form.Spinner({
		// renderTo : this.stringencyField.getEl(),
		// strategy: new Ext.ux.form.Spinner.NumberStrategy({
		// allowDecimals : false, minValue:2, maxValue:100 })
		// });
		// this.stringencySpinner = stringencySpinner;
	}

});
