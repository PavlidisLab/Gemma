/*
 * The input for differential expression searches. This form has two main parts:
 * a GeneChooserPanel, and the differential expression search parameters.
 * 
 * Differential expression search has one main setting, the threshold.
 * 
 * If scope=custom, a DatasetSearchField is shown.
 * 
 * @author keshav
 * 
 * @version $Id: DiffExpressionSearchForm.js,v 1.25 2008/05/31 00:40:42 kelsey
 *          Exp $
 */

Gemma.MIN_THRESHOLD = 0.01;
Gemma.MAX_THRESHOLD = 1.0;

Gemma.DiffExpressionSearchForm = Ext.extend(Ext.FormPanel, {

	width : 550,
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
		Gemma.DiffExpressionSearchForm.superclass.afterRender.apply(this,
				arguments);

		Ext.apply(this, {
			loadMask : new Ext.LoadMask(this.getEl(), {
				msg : "Searching  ..."
			})
		});

	},

	restoreState : function() {
		var queryStart = document.URL.indexOf("?");
		if (queryStart > -1) {
			Ext.log("Loading from url= " + document.URL);
			this.initializeFromQueryString(document.URL.substr(queryStart + 1));
		} else if (this.dsc && queryStart < 0) {
			this.initializeFromDiffSearchCommand(this.dsc);
		}

	},

	/**
	 * Construct the differential command object from the form, to be sent to
	 * the server.
	 * 
	 * @return {}
	 */
	getDiffSearchCommand : function() {
		var dsc = {
			geneIds : this.geneChooserPanel.getGeneIds(),
			threshold : Ext.getCmp('thresholdField').getValue(),
			taxonId : this.geneChooserPanel.getTaxonId(),
			queryGenesOnly : Ext.getCmp('querygenesonly').getValue()
		};

		if (this.currentSet) {
			dsc.eeIds = this.getActiveEeIds();
			dsc.eeSetName = this.currentSet.get("name");
			dsc.eeSetId = this.currentSet.get("id"); // might
			// be
			// -1.
		} // else a problem.
		return dsc;
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
		var eeQuery = param.eeq || "";
		var ees;
		if (param.ees) {
			ees = param.ees.split(',');
		}

		var dsc = {
			geneIds : param.g ? param.g.split(',') : [],
			threshold : param.t || Gemma.MIN_THRESHOLD ,
			eeQuery : param.eeq,
			taxonId : param.t
		};
		if (param.q) {
			dsc.queryGenesOnly = true;
		}

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

		return dsc;
	},

	initializeFromQueryString : function(query) {
		this.initializeFromDiffSearchCommand(this
				.getDiffSearchCommandFromQuery(query), true);
	},

	initializeGenes : function(dsc, doSearch) {
		if (dsc.geneIds.length > 1) {
			// load into table.
			this.geneChooserPanel.loadGenes(dsc.geneIds, this.maybeDoSearch
					.createDelegate(this, [dsc, doSearch]));
		} else {
			// show in combobox.
			this.geneChooserPanel.setGene(dsc.geneIds[0], this.maybeDoSearch
					.createDelegate(this, [dsc, doSearch]));
		}
	},

	/**
	 * make the form look like it has the right values; this will happen
	 * asynchronously... so do the search after it is done
	 */
	initializeFromDiffSearchCommand : function(dsc, doSearch) {
		this.geneChooserPanel = Ext.getCmp('gene-chooser-panel');
		this.thresholdField = Ext.getCmp('thresholdField');

		if (dsc.taxonId) {
			this.geneChooserPanel.taxonCombo.setState(dsc.taxonId);
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

		if (dsc.queryGenesOnly) {
			this.queryGenesOnly.setValue(true);
		}
	},

	maybeDoSearch : function(dsc, doit) {
		if (doit) {
			this.doSearch(dsc);
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
		var url = queryStart > -1
				? document.URL.substr(0, queryStart)
				: document.URL;
		url += String.format("?g={0}&s={1}&t={2}", dsc.geneIds.join(","),
				dsc.threshold, dsc.taxonId);
		if (dsc.queryGenesOnly) {
			url += "&q";
		}

		if (dsc.eeSetId) {
			url += String.format("&a={0}", dsc.eeSetId);
		}

		if (dsc.eeIds) {
			url += String.format("&ees={0}", dsc.eeIds.join(","));
		}

		if (dsc.eeSetName) {
			url += String.format("&setName={0}", dsc.eeSetName);
		}
		return url;
	},

	doSearch : function(dsc) {
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
		} else {
			this.handleError(msg);
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
		if (dsc.queryGenesOnly && dsc.geneIds.length < 2) {
			return "You must select more than one query gene to use 'search among query genes only'";
		} else if (!dsc.geneIds || dsc.geneIds.length === 0) {
			return "We couldn't figure out which gene you want to query. Please use the search functionality to find genes.";
		}  else if (dsc.threshold < Gemma.MIN_THRESHOLD) {
			return "Minimum threshold is "
					+ Gemma.MIN_THRESHOLD;
		} else if (dsc.threshold > Gemma.MAX_THRESHOLD) {
			return "Maximum threshold is "
					+ Gemma.MAX_THRESHOLD;
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
	 *            eeSet The ExpressionExperimentSet that was used (if any) - it
	 *            could be just as a starting point.
	 */
	updateDatasetsToBeSearched : function(datasets, eeSet, dirty) {
		var numDatasets = datasets.length;
		Ext.getCmp('thresholdField').maxValue = numDatasets;
		Ext.getCmp('analysis-options').setTitle(String.format(
				"Analysis options - Up to {0} datasets will be analyzed",
				numDatasets));
	},

	getActiveEeIds : function() {
		if (this.currentSet) {
			return this.currentSet.get("expressionExperimentIds");
		}
		return [];
	},

	initComponent : function() {

		this.geneChooserPanel = new Gemma.GeneChooserPanel({
			id : 'gene-chooser-panel'
		});

		this.eeSetChooserPanel = new Gemma.ExpressionExperimentSetPanel({
			fieldLabel : "Query scope"
		});

		this.geneChooserPanel.on("taxonchanged", function(taxon) {
			this.eeSetChooserPanel.filterByTaxon(taxon);
		}.createDelegate(this));

		this.eeSetChooserPanel.on("set-chosen", function(eeSetRecord) {
			this.currentSet = eeSetRecord;
			this.updateDatasetsToBeSearched(eeSetRecord
					.get("expressionExperimentIds"), eeSetRecord);
			this.geneChooserPanel.taxonChanged(this.currentSet.get("taxon"));
		}.createDelegate(this));

		this.eeSetChooserPanel.store.on("ready", this.restoreState
				.createDelegate(this));

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
						id : 'thresholdField',
						allowBlank : false,
						allowDecimals : false,
						allowNegative : false,
						minValue : Gemma.MIN_THRESHOLD,
						maxValue : Gemma.MAX_THRESHOLD,
						fieldLabel : 'Threshold',
						invalidText : "Minimum threshold is "
								+ Gemma.MIN_THRESHOLD +".  Max threshold is " + Gemma.MAX_THRESHOLD, 
						value : Gemma.MIN_THRESHOLD,
						width : 60,
						tooltip : "Only genes with a qvalue less than this threshold are returned."
					}, {
						xtype : 'checkbox',
						id : 'querygenesonly',
						fieldLabel : 'My genes only',
						tooltip : "Restrict the output to include only links among the listed query genes"
					}, this.eeSetChooserPanel]
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

	}

});
