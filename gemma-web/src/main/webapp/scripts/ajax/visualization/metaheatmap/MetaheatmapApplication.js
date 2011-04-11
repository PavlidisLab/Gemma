Ext.namespace('Gemma');

//MetaHeatmap Application
//Consist of 3 main panels:
//- gene labels
//- anlaysis labels
//- main visualization area
//It is controlled by window that allows sorting/filtering and choosing data.
Gemma.MetaHeatmapApp = Ext.extend(Ext.Panel, {	
	initComponent: function() {

	Ext.apply(this, {
		width: 1700,
		height: 2000,
		_visualizationData : this.visualizationData,
		geneScores: this.visualizationData.geneScores,
		geneOrdering: null,

		visibleMissingValuesGeneScore: null,

		TOTAL_NUMBER_OF_COLUMNS: null,
		TOTAL_NUMBER_OF_ROWS: null,

		layout: 'absolute',
		filterColumns: function(filteringFn) {
		this._heatmapArea.filterColumns(filteringFn);
	},			
	_sortColumns: function (asc_desc, sortingFn) {
		this._heatmapArea._sortColumns(asc_desc, sortingFn);
	},
	items: [
	        {
	        	xtype: 'button',
	        	x:5,
	        	y:5,
	        	text: 'Settings'
	        },
	        {
	        	xtype: 'button',
	        	x:5,				
	        	y:29,
	        	ref: 'miniWindowButton',					
	        	text: 'Pvalue',
	        	enableToggle: true,
	        	listeners: { 
	        	toggle : function ( target, checked ) {		
	        	if (checked) {
	        		this.MiniWindowTool.show();
	        	} else {
	        		this.MiniWindowTool.hide();
	        	}
	        }, scope: this
	        }					
	        },							
	        { xtype: 'metaVizGeneLabels',
	        	height: Gemma.MetaVisualizationUtils.calculateColumnHeight(this.visualizationData.geneNames),
	        	width: 80,
	        	x: 0,
	        	y: 272,
	        	labels: this.visualizationData.geneNames,
	        	geneGroupNames: this.visualizationData.geneGroupNames,
	        	border: false,
	        	bodyBorder: false,
	        	applicationRoot: this,
	        	ref: '_geneLabels'
	        },			
	        {			        
	        	xtype: 'metaVizRotatedLabels',
	        	width: 2300,
	        	height: 260,
	        	x: 80,
	        	y: 0,
	        	applicationRoot: this,
	        	visualizationData: this.visualizationData.resultSetValueObjects,
	        	datasetGroupNames: this.visualizationData.datasetGroupNames,
	        	ref: 'topLabelsPanel'
	        },
	        {
	        	xtype: 'metaVizScrollableArea',
	        	height: Gemma.MetaVisualizationUtils.calculateColumnHeight(this.visualizationData.geneNames),
	        	width: 2500,
	        	x: 80,
	        	y: 262,
	        	dataDatasetGroups: this.visualizationData.resultSetValueObjects,
	        	geneNames: this.visualizationData.geneNames,
	        	geneIds: this.visualizationData.geneIds,
	        	applicationRoot: this,
	        	ref: '_heatmapArea'
	        }]			
	});
	Gemma.MetaHeatmapApp.superclass.initComponent.apply(this, arguments);

	this.topLabelsPanel._setHeatmapContainer ( this._heatmapArea );
	this._heatmapArea._setTopLabelsBox ( this._rotatedLabelsBox );	

	this.TOTAL_NUMBER_OF_ROWS = 0;
	// Default geneOrdering
	this.geneOrdering = [];
	var geneGroupIndex;
	for (geneGroupIndex = 0; geneGroupIndex < this.visualizationData.geneNames.length; geneGroupIndex++) {			
		this.geneOrdering[geneGroupIndex] = [];
		var i;
		for (i = 0; i < this.visualizationData.geneNames[geneGroupIndex].length; i++) {
			this.geneOrdering[geneGroupIndex].push(i);
			this.TOTAL_NUMBER_OF_ROWS++;
		}
	}


	this.TOTAL_NUMBER_OF_COLUMNS = 0;
	var datasetGroupIndex;
	for (datasetGroupIndex = 0; datasetGroupIndex < this._visualizationData.resultSetValueObjects.length ; datasetGroupIndex++) {
		this.TOTAL_NUMBER_OF_COLUMNS = this.TOTAL_NUMBER_OF_COLUMNS + this._visualizationData.resultSetValueObjects[datasetGroupIndex].length;
	}		

	this.MiniWindowTool = new Ext.Window(
			{	width: 300,
				height: 150,
				title:"",
				layout:"vbox",
				items:[{xtype:'label', ref:'specificity', text:"Specificity: "},
				       {xtype:'label', ref:'pValue', text:"pValue: "},
				       {xtype:'label', ref:'foldChange', text:"Fold change: "}]
			});		
},
onRender: function() {
	Gemma.MetaHeatmapApp.superclass.onRender.apply(this, arguments);	

	// this.MiniWindowTool.show();
//	this.el.on('mousemove', function(event,target) {
//	if (this.miniWindowButton.pressed == true) {
//	var x = event.getPageX()+10;
//	var y = event.getPageY()+15;
//	this.MiniWindowTool.setPosition(x,y);
//	}
//	}, this );

},
refreshVisualization: function() {
	this.topLabelsPanel.refresh();
	this._heatmapArea.refresh();
	this._geneLabels.refresh();
}	
});


Gemma.MetaHeatmapScrollableArea = Ext.extend(Ext.Panel, {
	initComponent: function() {
	Ext.apply(this, {
		border: false,
		bodyBorder: false,
		layout: 'hbox',
		layoutConfig: {
		defaultMargins: {top:0, right:Gemma.MetaVisualizationConfig.groupSeparatorWidth, bottom:0, left:0}
	},
	dataDatasetGroups : this.dataDatasetGroups,
	geneNames: this.geneNames,
	geneIds: this.geneIds,

	applicationRoot: this.applicationRoot,

	_setTopLabelsBox : function (l) {
		this._topLabelsBox=l;			
	},

	filterColumns: function (filteringFn) {
		this.items.each( function() { this.filterColumns(filteringFn); });				
	},

	_filterRows: function (filteringFn) {

	},

	_sortColumns: function (asc_desc, sortingFn) {
		this.items.each(function() {this.items.sort(asc_desc, sortingFn);} );
	}
	});

	Gemma.MetaHeatmapScrollableArea.superclass.initComponent.apply(this, arguments);		
},

onRender: function() {				
	Gemma.MetaHeatmapScrollableArea.superclass.onRender.apply(this, arguments);
	var i;
	for ( i = 0; i < this.dataDatasetGroups.length; i++ ) {			
		this.add(new Gemma.MetaHeatmapDatasetGroupPanel(
				{ applicationRoot: this.applicationRoot,
					height: this.height,
					dataFactorColumns : this.dataDatasetGroups[i],
					datasetGroupIndex: i,
					geneNames: this.geneNames[i],
					geneIds: this.geneIds[i]}));
	}

},

refresh: function() {
	this.items.each(function() { this.refresh(); });
}

});
Ext.reg('metaVizScrollableArea', Gemma.MetaHeatmapScrollableArea);

Ext.reg('taxonCombo', Gemma.TaxonCombo);

Gemma.MetaHeatmapControlWindow = Ext.extend(Ext.Window, {	

	initComponent: function() {
	Ext.apply(this, {
		title: 'Visualization settings',
		height: 400,
		width: 300,
		layout: 'accordion',
		layoutConfig: {
		titleCollapse: false,
		animate: true,
		activeOnTop: true
	},
	
	items: [{title: 'Data selection', xtype: 'metaVizDataSelection', ref:'selectionPanel'},
	        {title: 'Filter/Sort', xtype: 'metaVizSortFilter', ref:'sortPanel'}]
	});
	Gemma.MetaHeatmapControlWindow.superclass.initComponent.apply(this, arguments);
},
onRender: function() {
	Gemma.MetaHeatmapControlWindow.superclass.onRender.apply(this, arguments);	
}
});


Gemma.MetaHeatmapSortFilter = Ext.extend(Ext.Panel, {	
	initComponent: function() {
	Ext.apply(this, {

	});
	Gemma.MetaHeatmapSortFilter.superclass.initComponent.apply(this, arguments);
},
onRender: function() {
	Gemma.MetaHeatmapSortFilter.superclass.onRender.apply(this, arguments);

}
});


Ext.reg('metaVizSortFilter', Gemma.MetaHeatmapSortFilter);


Gemma.MetaHeatmapDataSelection = Ext.extend(Ext.Panel, {
	
	constructor : function(searchCommand) {
		Ext.apply(this,{
			param: searchCommand.ownerCt
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
	taxonId: null,

	prepareVisualization : function(target) {
		
		this.geneGroupNames = [];
		this.geneGroupReferences = [];
		this.datasetGroupNames = [];
		this.datasetGroupReferences = [];
		this._selectedGeneGroups = [];
		this._selectedDatasetGroups = [];
					// control variables used if asynchronous session group creation is performed
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
												
						if(this._selectedGeneGroups[i] && this._selectedGeneGroups[i]!==null){
							// if the group has a null value for reference.id, then it hasn't been
							// created as a group in the database nor session
							if(this._selectedGeneGroups[i].reference.id === null){
								this._selectedGeneGroups[i].geneIds = this._selectedGeneGroups[i].memberIds;
								geneGroupsToBindToSession.push(this._selectedGeneGroups[i]);
							}else{
								this.geneGroupReferences.push(this._selectedGeneGroups[i].reference);
			        			this.geneGroupNames.push(this._selectedGeneGroups[i].name);
							}
						}
					}
					var j;
					if(geneGroupsToBindToSession.length !== 0){
						this.waitingForGeneSessionGroupBinding = true;
						GeneSetController.addNonModificationBasedSessionBoundGroups(geneGroupsToBindToSession,
							function(geneSets){
								// should be at least one geneset
								if(geneSets === null || geneSets.length === 0 ){
									// TODO error message
									return;
								}else{
									for(j = 0; j< geneSets.length; j++){
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
							// if the group has a null value for reference.id, then it hasn't been
							// created as a group in the database nor session
							if(this._selectedDatasetGroups[i].reference.id === null){
								this._selectedDatasetGroups[i].expressionExperimentIds = this._selectedDatasetGroups[i].memberIds;
								datasetGroupsToBindToSession.push(this._selectedDatasetGroups[i]);
							}else{
								this.datasetGroupReferences.push(this._selectedDatasetGroups[i].reference);
								this.datasetGroupNames.push(this._selectedDatasetGroups[i].name);
							}
						}
		        	}
					if(datasetGroupsToBindToSession.length !== 0){
						this.waitingForDatasetSessionGroupBinding = true;
						ExpressionExperimentSetController.addNonModificationBasedSessionBoundGroups(datasetGroupsToBindToSession,
							function(datasetSets){
								// should be at least one datasetSet
								if(datasetSets === null || datasetSets.length === 0 ){
									// TODO error message
									return;
								}else{
									for(j = 0; j< datasetSets.length; j++){
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
					if(!this.waitingForDatasetSessionGroupBinding && !this.waitingForGeneSessionGroupBinding){
						this.doVisualization();
					}
	},
	doVisualization : function() {
		        	var estimatedTime = 15 * this.geneGroupReferences.length * this.datasetGroupReferences.length;

		        	var progressWindow = new Ext.Window({ width: 400,
		        		height: 55,
		        		title:"Estimated time: " + ( estimatedTime / 1000) +"s",
		        		items:[{xtype:'progress', ref:'progress_bar'}]
		        	});
		        	progressWindow.show();
		        	progressWindow.progress_bar.wait({
		        		interval: 1000,
		        		duration: estimatedTime,
		        		increment: estimatedTime/1000,
		        		text: 'Building visualization...',
		        		scope: this,
		        		fn: function() { }
		        	});
					this._selectedDatasetGroups = [];
					this._selectedGeneGroups = [];
					
					if(!this.taxonId || this.taxonId === null){
						this.taxonId = this._taxonCombo.getSelected().id;
					}

		        	DifferentialExpressionSearchController.differentialExpressionAnalysisVisualizationSearch(this.taxonId, 
							this.datasetGroupReferences, this.geneGroupReferences, function(data) {
								
		        		progressWindow.hide();

		        		data.geneGroupNames = this.geneGroupNames;
		        		data.datasetGroupNames = this.datasetGroupNames;

		        		_metaVizApp = new Gemma.MetaHeatmapApp({visualizationData: data, applyTo:'meta-heatmap-div'});
		        		_metaVizApp.doLayout();
		        		_metaVizApp.refreshVisualization();
		       /* 		
						_sortPanel.add({xtype:'checkbox',
		        			boxLabel:'Sort by gene score.',
		        			listeners: {
		        			check : function(target, checked) {
		        			var geneGroupIndex;
		        			var i;
		        			if (checked) {
		        				// Sort genes : changes
		        				// gene order
		        				for (i = 0; i < this.geneScores[0].length; i++) {
		        					this.geneScores[0][i].sort( function ( o1, o2 ) { return o2.score - o1.score; } );																	
		        				}
		        				for (geneGroupIndex = 0; geneGroupIndex < this._heatmapArea.geneNames.length; geneGroupIndex++) {			
		        					this.geneOrdering[geneGroupIndex] = [];
		        					for (i = 0; i < this._heatmapArea.geneIds[geneGroupIndex].length; i++) {
		        						if ( this.geneScores[0][geneGroupIndex][i].score !== 0 ) {
		        							this.geneOrdering[geneGroupIndex].push( this.geneScores[0][geneGroupIndex][i].index );
		        						}
		        					}
		        				}														
		        			} else {														
		        				// Default geneOrdering
		        				for (geneGroupIndex = 0; geneGroupIndex < this._heatmapArea.geneNames.length; geneGroupIndex++) {			
		        					this.geneOrdering[geneGroupIndex] = [];														
		        					for (i = 0; i < this._heatmapArea.geneIds[geneGroupIndex].length; i++) {
		        						this.geneOrdering[geneGroupIndex].push(i);
		        					}
		        				}
		        			}
		        			this.refreshVisualization();
		        		}, scope: _metaVizApp
		        		}		  					
		        		});
		        		_sortPanel.add({xtype:'checkbox', 
		        			boxLabel:'Hide columns with no results.',
		        			listeners: { 
		        			check : function(target, checked) {
		        			if (checked) {
		        				var filteringFn = function(o) {
		        					if (o.overallDifferentialExpressionScore === 0) {return true;}
		        					if (o.miniPieValue > 120) {return true;}
		        					if ((o.missingValuesScore / this._metaVizApp.TOTAL_NUMBER_OF_ROWS) > 0.7 ) {return true;}		  														
		        					return false;
		        				};
		        				this.filterColumns(filteringFn);
		        				this.doLayout();
		        			} else {
//		        				this._unfilterColumns();
//		        				this.doLayout();
		        			}
		        			this.refreshVisualization();
		        		}, scope: _metaVizApp
		        		}
		        		});
		        		_sortPanel.add({xtype:'radiogroup',
		        			columns: 1,
		        			items: [
		        			        {xtype:'radio',
		        			        	name:'bbb',
		        			        	boxLabel:'Sort columns using sum of pValues.',
		        			        	listeners: { 
		        			        	check : function(target, checked) {
		        			        	if (checked) {
		        			        		this._sortColumns('DESC', function(o1, o2){return o1.overallDifferentialExpressionScore - o2.overallDifferentialExpressionScore;});
		        			        		this.doLayout();
		        			        	}
		        			        	this.refreshVisualization();
		        			        }, scope: _metaVizApp
		        			        }
		        			        },		  						  					
		        			        {xtype:'radio',
		        			        	name:'bbb',
		        			        	boxLabel:'Sort columns by specificity.',
		        			        	listeners: { 
		        			        	check : function(target, checked) {
		        			        	if (checked) {											
		        			        		this._sortColumns('DESC', function(o1, o2){return o1.specificityScore - o2.specificityScore;});
		        			        		this.doLayout();
		        			        	}
		        			        	this.refreshVisualization();
		        			        }, scope: _metaVizApp
		        			        }
		        			        }]


		        		}); 
	*/					
		        	}.createDelegate(this));
		        },	
			testfunc : function(){
			Ext.Msg.alert('imma','test func!');
		},
	initComponent: function() {
	
	this.on('geneGroupsReadyForVisualization', function(geneReferences, geneNames){
		if(!this.waitingForDatasetSessionGroupBinding){
			this.doVisualization();
		}
	}, this);
	this.on('datasetGroupsReadyForVisualization', function(){
		if(!this.waitingForGeneSessionGroupBinding){
			this.doVisualization();
		}
	}, this);
	Ext.apply(this, {
		_geneCombos: [],
		_datasetCombos: [],
		_metaVizApp: null,
		_sortPanel: null,
		height: 450,
		layout: 'fit',
		items:/* {
			xtype: 'analysisResultsSearchForm',
			ref: '_analysisResultsSearchForm',
			stateful: true
		}*/
		
		[
		        {
		        	xtype: 'taxonCombo',
		        	width: 200,
		        	ref: '_taxonCombo'
		        },
		        {
		        	xtype: 'panel',
		        	width: 200,
		        	height: 100,			
		        	ref: 'genePickerPanel',
		        	items: [{
		        		xtype: 'button',
		        		text: 'Add gene group',
		        		width: 200,
		        		ref: 'addNewGeneGroupButton',					
		        		listeners: {
		        		click: {fn: function(target) {
		        		//var genePicker = new Gemma.GeneGroupCombo({width : 200});
		        		//genePicker.setTaxon(this._taxonCombo.getTaxon());		
						var genePicker = new Gemma.GeneAndGeneGroupCombo({width:200});
						genePicker.setTaxonId(this._taxonCombo.getTaxon().id);			
		        		this._geneCombos.push(genePicker);
		        		this.genePickerPanel.add(genePicker);
		        		this.genePickerPanel.doLayout();
		        	}, scope: this
		        	}
		        	}
		        	}]

		        },{
		        	xtype: 'panel',
		        	width: 200,
		        	height: 100,			
		        	ref: 'datasetPickerPanel',
		        	items: [{
		        		xtype: 'button',
		        		text: 'Add dataset group',
		        		width: 200,
		        		ref: 'addNewDatasetGroupButton',					
		        		listeners: {
		        		click : {fn : function(target) {
		        		//var datasetPicker = new Gemma.DatasetGroupCombo({width : 200});
						var datasetPicker = new Gemma.ExperimentAndExperimentGroupCombo({width : 200});
		        		this._datasetCombos.push(datasetPicker);
		        		this.datasetPickerPanel.add(datasetPicker);
		        		this.datasetPickerPanel.doLayout();
		        	}, scope : this }
		        	}
		        	}]
		        },
		        {
		        	xtype: 'button',
		        	text: 'Visualize!',
		        	ref: 'goButton',
		        	width: 50,
		        	height: 30,			
		        	listeners: {
		        		click: {
							fn: this.prepareVisualization,
							scope: this
						}
		        	}
		        }]			
	});
		
	if(this.param.geneReferences){
		this.geneGroupReferences = this.param.geneReferences;
	}
	if(this.param.geneNames){
		this.geneGroupNames = this.param.geneNames;
	}
	if(this.param.datasetReferences){
		this.datasetGroupReferences = this.param.datasetReferences;
	}
	if(this.param.datasetNames){
		this.datasetGroupNames = this.param.datasetNames;
	}
	if(this.param.taxonId){
		this.taxonId = this.param.taxonId;
	}
	Gemma.MetaHeatmapDataSelection.superclass.initComponent.apply(this, arguments);
	
	if(this.param.geneReferences && this.param.geneNames && 
		this.param.datasetReferences && this.param.datasetNames && this.param.taxonId){
		this.doVisualization();
	}
},
onRender: function() {
	Gemma.MetaHeatmapDataSelection.superclass.onRender.apply(this, arguments);

	_sortPanel = this.ownerCt.sortPanel;
}
});


Ext.reg('metaVizDataSelection', Gemma.MetaHeatmapDataSelection);


