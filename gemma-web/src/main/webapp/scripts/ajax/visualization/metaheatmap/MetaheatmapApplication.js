/**
 * @version $Id$
 * @author AZ
 */
Ext.namespace('Gemma');

/**
 * MetaHeatmap Application Consist of 3 main panels:
 * 
 * <pre>
 * - gene labels 
 * - anlaysis labels 
 * - main visualization area 
 * </pre>
 * 
 * It is controlled by window that allows sorting/filtering and choosing data.
 * 
 */
Gemma.MetaHeatmapApp = Ext.extend(Ext.Panel, {
	autoScroll : false,
	initComponent : function() {

		// build data structure for filtering factor tree
		this.treeData = [];
		var i;
		var j;
		var k;
		for (i = 0; i < this.visualizationData.resultSetValueObjects.length; i++) {
			for (j = 0; j < this.visualizationData.resultSetValueObjects[i].length; j++) {

				var obj = this.visualizationData.resultSetValueObjects[i][j];

				var factorName = obj.factorCategory; // PP
				if (!factorName) {
					factorName = obj.factorName;
				}
				if (!factorName || (typeof factorName === 'undefined') || factorName === null) {
					factorName = "unavailable";
				}
				var values = [];
				for (k = 0; k < obj.contrastsFactorValueIds.length; k++) {
					values.push({
								text : obj.contrastsFactorValues[obj.contrastsFactorValueIds[k]],
								children : []
							});
				}

				this.treeData.push({
							text : factorName,
							children : values
						});
			}
		}
		// display the tree
		this.tree = new Gemma.FactorSelectTree(this.treeData);
		Ext.apply(this.tree, {
					autoScroll : true,
					bodyStyle : 'padding-bottom:5px'
				});
		this.tree.on('checkchange', function(node, checked) { // fired every
					// time a
					// checkbox
					// changes
					// state{
					var filteringFn = function(o) {
						var factorId = o.factorCategory ? o.factorCategory : o.factorName;
						if (factorId.toLowerCase() === node.text.toLowerCase()) {
							return !checked; // should return true if you
							// want to filter
						}
						return null; // don't do anything to a node that
						// wasn't checked
					};
					var num = this.filterColumns(filteringFn);
					this.TOTAL_NUMBER_OF_HIDDEN_COLUMNS = num;
					this.doLayout();
					this.refreshVisualization();
				}, this);

		/**
		 * selection grids
		 */
		var selectionRecord = Ext.data.Record.create([
			{ name: 'id'},
			{ name: 'shortName'},
			{ name: 'longName'}
		]);
		var eeSelectionStore = new Ext.data.Store({
			reader: new Ext.data.ArrayReader({
				idIndex: 0
			}, selectionRecord)
		});
		var geneSelectionStore = new Ext.data.Store({
			reader: new Ext.data.ArrayReader({
				idIndex: 0
			}, selectionRecord)
		});
		var eeSelectionData = [];
		var lastDatasetId = null;
		for (i = 0; i < this.visualizationData.resultSetValueObjects.length; i++) { // for every ee group
			for (j = 0; j < this.visualizationData.resultSetValueObjects[i].length; j++) { // for every col
				// get the experiments, they should be grouped by datasetId
				if (this.visualizationData.resultSetValueObjects[i][j].datasetId != lastDatasetId) {
					eeSelectionData.push([this.visualizationData.resultSetValueObjects[i][j].datasetId,
											this.visualizationData.resultSetValueObjects[i][j].datasetShortName,
											this.visualizationData.resultSetValueObjects[i][j].datasetName]);
				}
				lastDatasetId = this.visualizationData.resultSetValueObjects[i][j].datasetId;
			}
		}
		eeSelectionStore.loadData(eeSelectionData);
		var geneSelectionData = [];
		for (i = 0; i < this.visualizationData.geneIds.length; i++) {
				eeSelectionData.push([this.visualizationData.geneIds[i],
										this.visualizationData.geneNames[i],
										this.visualizationData.geneFullNames[i]]);
		}
		geneSelectionStore.loadData(geneSelectionData);
		
		this.eeSelectionList = new Ext.list.ListView({
			store:eeSelectionStore,
			multiSelect:true,
			columns:[{dataIndex:'shortName'}]
		});

		this.TOTAL_NUMBER_OF_COLUMNS = 0;
		var datasetGroupIndex;
		for (datasetGroupIndex = 0; datasetGroupIndex < this.visualizationData.resultSetValueObjects.length; datasetGroupIndex++) {
			this.TOTAL_NUMBER_OF_COLUMNS = this.TOTAL_NUMBER_OF_COLUMNS
					+ this.visualizationData.resultSetValueObjects[datasetGroupIndex].length;
		}
		this._heatMapWidth = this.TOTAL_NUMBER_OF_COLUMNS
				* 1
				* (Gemma.MetaVisualizationConfig.cellWidth * 1 + Gemma.MetaVisualizationConfig.columnSeparatorWidth * 1)
				* 1 + Gemma.MetaVisualizationConfig.groupSeparatorWidth
				* (this.visualizationData.resultSetValueObjects.length - 1) * 1;

		Ext.apply(this, {
			width : Gemma.MetaVisualizationConfig.panelWidth,
			height : Gemma.MetaVisualizationConfig.panelHeight,
			layout : 'absolute',
			_visualizationData : this.visualizationData,
			geneScores : this.visualizationData.geneScores,
			geneOrdering : null,

			visibleMissingValuesGeneScore : null,

			// TOTAL_NUMBER_OF_COLUMNS: null,
			TOTAL_NUMBER_OF_ROWS : null,
			TOTAL_NUMBER_OF_HIDDEN_COLUMNS : 0,

			filterColumns : function(filteringFn) {
				return this._imageArea._heatmapArea.filterColumns(filteringFn);
			},
			_sortColumns : function(asc_desc, sortingFn) {
				this._imageArea._heatmapArea._sortColumns(asc_desc, sortingFn);
			},
			tbar : [this.tbarTitle, '->', {
				xtype : 'button',
				x : 5,
				y : 5,
				text : '<b>Download</b>',
				icon : '/Gemma/images/download.gif',
				cls : 'x-btn-text-icon',
				// ctCls: 'purple-btn',
				handler : function() {
					Ext.Msg.alert("Download Results", "Coming soon!");
				}
			}/*
				 * , { xtype: 'button', x: 5, y: 29, ref: 'miniWindowButton',
				 * text: 'Pvalue', enableToggle: true, listeners: { toggle:
				 * function(target, checked){ if (checked) {
				 * this.MiniWindowTool.show(); } else {
				 * this.MiniWindowTool.hide(); } }, scope: this } }
				 */
			],
			items : [{
				// a window for displaying details as elements of the image are
				// hovered over
				//title : 'Details <span style="color:grey">(Drag me!)</span>',
				ref : '_hoverDetailsPanel',
				xtype : 'window',
				//height : 200,
				width : 300,
				x : Gemma.MetaVisualizationConfig.panelWidth - 650,
				y : 20,
				autoScroll : true,
				//collapsible : true,
				closable : false,
				shadow : false,
				border : false,
				bodyBorder : false,
				hidden: true, // doesn't work for some reason
				bodyStyle : 'padding: 7px',
				html : '<span style="color:grey;font-size:1.3em">Hover over the visualisation for quick details or click for more information.</span>',
				tpl : new Ext.XTemplate(
						'<span style="font-size: 12px ">',

						'<tpl for=".">',
						'<tpl if="type==\'experiment\'">',
						'<b>Experiment</b>: <a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
						'{datasetId}"',
						' ext:qtip="{datasetName}">{datasetShortName}</a> {datasetName}<br><br>',
						'<b>Factor</b>: {factorCategory} - {factorDescription}<br> <br>',
						'<b>Factor Values</b>: {factorValues}<br><br> ',
						'<b>Baseline</b>: {baseline}<br><br>',
						'</tpl>',
						'<tpl if="type==\'gene\'">',
						'<b>Gene</b>: <a target="_blank" href="/Gemma/gene/showGene.html?id={geneId}">{geneSymbol}</a> {geneFullName}<br><br> ',
						'</tpl>',
						'<tpl if="type==\'cell\'">',
						'<b>Gene</b>: <a target="_blank" href="/Gemma/gene/showGene.html?id={geneId}">{geneSymbol}</a> {geneFullName}<br><br> ',
						'<b>Experiment</b>: <a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
						'{datasetId}"', ' ext:qtip="{datasetName}">{datasetShortName}</a> {datasetName}<br><br>',
						'<b>Factor</b>:{factorCategory} - {factorDescription}<br><br> ', '<b>q Value</b>: {pvalue}',
						'</tpl>', '</tpl></span>'),
				tplWriteMode : 'overwrite'
			}, {
				ref : '_toolPanels',
				height : Gemma.MetaVisualizationConfig.panelHeight - 30,
				width : 300,
				x : Gemma.MetaVisualizationConfig.panelWidth - 300,
				y : 0,
				layout : 'vbox',
				// layout: 'accordion',
				/*
				 * layoutConfig:{ fill: true, titleCollapse: true,
				 * //activeOnTop: true animate: true },
				 */
				border : true,
				applicationRoot : this,
				items : [{
					title : 'Sort',
					flex : 0,
					width : 300,
					collapsible : true,
					// collapsed:true,
					border : true,
					bodyBorder : true,
					layout : 'form',
					bodyStyle : 'padding:5px',
					defaults : {
						hideLabel : false
					},
					items : [{
						xtype: 'combo',
						hiddenName: 'conditionSort',
						fieldLabel: 'Sort conditions by',
						mode: 'local',
						displayField: 'text',
						valueField: 'name',
						width:150,
						editable: 'false',
						forceSelection: 'true',
						triggerAction:'all',
						store: new Ext.data.ArrayStore({
							fields:['name','text'],
							data:[['experiment','experiment'],['qValues','sum of q Values'], ['specificity','specificity']]
						}),
						listeners : {
								select : function(field, record, selIndex) {

									if (record.get('name') === 'qValues') {
										this._sortColumns('DESC', function(o1, o2) {

													return o1.overallDifferentialExpressionScore
															- o2.overallDifferentialExpressionScore;
												});
										this.doLayout();
									}else if(record.get('name') ==='specificity'){
										this._sortColumns('ASC', function(o1, o2) {

													return o1.specificityScore - o2.specificityScore;
												});

										this.doLayout();
									}else if(record.get('name') ==='experiment'){
										this._sortColumns('ASC', function(o1, o2) {

													return (o1.datasetName >= o2.datasetName)? 1:-1;
												});

										this.doLayout();
									}
									this.refreshVisualization();
								},
								render: function(combo){
									combo.setValue('experiment');
								},
								scope : this
							}
					},{
						xtype: 'combo',
						hiddenName: 'geneSort',
						fieldLabel: 'Sort genes by',
						mode: 'local',
						displayField: 'text',
						valueField: 'name',
						width:150,
						editable: 'false',
						forceSelection: 'true',
						triggerAction:'all',
						store: new Ext.data.ArrayStore({
							fields:['name','text'],
							data:[['symbol','symbol'],['score','gene score']]
						}),
						listeners : {
								select : function(field, record, selIndex) {
									var geneGroupIndex;
                                    var i;
									if (record.get('name') === 'symbol') {
										// Default geneOrdering
                                        for (geneGroupIndex = 0; geneGroupIndex < this._imageArea._heatmapArea.geneNames.length; geneGroupIndex++) {
                                            this.geneOrdering[geneGroupIndex] = [];
                                            for (i = 0; i < this._imageArea._heatmapArea.geneIds[geneGroupIndex].length; i++) {
                                                this.geneOrdering[geneGroupIndex].push(i);
                                            }
                                        }
									}else if(record.get('name') ==='score'){
										// Sort genes : changes gene order
										for (i = 0; i < this.geneScores[0].length; i++) {
											this.geneScores[0][i].sort(function(o1, o2) {
														return o2.score - o1.score;
													});
										}
										for (geneGroupIndex = 0; geneGroupIndex < this._imageArea._heatmapArea.geneNames.length; geneGroupIndex++) {
											this.geneOrdering[geneGroupIndex] = [];
											for (i = 0; i < this._imageArea._heatmapArea.geneIds[geneGroupIndex].length; i++) {
												// if
												// (this.geneScores[0][geneGroupIndex][i].score
												// !== 0) {
												this.geneOrdering[geneGroupIndex]
														.push(this.geneScores[0][geneGroupIndex][i].index);
												// }
											}
										}
									}
									this.refreshVisualization();
								},
								render: function(combo){
									combo.setValue('symbol');
								},
								scope : this
							}
					}]
				}, {
					title : 'Filter',
					flex : 1,
					collapsible : true,
					collapsed : false,
					border : true,
					bodyBorder : true,
					autoScroll : true,
					layout : 'form', // to get font sizes matching
					items : [{
								xtype : 'checkbox',
								hideLabel : true,
								boxLabel : 'Show columns with no results.',
								checked : true,
								hidden : true,
								listeners : {
									check : function(target, checked) {
										var filteringFn = null;
										if (!checked) { // hide columns without
											// results
											filteringFn = function(o) {

												if (o.overallDifferentialExpressionScore === 0) {
													return !checked;
												}
												if (o.miniPieValue > 120) {
													return !checked;
												}

												if ((o.missingValuesScore / this.TOTAL_NUMBER_OF_ROWS) > 0.7) {
													return !checked;
												}

												return null;
											};
										} else { // show columns without
											// results but respect
											// filtering by factor
											var checkedNodeIds = this.tree.getChecked('id');
											console.log("checkedNodeIds: " + checkedNodeIds);
											// if column has same factor name as
											// checked node, show it, otherwise
											// hide it
											filteringFn = function(o) {
												console
														.log("o.factorName.toLowerCase(): "
																+ o.factorName.toLowerCase());
												if (checkedNodeIds.indexOf(o.factorName.toLowerCase()) > -1) {
													return false; // don't
													// filter it
												} else {
													return true;
												}
											};
										}

										var num = this.filterColumns(filteringFn);
										this.TOTAL_NUMBER_OF_HIDDEN_COLUMNS = num;
										this.doLayout();
										this.refreshVisualization();
									},
									scope : this
								}
							}, this.tree]
				},{
					title: 'Select',
					flex: 1,
					items: [{
						html: 'Experiments',items:this.eeSelectionList
					}, {
						html: 'Genes'
					}]
				}]
			}, {
				ref : '_imageArea',
				autoScroll : true,
				layout : 'absolute',
				frame : false,
				border : false,
				width : Gemma.MetaVisualizationConfig.panelWidth - 300,
				height : Gemma.MetaVisualizationConfig.panelHeight - 30,
				items : [{
							xtype : 'metaVizGeneLabels',
							height : Gemma.MetaVisualizationUtils
									.calculateColumnHeight(this.visualizationData.geneNames),
							width : 80,
							x : 0,
							y : Gemma.MetaVisualizationConfig.columnLabelHeight+12,
							labels : this.visualizationData.geneNames,
							geneGroupNames : this.visualizationData.geneGroupNames,
							border : false,
							bodyBorder : false,
							applicationRoot : this,
							ref : '_geneLabels'
						}, {
							xtype : 'metaVizRotatedLabels',
							width : this._heatMapWidth + Gemma.MetaVisualizationConfig.labelExtraSpace,
							height : 260,
							x : 80,
							y : 0,
							applicationRoot : this,
							visualizationData : this.visualizationData.resultSetValueObjects,
							datasetGroupNames : this.visualizationData.datasetGroupNames,
							ref : 'topLabelsPanel'
						}, {
							xtype : 'metaVizScrollableArea',
							height : Gemma.MetaVisualizationUtils
									.calculateColumnHeight(this.visualizationData.geneNames),
							width : this._heatMapWidth,
							x : 80,
							y : Gemma.MetaVisualizationConfig.columnLabelHeight+2,
							dataDatasetGroups : this.visualizationData.resultSetValueObjects,
							geneNames : this.visualizationData.geneNames,
							geneIds : this.visualizationData.geneIds,
							applicationRoot : this,
							ref : '_heatmapArea'
						}]
			}]
		});
		Gemma.MetaHeatmapApp.superclass.initComponent.apply(this, arguments);

		this._imageArea.topLabelsPanel._setHeatmapContainer(this._imageArea._heatmapArea);
		this._imageArea._heatmapArea._setTopLabelsBox(this._rotatedLabelsBox);

		this.TOTAL_NUMBER_OF_ROWS = 0;
		// Default geneOrdering
		this.geneOrdering = [];
		var geneGroupIndex;
		for (geneGroupIndex = 0; geneGroupIndex < this.visualizationData.geneNames.length; geneGroupIndex++) {
			this.geneOrdering[geneGroupIndex] = [];
			for (i = 0; i < this.visualizationData.geneNames[geneGroupIndex].length; i++) {
				this.geneOrdering[geneGroupIndex].push(i);
				this.TOTAL_NUMBER_OF_ROWS++;
			}
		}

		this.MiniWindowTool = new Ext.Window({
					width : 300,
					height : 150,
					title : "",
					layout : "vbox",
					items : [{
								xtype : 'label',
								ref : 'specificity',
								text : "Specificity: "
							}, {
								xtype : 'label',
								ref : 'pValue',
								text : "pValue: " + this.pvalue
							}, {
								xtype : 'label',
								ref : 'foldChange',
								text : "Fold change: "
							}]
				});
	},
	onRender : function() {
		Gemma.MetaHeatmapApp.superclass.onRender.apply(this, arguments);

		this._hoverDetailsPanel.show();
		// this.MiniWindowTool.show();
		// this.el.on('mousemove', function(event,target) {
		// if (this.miniWindowButton.pressed == true) {
		// var x = event.getPageX()+10;
		// var y = event.getPageY()+15;
		// this.MiniWindowTool.setPosition(x,y);
		// }
		// }, this );

	},
	refreshVisualization : function() {
		this._imageArea.topLabelsPanel.refresh();
		this._imageArea._heatmapArea.refresh();
		this._imageArea._geneLabels.refresh();
	}
});

Gemma.MetaHeatmapScrollableArea = Ext.extend(Ext.Panel, {
			initComponent : function() {
				Ext.apply(this, {
							border : false,
							bodyBorder : false,
							layout : 'hbox',
							layoutConfig : {
								defaultMargins : {
									top : 0,
									right : Gemma.MetaVisualizationConfig.groupSeparatorWidth,
									bottom : 0,
									left : 0
								}
							},
							dataDatasetGroups : this.dataDatasetGroups,
							geneNames : this.geneNames,
							geneIds : this.geneIds,

							applicationRoot : this.applicationRoot,

							_setTopLabelsBox : function(l) {
								this._topLabelsBox = l;
							},

							filterColumns : function(filteringFn) {
								var count = 0;
								this.items.each(function() {
											count = count + this.filterColumns(filteringFn);
										});
								return count;
							},

							_filterRows : function(filteringFn) {

							},

							_sortColumns : function(asc_desc, sortingFn) {
								this.items.each(function() {
											this.items.sort(asc_desc, sortingFn);
										});
							}
						});

				Gemma.MetaHeatmapScrollableArea.superclass.initComponent.apply(this, arguments);
			},

			onRender : function() {
				Gemma.MetaHeatmapScrollableArea.superclass.onRender.apply(this, arguments);
				var i;
				for (i = 0; i < this.dataDatasetGroups.length; i++) {
					if (this.dataDatasetGroups[i].length > 0) {
						this.add(new Gemma.MetaHeatmapDatasetGroupPanel({
									applicationRoot : this.applicationRoot,
									height : this.height,
									dataFactorColumns : this.dataDatasetGroups[i],
									datasetGroupIndex : i,
									geneNames : this.geneNames[i],
									geneIds : this.geneIds[i]
								}));
					}

				}

			},

			refresh : function() {
				this.items.each(function() {
							this.refresh();
						});
			}

		});
Ext.reg('metaVizScrollableArea', Gemma.MetaHeatmapScrollableArea);

Ext.reg('taxonCombo', Gemma.TaxonCombo);

Gemma.MetaHeatmapControlWindow = Ext.extend(Ext.Window, {

			hidden : true,
			shadow : false,
			initComponent : function() {
				Ext.apply(this, {
							title : 'Visualization settings',
							height : 400,
							width : 300,
							layout : 'accordion',
							layoutConfig : {
								titleCollapse : false,
								animate : true,
								activeOnTop : true
							},
							items : [{
										title : 'Data selection',
										xtype : 'metaVizDataSelection',
										ref : 'selectionPanel'
									}, {
										title : 'Filter/Sort',
										xtype : 'metaVizSortFilter',
										ref : 'sortPanel'
									}]
						});
				Gemma.MetaHeatmapControlWindow.superclass.initComponent.apply(this, arguments);
			},
			onRender : function() {
				Gemma.MetaHeatmapControlWindow.superclass.onRender.apply(this, arguments);
			}
		});

Gemma.MetaHeatmapSortFilter = Ext.extend(Ext.Panel, {
			initComponent : function() {
				Ext.apply(this, {});
				Gemma.MetaHeatmapSortFilter.superclass.initComponent.apply(this, arguments);
			},
			onRender : function() {
				Gemma.MetaHeatmapSortFilter.superclass.onRender.apply(this, arguments);
			}
		});

Ext.reg('metaVizSortFilter', Gemma.MetaHeatmapSortFilter);

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
					if (experimentCount == 0) {
						Ext.DomHelper.overwrite('meta-heatmap-div', {
							html : '<img src="/Gemma/images/icons/warning.png"/> Sorry, no data available for your search.'
						});
					} else {
						_metaVizApp = new Gemma.MetaHeatmapApp({
									tbarTitle : '<b>Differential Expression Visualisation</b> (Data available for '
											+ experimentCount + ' of ' + this.param.datasetCount + ' experiments)',
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
				id : 94,
				type : "databaseBackedGroup"
			}/*
				 * ,{ id: 75, type: "databaseBackedGroup" }
				 */
			],
			datasetReferences : [{
						id : 6110,
						type : "databaseBackedGroup"
					}
			/*
			 * ,{ id: 6107, type: "databaseBackedGroup" }
			 */
			],
			geneNames : ["gene TEST"],//, "gene TEST 2"],
			datasetNames : ["dataset TEST"],//, "dataset TEST2"],
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
		}

		Gemma.MetaHeatmapDataSelection.superclass.initComponent.apply(this, arguments);

		if (this.param && this.param.geneReferences && this.param.geneNames && this.param.datasetReferences
				&& this.param.datasetNames && this.param.taxonId) {
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
