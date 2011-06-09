Ext.namespace('Gemma');

Gemma.MetaHeatmapDataSelection = Ext.extend(Ext.Panel, {

	constructor : function(searchCommand) {
		Ext.apply(this, {
			param : searchCommand.ownerCt
				// get input from front-page search
			});
		Gemma.MetaHeatmapDataSelection.superclass.constructor.call(this);
	},

	_selectedDatasetGroups : [],
	_selectedGeneGroups : [],
	waitingForGeneSessionGroupBinding : false,
	waitingForDatasetSessionGroupBinding : false,
	geneGroupNames : [],
	geneGroupReferences : [],
	datasetGroupNames : [],
	datasetGroupReferences : [],
	taxonId : null,

	prepareVisualization : function(target) {

		this.geneGroupNames = [];
		this.geneGroupReferences = [];
		this.datasetGroupNames = [];
		this.datasetGroupReferences = [];
		this._selectedGeneGroups = [];
		this._selectedDatasetGroups = [];
		// control variables used if asynchronous session group creation is
		// performed
		this.waitingForGeneSessionGroupBinding = false;
		this.waitingForDatasetSessionGroupBinding = false;

		var i;
		// populate list of selected groups
		for (i = 0; i < this._geneCombos.length; i++) {
			this._selectedGeneGroups.push(this._geneCombos[i].getGeneGroup());
		}

		for (i = 0; i < this._datasetCombos.length; i++) {
			this._selectedDatasetGroups.push(this._datasetCombos[i].getSelected());
		}

		var geneGroupsToBindToSession = [];

		for (i = 0; i < this._selectedGeneGroups.length; i++) {

			if (this._selectedGeneGroups[i] && this._selectedGeneGroups[i] !== null) {
				// if the group has a null value for reference.id, then it
				// hasn't been
				// created as a group in the database nor session
				if (this._selectedGeneGroups[i].reference.id === null) {
					this._selectedGeneGroups[i].geneIds = this._selectedGeneGroups[i].memberIds;
					geneGroupsToBindToSession.push(this._selectedGeneGroups[i]);
				} else {
					this.geneGroupReferences.push(this._selectedGeneGroups[i].reference);
					this.geneGroupNames.push(this._selectedGeneGroups[i].name);
				}
			}
		}
		var j;
		if (geneGroupsToBindToSession.length !== 0) {
			this.waitingForGeneSessionGroupBinding = true;
			GeneSetController.addNonModificationBasedSessionBoundGroups(geneGroupsToBindToSession, function(geneSets) {
						// should be at least one geneset
						if (geneSets === null || geneSets.length === 0) {
							// TODO error message
							return;
						} else {
							for (j = 0; j < geneSets.length; j++) {
								this.geneGroupReferences.push(geneSets[j].reference);
								this.geneGroupNames.push(geneSets[j].name);
							}
						}
						this.waitingForGeneSessionGroupBinding = false;
						this.fireEvent('geneGroupsReadyForVisualization');
					}.createDelegate(this));
		}

		var datasetGroupsToBindToSession = [];
		for (i = 0; i < this._selectedDatasetGroups.length; i++) {
			if (this._selectedDatasetGroups[i] && this._selectedDatasetGroups[i] !== null) {
				// if the group has a null value for reference.id, then it
				// hasn't been
				// created as a group in the database nor session
				if (this._selectedDatasetGroups[i].reference.id === null) {
					this._selectedDatasetGroups[i].expressionExperimentIds = this._selectedDatasetGroups[i].memberIds;
					datasetGroupsToBindToSession.push(this._selectedDatasetGroups[i]);
				} else {
					this.datasetGroupReferences.push(this._selectedDatasetGroups[i].reference);
					this.datasetGroupNames.push(this._selectedDatasetGroups[i].name);
				}
			}
		}
		if (datasetGroupsToBindToSession.length !== 0) {
			this.waitingForDatasetSessionGroupBinding = true;
			ExpressionExperimentSetController.addNonModificationBasedSessionBoundGroups(datasetGroupsToBindToSession,
					function(datasetSets) {
						// should be at least one datasetSet
						if (datasetSets === null || datasetSets.length === 0) {
							// TODO error message
							return;
						} else {
							for (j = 0; j < datasetSets.length; j++) {
								this.datasetGroupReferences.push(datasetSets[j].reference);
								this.datasetGroupNames.push(datasetSets[j].name);
							}
						}
						this.waitingForDatasetSessionGroupBinding = false;
						this.fireEvent('datasetGroupsReadyForVisualization');
					}.createDelegate(this));
		}

		// if no asynchronous calls were made, run visualization right away
		// otherwise, doVisualization will be triggered by events
		if (!this.waitingForDatasetSessionGroupBinding && !this.waitingForGeneSessionGroupBinding) {
			this.doVisualization(null);
		}
	},
	doVisualization : function() {
		var estimatedTime = 15 * this.geneGroupReferences.length * this.datasetGroupReferences.length;

		var progressWindow = new Ext.Window({
					width : 400,
					height : 55,
					// title: "Estimated time: " + (estimatedTime / 1000) + "s",
					title : "Loading",
					items : [{
								xtype : 'progress',
								ref : 'progress_bar'
							}]
				});
		// progressWindow.show();
		progressWindow.progress_bar.wait({
					interval : 1000,
					duration : estimatedTime,
					increment : estimatedTime / 1000,
					text : 'Building visualization...',
					scope : this,
					fn : function() {
					}
				});
		this._selectedDatasetGroups = [];
		this._selectedGeneGroups = [];

		if (!this.taxonId || this.taxonId === null) {
			this.taxonId = this._taxonCombo.getSelected().id;
		}

		DifferentialExpressionSearchController.differentialExpressionAnalysisVisualizationSearch(this.taxonId,
				this.datasetGroupReferences, this.geneGroupReferences, function(data) {

					progressWindow.hide();
					if (typeof this.ownerCt !== 'undefined') {
						// to trigger loadmask on search form to hide
						this.ownerCt.fireEvent('visualizationLoaded');
					} 

					data.geneGroupNames = this.geneGroupNames;
					data.datasetGroupNames = this.datasetGroupNames;
					var experimentCount = 0;
					var lastDatasetId = null;

					var i;
					var j;
					for (i = 0; i < data.resultSetValueObjects.length; i++) {
						for (j = 0; j < data.resultSetValueObjects[i].length; j++) {

							// get the number of experiments, they should be
							// grouped by datasetId
							if (data.resultSetValueObjects[i][j].datasetId != lastDatasetId) {
								experimentCount++;
							}
							lastDatasetId = data.resultSetValueObjects[i][j].datasetId;
						}
					}					

					// if no experiments were returned, don't show visualizer
					if (experimentCount === 0) {
						Ext.DomHelper.overwrite('meta-heatmap-div', {
							html : '<img src="/Gemma/images/icons/warning.png"/> Sorry, no data available for your search.'
						});
					} else {
						_metaVizApp = new Gemma.MetaHeatmapApp({
									tbarTitle : '<b>Differential Expression Visualisation</b> (Data available for ' +
											experimentCount + ' of ' + this.param.datasetCount + ' experiments)',
									visualizationData : data,
									applyTo : 'meta-heatmap-div',
									pvalue : this.param.pvalue
								});
						_metaVizApp.doLayout();
						_metaVizApp.refreshVisualization();
					}

				}.createDelegate(this));
	},
	initComponent : function() {

		this.on('geneGroupsReadyForVisualization', function(geneReferences, geneNames) {
					if (!this.waitingForDatasetSessionGroupBinding) {
						this.doVisualization();
					}
				}, this);
		this.on('datasetGroupsReadyForVisualization', function() {
					if (!this.waitingForGeneSessionGroupBinding) {
						this.doVisualization();
					}
				}, this);
		Ext.apply(this, {
					_geneCombos : [],
					_datasetCombos : [],
					_metaVizApp : null,
					_sortPanel : null,
					height : 450,
					layout : 'fit',
					items : [{
								xtype : 'taxonCombo',
								width : 200,
								ref : '_taxonCombo'
							}, {
								xtype : 'panel',
								width : 200,
								height : 100,
								ref : 'genePickerPanel',
								items : [{
											xtype : 'button',
											text : 'Add gene group',
											width : 200,
											ref : 'addNewGeneGroupButton',
											listeners : {
												click : {
													fn : function(target) {
														// var genePicker = new
														// Gemma.GeneGroupCombo({width
														// : 200});
														// genePicker.setTaxon(this._taxonCombo.getTaxon());
														var genePicker = new Gemma.GeneAndGeneGroupCombo({
																	width : 200
																});
														genePicker.setTaxonId(this._taxonCombo.getTaxon().id);
														this._geneCombos.push(genePicker);
														this.genePickerPanel.add(genePicker);
														this.genePickerPanel.doLayout();
													},
													scope : this
												}
											}
										}]

							}, {
								xtype : 'panel',
								width : 200,
								height : 100,
								ref : 'datasetPickerPanel',
								items : [{
											xtype : 'button',
											text : 'Add dataset group',
											width : 200,
											ref : 'addNewDatasetGroupButton',
											listeners : {
												click : {
													fn : function(target) {
														// var datasetPicker =
														// new
														// Gemma.DatasetGroupCombo({width
														// : 200});
														var datasetPicker = new Gemma.ExperimentAndExperimentGroupCombo(
																{
																	width : 200
																});
														this._datasetCombos.push(datasetPicker);
														this.datasetPickerPanel.add(datasetPicker);
														this.datasetPickerPanel.doLayout();
													},
													scope : this
												}
											}
										}]
							}, {
								xtype : 'button',
								text : 'Visualize!',
								ref : 'goButton',
								width : 50,
								height : 30,
								listeners : {
									click : {
										fn : this.prepareVisualization,
										scope : this
									}
								}
							}]
				});
		// FOR TESTING !!!!!
		this.param2 = {
			geneReferences : [{
				id : 96,
				type : "databaseBackedGroup"
			}
				 ,{ id: 94, type: "databaseBackedGroup" }
				 
			],
			datasetReferences : [{
						id : 6137,
						type : "databaseBackedGroup"
					}
			
			  ,{ id: 6110, type: "databaseBackedGroup" }
			 
			],
			geneNames : ["gene TEST", "gene TEST 2"],
			datasetNames : ["dataset TEST", "dataset TEST2"],
			taxonId : 2,
			pvalue : Gemma.DEFAULT_THRESHOLD
		};

		if (this.param) {
			if (this.param.geneReferences) {
				this.geneGroupReferences = this.param.geneReferences;
			}
			if (this.param.geneNames) {
				this.geneGroupNames = this.param.geneNames;
			}
			if (this.param.datasetReferences) {
				this.datasetGroupReferences = this.param.datasetReferences;
			}
			if (this.param.datasetNames) {
				this.datasetGroupNames = this.param.datasetNames;
			}
			if (this.param.datasetCount) {
				this.datasetsSelectedCount = this.param.datasetCount;
			}
			if (this.param.taxonId) {
				this.taxonId = this.param.taxonId;
			}
			if (this.param.taxonName) {
				this.taxonName = this.param.taxonName;
			}
		}

		Gemma.MetaHeatmapDataSelection.superclass.initComponent.apply(this, arguments);

		if (this.param && this.param.geneReferences && this.param.geneNames && this.param.datasetReferences &&
			this.param.datasetNames && this.param.taxonId) {
			this.doVisualization();
		}
	},
	onRender : function() {
		Gemma.MetaHeatmapDataSelection.superclass.onRender.apply(this, arguments);
		// var myNodes = [{text: 'parent1', children: [{text: 'child1'}]},
		// {text: 'parent2', children:[]},{text: 'parent1', children: [{text:
		// 'child2'}]}];
		// var tree = new Gemma.FactorSelectTree(myNodes);

		_sortPanel = this.ownerCt.sortPanel;
	}
});

Ext.reg('metaVizDataSelection', Gemma.MetaHeatmapDataSelection);
