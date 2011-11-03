Ext.namespace('Gemma.Metaheatmap', 'Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo', 'Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo');

Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieldTipHTML = '<br><b>Full Name</b>: official descriptive title<br><br>'+
	'<b>Short Name</b>: short name or ID (ex: GSE1234)<br><br>'+
	'<b>q Value</b>: confidence that the selected genes are differentially expressed<br><br>'+
	'<b>Diff. Exp. Specificity</b>: within each column, this is the proportion of probes that are differentially expressed '+
	'versus the total number of expressed probes. This measure is represented by each column\'s pie chart. Experiments are ordered based '+
	'on their columns\' average specificity.<br><br>';

Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieldTip = 'Name: official descriptive title.  q Value: confidence in the expression levels of the selected genes.  '+
	'Diff. Exp. Specificity: the proportion of probes that are differentially expressed '+
	'across each experimental factor '+
	'versus the total number of expressed probes. This measure is represented by each column\'s pie chart. Experiments are ordered based '+
	'on their column\'s average specificity.';

Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo.fieldTipHTML = '<b>Symbol</b>: official gene symbol<br>'+
	'<b>q Values</b>: confidence that the gene is differentially expressed, averaged across the queried experiments<br><br>';

Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo.fieldTip = 'Symbol: official gene symbol.  '+
	'q Values: confidence that the gene is differentially expressed, averaged across the queried experiments';

Gemma.Metaheatmap.ControlPanel = Ext.extend (Ext.Panel, {
			
	initComponent : function() {
		Ext.apply (this, {
			layout : { type : 'vbox',
					   align : 'stretch',
					   pack  : 'start',
					   defaultMargins : {
			    			top: 5,
			    			right: 20,
			    			bottom: 5,
			    			left: 10
					   }
//					   flex : 1
			},

			factorTreeFilter : [],
			
			autoScroll : false,			
			border : false,
			items: [
			        {
			        	xtype: 'label',
			        	text : 'Sort conditions:'
			        },
			        {
//						fieldLabel     : 'Sort conditions by',
//						fieldTipTitle  : 'Sort Conditions By:',
						//fieldTipHTML   : Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieldTipHTML,
						//fieldTip 	   : Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieledTip ,
			        	xtype 		   : 'combo',
			        	ref   		   : 'cmbConditionPresets',
			        	minHeight : 22,
						triggerAction  : 'all',
						displayField   : 'text',
						valueField 	   : 'index',
						editable 	   : false,
						mode 		   : 'local',
						forceSelection : true,
						autoSelect 	   : true,
						store :	new Ext.data.ArrayStore({
									fields : ['text', 'index'],
									data : this.ownerCt.conditionPresetNames,
									idIndex : 0
								}),
						listeners : {
			        		scope : this, 
			        		'select' : function (field, record, selIndex) {
										var selectedIndex = record.get('index');
										this.conditionPreset = this.ownerCt.conditionPresets[selectedIndex];
										this.applySortFilter();
			        		}	
			        	}
			        },
			        {
			        	xtype  : 'label',
			        	text   : '% missing data filter:',
						height : 15
			        },			        
			        {
			        	xtype  : 'slider',
			        	ref    : 'sldConditionDataMissingFilter',
			        	width  : 150,
			        	height : 20,
			        	value  : 100,
			        	increment : 1,
			        	minValue  : 1,
			        	maxValue  : 100,
			        	plugins: new Ext.slider.Tip({
			                 getText: function(thumb){
			                     return String.format('{0}% of genes missing', thumb.value);
			                 }
			            }),
			        	listeners : {
			        			changecomplete : function (slider, newValue, thumb) {
										this.applySortFilter();
			        				}, scope: this
			        		}					          		        	            
			        },
			        {
			        	xtype  : 'label',
			        	text   : 'Specificity filter:',
						height : 15
			        },			        
			        {
			        	xtype  : 'slider',
			        	ref    : 'sldSpecificityFilter',
			        	width  : 150,
			        	height : 20,
			        	value  : 10,
			        	increment : 1,
			        	minValue  : 1,
			        	maxValue  : 10,
			        	plugins: new Ext.slider.Tip({
			                 getText: function(thumb){
			                     return String.format('{0}% experiment specificity', thumb.value*10);
			                 }
			            }),
			        	listeners : {
			        			changecomplete : function (slider, newValue, thumb) {
										this.applySortFilter();
			        				}, scope: this
			        	}					          		        	            
			        },
			        {
			        	xtype  : 'label',
			        	text   : 'Sum pValue filter:',
						height : 15
			        },			        
			        {
			        	xtype  : 'slider',
			        	ref    : 'sldConditionPvalueFilter',
			        	width  : 150,
			        	height : 20,
			        	value  : 1,
			        	increment : 1,
			        	minValue  : 1,
			        	maxValue  : 100,
			        	plugins: new Ext.slider.Tip({
			                 getText: function(thumb){
			                     return String.format('{0} not sure what', thumb.value/100);
			                 }
			            }),
			        	listeners : {
			        			changecomplete : function (slider, newValue, thumb) {
									this.applySortFilter();
			        			}, scope: this
			        	}					          		        	            
			        },			        
			        {
			        	xtype: 'label',
			        	text : 'Sort genes:'
			        },
			        {
			        	ref : 'cmbGenePresets',
			        	xtype : 'combo',
			        	minHeight : 22,
						triggerAction  : 'all',
						displayField   : 'text',
						valueField 	   : 'id',
						editable 	   : false,
						mode 		   : 'local',
						forceSelection : true,
						autoSelect : true,
						store :	new Ext.data.ArrayStore({
									fields : ['text', 'id'],
									data : this.ownerCt.genePresetNames,
									idIndex : 0
								}),
						listeners : {
			        		scope : this, 
			        		'select' : function (field, record, selIndex) {
										var selectedIndex = record.get('id');
										this.genePreset = this.ownerCt.genePresets[selectedIndex];
										this.applySortFilter();
			        		}
			        	}
			        },
			        {
			        	xtype  : 'label',
			        	text   : '% missing data filter:',
						height : 15
			        },			        
			        {
			        	xtype  : 'slider',
			        	ref    : 'sldGeneDataMissingFilter',
			        	height : 20,
			        	value  : 100,
			        	increment : 1,
			        	minValue  : 1,
			        	maxValue  : 100,
			        	plugins: new Ext.slider.Tip({
			                 getText: function(thumb){
			                     return String.format('{0}% missing', thumb.value);
			                 }
			            }),
			        	listeners : {
			        			changecomplete : function (slider, newValue, thumb) {
										this.applySortFilter();
			        				}, scope: this
			        	}					          		        	            
			        },
			        {
			        	xtype  : 'label',
			        	text   : 'Sum pValue filter:'
			        },			        
			        {
			        	xtype  : 'slider',
			        	ref    : 'sldGenePvalueFilter',
			        	value  : 1,
			        	increment : 1,
			        	minValue  : 1,
			        	maxValue  : 100,
			        	plugins: new Ext.slider.Tip({
			                 getText: function(thumb){
			                     return String.format('{0} not sure what', thumb.value/100);
			                 }
			            }),
			        	listeners : {
			        			changecomplete : function (slider, newValue, thumb) {
									this.applySortFilter();
			        			}, scope: this
			        	}					          		        	            
			        },			        
			        {
			        	xtype: 'Metaheatmap.FactorTree',
			        	ref :'factorTree',
			        	sortedTree : this.sortedTree,
			        	autoScroll : true,
			        	//height : 200,
			        	//bodyStyle : 'padding-bottom:5px',
			        	border : false, 
			        	flex : 2
			        }
			   ]
			 
		});
				
				
		Gemma.Metaheatmap.ControlPanel.superclass.initComponent.apply (this, arguments);
			
		this.addEvents('gene_zoom_change','condition_zoom_change');
				
		/************************* Selection grids ****************************************/
		
//		var SelectionRecord = Ext.data.Record.create([
//			{ name: 'id'},
//			{ name: 'shortName'},
//			{ name: 'longName'}
//		]);
//		var eeSelectionStore = new Ext.data.Store({
//			reader: new Ext.data.ArrayReader({
//				idIndex: 0
//			}, SelectionRecord)
//		});
//		var geneSelectionStore = new Ext.data.Store({
//			reader: new Ext.data.ArrayReader({
//				idIndex: 0
//			}, SelectionRecord)
//		});
//		var eeSelectionData = [];
//		var lastDatasetId = null;
//		for (i = 0; i < this.visualizationData.resultSetValueObjects.length; i++) { // for every ee group
//			for (j = 0; j < this.visualizationData.resultSetValueObjects[i].length; j++) { // for every col
//				// get the experiments, they should be grouped by datasetId
//				if (this.visualizationData.resultSetValueObjects[i][j].datasetId != lastDatasetId) {
//					eeSelectionData.push([this.visualizationData.resultSetValueObjects[i][j].datasetId,
//											this.visualizationData.resultSetValueObjects[i][j].datasetShortName,
//											this.visualizationData.resultSetValueObjects[i][j].datasetName]);
//				}
//				lastDatasetId = this.visualizationData.resultSetValueObjects[i][j].datasetId;
//			}
//		}
//		eeSelectionStore.loadData(eeSelectionData);
//		var geneSelectionData = [];
//		for (i = 0; i < this.visualizationData.geneIds.length; i++) {
//			for (j = 0; j < this.visualizationData.geneIds[i].length; j++) {
//				geneSelectionData.push( [ this.visualizationData.geneIds[i][j], 
//										  this.visualizationData.geneNames[i][j], 
//										  this.visualizationData.geneFullNames[i][j] ] );
//			}
//		}
//		geneSelectionStore.loadData( geneSelectionData );
//		
//		this.eeSelectionList = new Ext.grid.GridPanel({ // Tried listView but it was very very hard to select an entry by gene id.
//			store : eeSelectionStore,
//			multiSelect : true,
//			hideHeaders : true,
//			stripeRows : true,
//			viewConfig : {
//				forceFit : true
//			},
//			columns : [{
//					dataIndex : 'shortName',
//					renderer : function(value, metadata, record, row, col, ds) {
//						return String
//								.format(
//										"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={0}'>{1}</a> "+
//										"<span style='font-color:grey; white-space:normal !important;'>{2}</span> ",
//										record.data.id, record.data.shortName, record.data.longName);
//					}
//				}]
//		});
//				
//		this.geneSelectionList = new Ext.grid.GridPanel({
//			store:geneSelectionStore,
//			multiSelect:true,
//			hideHeaders:true,
//			stripeRows : true,
//			viewConfig: {
//				forceFit: true
//			},
//			columns : [{
//					dataIndex : 'shortName',
//					renderer : function(value, metadata, record, row, col, ds) {
//						return String
//								.format(
//										"<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a> "+
//										"<span style='font-color:grey; white-space:normal !important;'>{2}</span> ",
//										record.data.id, record.data.shortName, record.data.longName);
//					}
//				}]
//		});
//		
//		Ext.apply(this,{
//			_selectedGenes : [], // array of ids of selected genes
//			_selectedExperiments : [] // array of ids of selected experiments
//		});
//		
//		/* selection control */
//		this.geneSelectionList.getSelectionModel().on('rowselect', function(selModel, rowIndex, record){
//			this._selectedGenes.push(record.data.id);
//			this._imageArea._geneLabels.items.each(function(){ // for each gene group
//				this._drawLabels();
//			});
//			this._imageArea._heatmapArea.items.each(function(){ // redraw each column with row cells selected
//				this.items.each(function(){
//					this.items.each(function(){
//						this.items.each(function(){
//							this.items.each(function(){
//								if (this.xtype !== 'button'){
//									this.drawHeatmapSubColumn_();									
//								}
//							});
//						});
//					});
//				});
//			});				
//			
//		},this);
//		this.geneSelectionList.getSelectionModel().on('rowdeselect', function(selModel, rowIndex, record){
//			this._selectedGenes.remove(record.data.id);
//			this._imageArea._geneLabels.items.each(function(){
//				this._drawLabels();
//			});
//			this._imageArea._heatmapArea.items.each(function(){ // redraw each column with row cells selected
//				this.items.each(function(){
//					this.items.each(function(){
//						this.items.each(function(){
//							this.items.each(function(){
//								if (this.xtype !== 'button'){
//									this.drawHeatmapSubColumn_();									
//								}
//							});
//						});
//					});
//				});
//			});	
//		},this);
//		
//		this.on('geneSelectionChange', function(geneId){ // fired when a gene is selected from the image
//			var rec = this.geneSelectionList.getStore().getById(geneId);
//			var index = this.geneSelectionList.getStore().indexOfId(geneId);
//			if (this._selectedGenes.indexOf(geneId) == -1) {
//				this.geneSelectionList.getSelectionModel().selectRecords([rec],true);
//			}else{
//				this.geneSelectionList.getSelectionModel().deselectRow(index);
//			}
//			this._toolPanels._selectionTabPanel.setActiveTab(1);
//		});
//		
//		this.eeSelectionList.getSelectionModel().on('rowselect', function(selModel, rowIndex, record){
//			this._selectedExperiments.push(record.data.id);
//			// update viz
//			//this._imageArea._experimentLabels.items.each(function(){
//			//	this._drawLabels();
//			//});
//		},this);
//		this.eeSelectionList.getSelectionModel().on('rowdeselect', function(selModel, rowIndex, record){
//			this._selectedExperiments.remove(record.data.id);
//			// update viz
//			//this._imageArea._experimentLabels.items.each(function(){
//			//	this._drawLabels();
//			//});
//		},this);
//				
//		this.on('experimentSelectionChange', function(){ // fired when an experiment is selected from the image
//			var rec = this.eeSelectionList.getStore().getById(experimentId);
//			var index = this.eeSelectionList.getStore().indexOfId(experimentId);
//			if (this._selectedExperiments.indexOf(experimentId) == -1) {
//				this.eeSelectionList.getSelectionModel().selectRecords([rec],true);
//			}else{
//				this.eeSelectionList.getSelectionModel().deselectRow(index);
//			}
//			this._toolPanels._selectionTabPanel.setActiveTab(0);
//		});
//				
//		this.geneSelectionEditor = new Gemma.GeneMembersGrid({
//			name: 'geneSelectionEditor',
//			hideHeaders: true,
//			frame: false,
//			allowSaveToSession: false
//		});
//
//		this.geneSelectionEditorWindow = new Ext.Window({
//			closable : false,
//			layout : 'fit',
//			width : 450,
//			height : 500,
//			items : this.geneSelectionEditor,
//			title : 'Edit Your Gene Selection'
//		});
//				
//		this.geneSelectionEditor.on('doneModification', function() {
//			this.geneSelectionEditorWindow.hide();
//		}, this);
//				
//		Ext.apply(this,{
//			
//			launchGeneSelectionEditor : function() {
//
//				var geneRecords = this.geneSelectionList.getSelectionModel().getSelections();
//				if (!geneRecords || geneRecords === null || geneRecords.length === 0) {
//					return;
//				}
//		
//				this.geneSelectionEditorWindow.show();
//		
//				this.geneSelectionEditor.loadMask = new Ext.LoadMask(this.geneSelectionEditor.getEl(), {
//							msg : "Loading genes ..."
//						});
//				this.geneSelectionEditor.loadMask.show();
//				Ext.apply(this.geneSelectionEditor, {
//							geneGroupId : null,
//							selectedGeneGroup : null,
//							groupName : null,
//							taxonId : this.taxonId,
//							taxonName : this.taxonName
//						});
//				this.geneSelectionEditor.loadGenes(this._selectedGenes, function() {
//							this.geneSelectionEditor.loadMask.hide();
//						}.createDelegate(this, [], false));
//			}
//		});
//						
//		this.eeSelectionEditor = new Gemma.ExpressionExperimentMembersGrid({
//			name: 'eeSelectionEditor',
//			height: 200,
//			hideHeaders: true,
//			frame: false,
//			allowSaveToSession: false
//		});
//
//		this.eeSelectionEditorWindow = new Ext.Window({
//			closable : false,
//			layout : 'fit',
//			width : 450,
//			height : 500,
//			items : this.eeSelectionEditor,
//			title : 'Edit Your Experiment Selection'
//		});
//				
//		this.eeSelectionEditor.on('doneModification', function() {
//			this.eeSelectionEditorWindow.hide();
//		}, this);
//				
//		Ext.apply (this,{
//			
//			launchExperimentSelectionEditor : function() {
//
//				var eeRecords = this.eeSelectionList.getSelectionModel().getSelections();
//				if (!eeRecords || eeRecords === null || eeRecords.length === 0) {
//					return;
//				}
//		
//				this.eeSelectionEditorWindow.show();
//		
//				this.eeSelectionEditor.loadMask = new Ext.LoadMask(this.eeSelectionEditor.getEl(), {
//							msg : "Loading ees ..."
//						});
//				this.eeSelectionEditor.loadMask.show();
//				Ext.apply(this.eeSelectionEditor, {
//							eeGroupId : null,
//							selectedExperimentGroup : null,
//							groupName : null,
//							taxonId : this.taxonId,
//							taxonName : this.taxonName
//						});
//				this.eeSelectionEditor.loadExperiments(this._selectedExperiments, function() {
//							this.eeSelectionEditor.loadMask.hide();
//						}.createDelegate(this, [], false));
//			}
//		});


		/*************** end of selection grids **********************/	
		
	},
	
	makeFilterFunction : function (filterString) {
		return function (o) {
			return (o.contrastFactorValue == filterString);
		};
	},
	
	applySortFilter : function () {
		var genePercentMissingThreshold = this.sldGeneDataMissingFilter.getValue() / 100;
		var genePercentMissingFilter = [{'filterFn' : function (o) {return o.percentProbesMissing > genePercentMissingThreshold;} }];

		var conditionPercentMissingThreshold = this.sldConditionDataMissingFilter.getValue() / 100;
		var conditionPercentMissingFilter = [{'filterFn' : function (o) {return o.percentProbesMissing > conditionPercentMissingThreshold;} }];
		
		var specificityThreshold = this.sldSpecificityFilter.getValue();
		var specificityFilter = [{'filterFn' : function (o) {return o.miniBarValue > specificityThreshold;} }];

		var geneThreshold = this.sldGenePvalueFilter.getValue() / 100;
		var genePvalueFilter = [{'filterFn' : function (o) {return o.inverseSumPvalue < geneThreshold;} }];
				
		var conditionThreshold = this.sldConditionPvalueFilter.getValue() / 100;
		var conditionPvalueFilter = [{'filterFn' : function (o) {return o.inverseSumPvalue < conditionThreshold;} }];
		
		var conditionSort = [];
		conditionSort = conditionSort.concat (this.conditionPreset.sort);
		
		var conditionFilter = [];
		conditionFilter = conditionFilter.concat (specificityFilter);
		conditionFilter = conditionFilter.concat (conditionPvalueFilter);
		conditionFilter = conditionFilter.concat (conditionPercentMissingFilter);		
		conditionFilter = conditionFilter.concat (this.factorTreeFilter);
				
		var geneSort = [];
		geneSort = geneSort.concat (this.genePreset.sort);

		var geneFilter = [];
		geneFilter = geneFilter.concat (genePvalueFilter);
		geneFilter = geneFilter.concat (genePercentMissingFilter);
		
		this.fireEvent('applySortGroupFilter', geneSort, geneFilter, conditionSort, conditionFilter);					
	},
	
	onRender : function() {
		Gemma.Metaheatmap.ControlPanel.superclass.onRender.apply (this, arguments);
		
		this.addEvents('applySortGroupFilter');
		
		this.cmbGenePresets.setValue (0);
		this.genePreset = this.ownerCt.genePresets[0]
		this.cmbConditionPresets.setValue (0);
		this.conditionPreset = this.ownerCt.conditionPresets[0];
		
		this.factorTree.on('checkchange', function(node, checked) {
			if (node.isLeaf()) {
				
			} else {
				// Propagate choice to children.
				for (var i = 0; i < node.childNodes.length; i++) {
					var child = node.childNodes[i];
					child.ui.toggleCheck (checked);
					child.attributes.checked = checked;
				}
			}
						
			// Go through factorTree and create filter functions for unchecked factor values.
			this.factorTreeFilter = [];
			var root = this.factorTree.root;
			for (var i = 0; i < root.childNodes.length; i++) {
				var categoryNode = root.childNodes[i];
				for (var j = 0; j < categoryNode.childNodes.length; j++) {
					var factorNode = categoryNode.childNodes[j];
					if (factorNode.attributes.checked === false) {
						this.factorTreeFilter.push ({'filterFn' : this.makeFilterFunction (factorNode.text) });
					}
				}
			}
			
			this.applySortFilter();
		}, this);
				
	}
	
});

Ext.reg('Metaheatmap.ControlPanel', Gemma.Metaheatmap.ControlPanel);


/// Initialize sort/filter from savedState

// Restore sorting from URL
//var rec, index;
//if (this.initGeneSort){
//	this.toolPanel_._sortPanel._geneSort.setValue(this.initGeneSort);
//	rec = this.toolPanel_._sortPanel._geneSort.getStore().getById(this.initGeneSort);
//	index = this.toolPanel_._sortPanel._geneSort.getStore().indexOfId(this.initGeneSort);
//	if (index !== -1) {
//		this.toolPanel_._sortPanel._geneSort.fireEvent('select', this.toolPanel_._sortPanel._geneSort, rec, index);
//	}
//}
//if (this.initExperimentSort){
//	this.toolPanel_._sortPanel._experimentSort.setValue(this.initExperimentSort);
//	rec = this.toolPanel_._sortPanel._experimentSort.getStore().getById(this.initExperimentSort);
//	index = this.toolPanel_._sortPanel._experimentSort.getStore().indexOfId(this.initExperimentSort);
//	if (index !== -1) {
//		this.toolPanel_._sortPanel._experimentSort.fireEvent('select', this.toolPanel_._sortPanel._experimentSort, rec, index);
//	}
//	
//}
//
//// restore filtering from URL
//if(this.initFactorFilter){
//	var j = 0;
//	for(j = 0; j < this.initFactorFilter.length; j++){
//		this.tree.getNodeById(this.initFactorFilter[j]).getUI().toggleCheck(false);
//	}
//}		



//Gemma.Metaheatmap.SortGroupPanel = Ext.extend (Ext.Panel, {
//
//	initComponent : function() {
//		Ext.apply (this, {
//			width  : 300,
//			height : 180,
//			layout : 'vbox'				
//		});
//			
//		Gemma.Metaheatmap.SortGroupPanel.superclass.initComponent.apply (this, arguments);
//		
//		
//		this.add({xtype:'panel', layout: 'hbox', ref:'pnlSortGroupLabel', width:300, height:25, 
//																		  items:[{xtype:'button',ref:'btnAddSortGroup',text:'+', width: 20},
//		                                                                     {xtype:'label', width: 130, height: 25, text:'Sort by:'},
//		                                                                     {xtype:'label', width: 100, height: 25, text:'Group by:'},
//		                                                                     {xtype:'button', ref:'btnApply', text:'Apply', width: 30}
//		                                                                  ]});
//		                                                                  
//
//	},
//
//
//
//		onRender : function() {
//			Gemma.Metaheatmap.SortGroupPanel.superclass.onRender.apply (this, arguments);
//									
//			this.pnlSortGroupLabel.btnApply.on('click', function() {
//				var sortGroupArray = [];
//				var filterArray = [];
//				var i;
//				var sortGroupItem, sortProperty, groupProperty;
//				
//				for (i = 1; i < this.items.getCount(); i++ ) {
//					sortProperty  = this.items.get(i).cmbSort.getValue();
//					groupProperty = this.items.get(i).cmbGroup.getValue(); 
//					sortGroupItem = {'sortFn'  : Gemma.Metaheatmap.Utils.createSortByPropertyFunction (sortProperty),
//									 'groupBy' : groupProperty};
//					if (sortGroupItem.groupBy === 'null') {sortGroupItem.groupBy = null;}
//					sortGroupArray.push (sortGroupItem);
//				}
//				
//				// Go through factorTree and create filter functions.
//				
//				var root = this.ownerCt.factorTree.root;
//				for (i = 0; i < root.childNodes.length; i++) {
//					var categoryNode = root.childNodes[i];
//					for (var j = 0; j < categoryNode.childNodes.length; j++) {
//						var factorNode = categoryNode.childNodes[j];
//						if (factorNode.attributes.checked === false) {
//								filterArray.push ({'filterFn' : this.makeFilterFunction(factorNode.text) });
//						}
//					}
//				}
//				
//				// Add other filters
//				
//				
//				
//							
//				this.ownerCt.ownerCt.fireEvent('applySortGroupFilter', sortGroupArray, filterArray);
//			}, this);
//
//			
//			this.pnlSortGroupLabel.btnAddSortGroup.on('click', function() {
//				var row = new Ext.Panel({
//					width : 300,
//					height : 25,
//					layout : 'hbox',
//					items : [
//					         {
//					        	 xtype: 'button',
//					        	 text : '-',
//					        	 width : 20,
//					        	 listeners: {'click' : {
//					        	 		fn : function(btn,e) {this.remove(btn.ownerCt); this.doLayout();},				        	 	
//					        	 		scope: this
//					         		}
//					         	}
//					         },
//					         {
//					        	 xtype: 'combo',
//					        	 ref : 'cmbSort',
//					        	 mode 		  : 'local',
//					        	 displayField : 'text',
//					        	 valueField   : 'name',
//					        	 width 		  : 130,
//					        	 editable 	  : 'false',
//					        	 forceSelection : 'true',
//					        	 triggerAction  : 'all',
//					        	 store: new Ext.data.ArrayStore({
//					        		 fields : ['name', 'text'],
//					        		 data : [['factorCategory', 'Factor Category'], ['inverseSumQvalue', 'inverse sum Qvalues'], ['miniPieValue', 'diff. exp. specificity']],
//					        		 idIndex : 0
//					        	 })
//					         },
//					         {
//					        	 xtype		  : 'combo',
//					        	 ref : 'cmbGroup',
//					        	 mode 		  : 'local',
//					        	 displayField : 'text',
//					        	 valueField   : 'name',
//					        	 width 		  : 130,
//					        	 editable 	  : 'false',
//					        	 forceSelection : 'true',
//					        	 triggerAction  : 'all',
//					        	 store: new Ext.data.ArrayStore({
//					        		 fields : ['name', 'text'],
//					        		 data : [['null', '--'], ['experimentGroupId', 'Experiment Group'], ['factorCategory', 'Factor Category']],
//					        		 idIndex : 0
//					        	 })
//					         }		         
//					        ]
//				});
//				
//				this.add(row);
//				this.doLayout();
//			}, this);
//
//	}
//
//	});
//
//	Ext.reg('Metaheatmap.SortGroupPanel', Gemma.Metaheatmap.SortGroupPanel);
//{
//title : 'Sorting & Grouping',
//ref   : 'pnlSortGroup',
//flex  : 1, // because more space for this panel doesn't help
//width 		: this.width,
//collapsible : true,
//border 		: true,
//bodyBorder 	: true,
//layout 		: 'form',
//labelWidth 	: 115,
//bodyStyle 	: 'padding:5px',
//defaults : {
//	hideLabel: false
//},
//items : [{xtype:'Metaheatmap.SortGroupPanel'},
//         {
//	xtype 		   : 'combo',
//	ref   		   : 'cmbSortCondition',
//	hiddenName     : 'conditionSort',
//	fieldLabel     : 'Sort conditions by',
//	fieldTipTitle  : 'Sort Conditions By:',
//	fieldTipHTML   : Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieldTipHTML,
//	fieldTip 	   : Gemma.Metaheatmap.Strings.ControlPanel.SortConditionCombo.fieledTip ,
//	mode 		   : 'local',
//	displayField   : 'text',
//	valueField 	   : 'name',
//	width 		   : 150,
//	editable 	   : 'false',
//	forceSelection : 'true',
//	triggerAction  : 'all',
//	store: new Ext.data.ArrayStore({
//		fields : ['name', 'text'],
//		data : [['factorCategory', '--'], ['qValues', 'q values'], ['specificity', 'diff. exp. specificity']],
//		idIndex : 0
//	}),
//	listeners: {
//		select : function (field, record, selIndex) {
//			var selectedProperty = record.get('name');
//			var sortFn = Gemma.Metaheatmap.Utils.createSortByPropertyFunction (selectedProperty);							
//			var sortGroupList = [{'sortFn' : sortFn , 'groupBy' : null}];
//
////			this.refreshVisualization();
//		},
//		render: function (combo){
//			combo.setValue('--');
//		},
//		scope: this
//	}
//}, {
//	xtype 	   : 'combo',
//	hiddenName : 'geneSort',
//	ref 	   : 'cmbSortGene',
//	fieldLabel : 'Sort genes by',
//	fieldTipTitle : 'Sort Genes By:',
//	fieldTipHTML  : Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo.fieldTipHTML,
//	fieldTip 	  : Gemma.Metaheatmap.Strings.ControlPanel.SortGeneCombo.fieldTip,
//	mode		  : 'local',
//	displayField  : 'text',
//	valueField	  : 'name',
//	width		  : 150,
//	editable 	  : 'false',
//	forceSelection: 'true',
//	triggerAction : 'all',
//	store : new Ext.data.ArrayStore({
//		fields : ['name', 'text'],
//		data : [['symbol', 'symbol'], ['score', 'q values']],
//		idIndex : 0
//	}),
//	listeners: {
//		select: function (field, record, selIndex) {
//			var selectedProperty = record.get('name');
//			var sortFn = Gemma.Metaheatmap.Utils.createSortByPropertyFunction (selectedProperty);
//			var sortGroupList = [{'sortFn' : this.createSortByPropertyFunction_ ('groupName'), 'groupBy' : 'groupName'},{'sortFn' : sortFn , 'groupBy' : null}];
//		},				
//		render: function(combo){
//			combo.setValue('symbol');
//		},
//		scope: this
//	}
//},
//}, 
//{
//	title		: 'Filtering',
//	ref			: 'pnlFilter',
//	collapsible : true,
//	collapsed	: false,
//	border		: true,
//	bodyBorder	: true,
//	autoScroll	: true,
//	padding		: 10,
//	layout: 'hbox',
//	defaults: {
//		border: false
//	},
//	items: [					   
//		  ]
//		items :[
//		    {
//				layout: 'form',
//				labelWidth: 140,
//				items: [this.qValueFilterField]
//		    },
//		    this.qValueFilterButton
//		]
//	},
//}, 
//{
//		ref: 'tpnlSelection',
//		width: this.width,
//		xtype: 'tabpanel',
//		defaults: {
//			layout: 'fit'
//		},
//		layoutConfig: {
//			monitorResize: true
//		},
//		activeTab: 0,
//		deferredRender: false,
//		items: [{
//			title: 'Select Experiments',
//			layout: 'fit',
//			autoScroll: true,
//			items: [], //this.eeSelectionList,
//			bbar: ['Hold \'ctrl\' to select > 1', '->', {
//				text: 'Save Selection',
//				icon: '/Gemma/images/icons/disk.png',
//				handler: this.launchExperimentSelectionEditor,
//				scope: this,
//				tooltip: 'Create a group of experiments from your selection'
//			}, '-', {
//				text: 'Clear'
//				handler: function(button, clickEvent){
//					this.eeSelectionList.getSelectionModel().clearSelections();
//					this.fireEvent('clearedExperimentSelections');
//				},
//				scope: this
//			}]
//		}, {
//			title: 'Select Genes',
//			autoScroll: true,
//			layout: 'fit',
//			items: [],//this.geneSelectionList,
//			bbar: ['Hold \'ctrl\' to select > 1', '->', {
//				text: 'Save Selection',
//				icon: '/Gemma/images/icons/disk.png',
//				handler: this.launchGeneSelectionEditor,
//				scope: this,
//				tooltip: 'Create a group of genes from your selection'
//			}, '-', {
//				text: 'Clear'
//				handler: function(button, clickEvent){
//					this.geneSelectionList.getSelectionModel().clearSelections();
//					this.fireEvent('clearedGeneSelections');
//				},
//				scope: this
//			}]
//		}
//	]}
//]					
