/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	if (Ext.isIE && Ext.isIE6) {
		Ext.DomHelper.append('analysis-results-search-form', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 7 or newer.'
		});
	}

	var searchPanel = new Gemma.AnalysisResultsSearchForm();

	searchPanel.render("analysis-results-search-form");

	searchPanel.on("beforesearch", function(panel, result) {
		//diffExGrid.loadData(result);
		var link = panel.getBookmarkableLink().replace("mainMenu","searchCoexpression");
		Ext.Msg.alert('Redirecting', 'Using existing coexpression search page to show results');
		window.location = link;
	});

});


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



Gemma.AnalysisResultsSearchForm = Ext.extend(Ext.Panel, {

	layout : 'table',
	layoutConfig:{ columns:8},
	width : 900,
	//height : 200,
	frame : false,
	stateful : false,
	stateEvents : ["beforesearch"],
	taxonComboReady : false,
	eeSetReady : false,
	border:true,
	bodyBorder:false,
	bodyStyle:"backgroundColor:white",
	defaults:{border:false},
	
	
	DEFAULT_STRINGENCY : '2',
	DEFAULT_forceProbeLevelSearch : false,
	DEFAULT_useMyDatasets : false,
	DEFAULT_queryGenesOnly: false,
	

	// share state with main page...
	//stateId : "Gemma.CoexpressionSearch",

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
					//stringency : Ext.getCmp('stringencyfield').getValue(),
					stringency : this.DEFAULT_STRINGENCY,
					forceProbeLevelSearch : this.DEFAULT_forceProbeLevelSearch,
					useMyDatasets : this.DEFAULT_useMyDatasets,
					queryGenesOnly : this.DEFAULT_queryGenesOnly,
					taxonId : this.geneChooserPanel.getTaxonId()
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
			csc.eeSetName = param.an
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
			//Ext.getCmp('stringencyfield').setValue(csc.stringency);
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
			if (typeof pageTracker != 'undefined') {
				pageTracker._trackPageview("/Gemma/coexpressionSearch.doSearch");
			}
		} else {
			this.handleError(msg);
		}
	},

	handleError : function(msg, e) {
		// console.log(e); // this contains the full stack.
/*		Ext.DomHelper.overwrite("analysis-results-search-form-messages", {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});
		Ext.DomHelper.append("analysis-results-search-form-messages", {
					tag : 'span',
					html : "&nbsp;&nbsp;" + msg
				});
*/		this.returnFromSearch({
					errorState : msg
				});

	},

	clearError : function() {
//		Ext.DomHelper.overwrite("analysis-results-search-form-messages", "");
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

		/*if (numDatasets != 0)
			//Ext.getCmp('stringencyfield').maxValue = numDatasets;

		Ext.getCmp('analysis-options').setTitle(String.format("Analysis options - Up to {0} datasets will be analyzed",
				numDatasets));*/
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
	
		this.datasetMembersDataView = new Gemma.ExpressionExperimentDataView({
			id:'datasetMembersDataView'
		});

		this.experimentList = new Ext.Panel({
				height:200,
				width:130,
				id:'experimentPreview',
				layout:'fit',
				items:this.datasetMembersDataView
		});

		this.experimentListText = new Ext.Panel({
				//height:140,
				width:175,
				id:'experimentPreviewText',
				html:'<div style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview</div>',
				//tpl: new Ext.Template('<div><b>{shortName}</b><br>{name}</div>'),
				tpl: new Ext.XTemplate(
				'<tpl for="."><div style="padding-bottom:7px;"><a target="_blank" title="{name}" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
				'{[values.sourceExperiment ? values.sourceExperiment : values.id]}"',
				' ext:qtip="{name}">{shortName}</a>&nbsp; {name}</div></tpl>'),
				tplWriteMode: 'append',
				frame:true
				
		});
						
		//Ext.apply(this.experimentList,{items:this.datasetMembersDataView});
		
		//use this.geneList.update("one line of gene text"); to write to this panel
		this.geneList = new Ext.Panel({
				//height:100,
				width:140,
				id:'genePreview',
				html:'<div style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview</div>',
				tpl: new Ext.Template('<div style="padding-bottom:7px;font-weight:bold;">Gene Selection Preview</div>'+
										'<div style="padding-bottom:7px;"><a href="/Gemma/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName}</div>'),
				//tplWriteMode: 'append', // use this to append to content when calling update instead of replacing
				tplWriteMode: 'overwrite',
				frame:true
		});
		
		


		this.geneChooserPanel = new Gemma.GeneGrid({
					height : 400,
					width : 350,
					id : 'gene-chooser-panel'
				});
	

		Ext.apply(this.geneChooserPanel.getTopToolbar().taxonCombo, {
					stateId : "",
					stateful : false,
					stateEvents : [],
					allTaxa: true
				});
		// reinitialise taxon combo so it has 'alltaxa' option
		this.geneChooserPanel.getTopToolbar().taxonCombo.initComponent();

		/*
		 * Shows the combo box (without 'edit' button)
		 */
		this.eeSetChooserPanel = new Gemma.DatasetGroupComboPanel();

		/*
		 * Filter the EESet chooser based on gene taxon.
		 */
		this.geneChooserPanel.on("taxonchanged", function(taxon) {
					this.eeSetChooserPanel.filterByTaxon(taxon);
				}.createDelegate(this));

		this.eeSetChooserPanel.on("select", function(combo, eeSetRecord, index) {

					if (eeSetRecord === null || eeSetRecord == undefined) {
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
				
		this.eeSetChooserPanel.combo.on('select', function(combo, record, index) {
					this.display(record);
				}, this);
				
		this.geneChooserPanel.getTopToolbar().taxonCombo.on('select', function(combo, record, index) {
					this.eeSetChooserPanel.combo.fireEvent('select');
					//this.display(this.eeSetChooserPanel.combo.getValue);
					//Ext.DomHelper.overwrite(Ext.getCmp('experimentPreviewText').body, {cn: '<div style="padding-bottom:7px;">No Experiments Selected </div>'});
				}, this);
		this.geneChooserPanel.getTopToolbar().taxonCombo.removeListener

		Ext.apply(this, {

			//title : "Search configuration",
			
			/* very hacky! grabbing the elements I need from the genechooserpanel 
			 * and putting them in a different panel so I can control the layout
			 */

			items:[{},{html: 'this taxon', style:'text-align:center;vertical-align:bottom;font-size:15px;'},
					{},{html: 'these experiments', style:'text-align:center;font-size:15px;'},
					{},{html: 'these genes', style:'text-align:center;font-size:15px;padding:0px'},
					{},
					{
						rowspan: 4,
						items: [{html:"<br>",border:false},new Ext.Button({
							text: "Coexpression",style:"font-size:16px!important",
							handler: this.doSearch.createDelegate(this, [], false)
						}), new Ext.Button({
							text: "Differential Expression",
							handler: this.doSearch.createDelegate(this, [], false)
						})]
					},
			
					{html:'Search within ', style:'text-align:center;vertical-align:middle;font-size:18px;'},
					{items: this.geneChooserPanel.getTopToolbar().taxonCombo},
					{html: ' and ', style:'text-align:center;vertical-align:middle;font-size:18px;'},
					{items: this.eeSetChooserPanel.combo},
					{html: ' based on ', style:'text-align:center;vertical-align:middle;font-size:18px;'},
					{items: this.geneChooserPanel.getTopToolbar().geneCombo},
					{html: ' for ',style:'text-align:center;vertical-align:middle;font-size:18px;'},
					
					{},{},{},this.experimentListText,{},this.geneList
					],
		});
		
		Gemma.AnalysisResultsSearchForm.superclass.initComponent.call(this);
		
		this.addEvents('beforesearch', 'aftersearch');		
		
		this.geneChooserPanel.on("addgenes", function(geneids) {
					if (this.geneChooserPanel.getGeneIds().length > 1) {
						//var cmp = Ext.getCmp("querygenesonly");
						//cmp.enable();
					}

				}, this);
				
		this.geneChooserPanel.getTopToolbar().geneCombo.on("selectSingle", function(combo,record,index) {
			//console.log("in response to select"+record.data+", "+record.fields);
			this.geneList.update(record.data);
			if (this.geneChooserPanel.getGeneIds().length > 1) {
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
				
				
		this.doLayout();
	},
	/**
	 * Show the selected eeset members 
	 */

			display : function(record) {
				if (record && this.datasetMembersDataView) {
					this.experimentListText.loadMask= new Ext.LoadMask(this.experimentListText.getEl(), {
						msg : "Loading experiments ..."
					});this.experimentListText.loadMask.show();
					this.datasetMembersDataView.getStore().removeAll();
					this.datasetMembersDataView.getStore().load({
								params : [null,record.get("expressionExperimentIds"),2,null],
								callback: function(r, options, success){
									Ext.DomHelper.overwrite(Ext.getCmp('experimentPreviewText').body, {cn: '<div style="padding-bottom:7px;font-weight:bold;">Experiment Selection Preview </div>'});
									Ext.getCmp('experimentPreviewText').doLayout();
									var limit = (r.size() < 2)? r.size(): 2;
									for(var i =0;i<limit;i++){
										Ext.getCmp('experimentPreviewText').update(r[i].data);
									}
									Ext.DomHelper.append(Ext.getCmp('experimentPreviewText').body, {cn: '<div style="text-align:right"><a>'+(record.get("expressionExperimentIds").size()-limit)+' more<a></div>'});
									Ext.getCmp('experimentPreviewText').loadMask.hide();
								}
							});
					//this.datasetMembersDataView.setTitle(record.get("name"));
		//this.doLayout();
				}
			},

			/**
			 * Clear the ee grid
			 */
			clearDisplay : function() {
				this.datasetMembersDataView.getStore().removeAll();
				this.datasetMembersDataView.setTitle('Set members');
				//this.sourceDatasetsGrid.getTopToolbar().taxonCombo.enable();
			}

});

