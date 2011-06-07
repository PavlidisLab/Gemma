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

		/************************* selection grids ****************************************/
		
		var SelectionRecord = Ext.data.Record.create([
			{ name: 'id'},
			{ name: 'shortName'},
			{ name: 'longName'}
		]);
		var eeSelectionStore = new Ext.data.Store({
			reader: new Ext.data.ArrayReader({
				idIndex: 0
			}, SelectionRecord)
		});
		var geneSelectionStore = new Ext.data.Store({
			reader: new Ext.data.ArrayReader({
				idIndex: 0
			}, SelectionRecord)
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
			for (j = 0; j < this.visualizationData.geneIds[i].length; j++) {
				geneSelectionData.push([this.visualizationData.geneIds[i][j], 
											this.visualizationData.geneNames[i][j], 
											this.visualizationData.geneFullNames[i][j]]);
			}
		}
		geneSelectionStore.loadData(geneSelectionData);
		
		this.eeSelectionList = new Ext.grid.GridPanel({ // tried listView but it was very very hard to select an entry by gene id
			store:eeSelectionStore,
			multiSelect:true,
			hideHeaders:true,
			stripeRows : true,
			viewConfig: {
				forceFit: true
			},
			columns : [{
					dataIndex : 'shortName',
					renderer : function(value, metadata, record, row, col, ds) {
						return String
								.format(
										"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={0}'>{1}</a> "+
										"<span style='font-color:grey; white-space:normal !important;'>{2}</span> ",
										record.data.id, record.data.shortName, record.data.longName);
					}
				}]
		});
				
		this.geneSelectionList = new Ext.grid.GridPanel({
			store:geneSelectionStore,
			multiSelect:true,
			hideHeaders:true,
			stripeRows : true,
			viewConfig: {
				forceFit: true
			},
			columns : [{
					dataIndex : 'shortName',
					renderer : function(value, metadata, record, row, col, ds) {
						return String
								.format(
										"<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a> "+
										"<span style='font-color:grey; white-space:normal !important;'>{2}</span> ",
										record.data.id, record.data.shortName, record.data.longName);
					}
				}]
		});
		
		Ext.apply(this,{
			_selectedGenes : [], // array of ids of selected genes
			_selectedExperiments : [], // array of ids of selected experiments
		});
		
		/* selection control */
		this.geneSelectionList.getSelectionModel().on('rowselect', function(selModel, rowIndex, record){
			this._selectedGenes.push(record.data.id);
			this._imageArea._geneLabels.items.each(function(){ // for each gene group
				this._drawLabels();
			});
			this._imageArea._heatmapArea.items.each(function(){ // redraw each column with row cells selected
				this.items.each(function(){
					this.items.each(function(){
						this.items.each(function(){
							this.items.each(function(){
								if (this.xtype !== 'button'){
									this._drawHeatmapColumn();									
								}
							});
						});
					});
				});
			});				
			
		},this);
		this.geneSelectionList.getSelectionModel().on('rowdeselect', function(selModel, rowIndex, record){
			this._selectedGenes.remove(record.data.id);
			this._imageArea._geneLabels.items.each(function(){
				this._drawLabels();
			});
			this._imageArea._heatmapArea.items.each(function(){ // redraw each column with row cells selected
				this.items.each(function(){
					this.items.each(function(){
						this.items.each(function(){
							this.items.each(function(){
								if (this.xtype !== 'button'){
									this._drawHeatmapColumn();									
								}
							});
						});
					});
				});
			});	
		},this);
		
		this.on('geneSelectionChange', function(geneId){ // fired when a gene is selected from the image
			var rec = this.geneSelectionList.getStore().getById(geneId);
			var index = this.geneSelectionList.getStore().indexOfId(geneId);
			if (this._selectedGenes.indexOf(geneId) == -1) {
				this.geneSelectionList.getSelectionModel().selectRecords([rec],true);
			}else{
				this.geneSelectionList.getSelectionModel().deselectRow(index);
			}
			this._toolPanels._selectionTabPanel.setActiveTab(1);
		});
		
		this.eeSelectionList.getSelectionModel().on('rowselect', function(selModel, rowIndex, record){
			this._selectedExperiments.push(record.data.id);
			// update viz
			//this._imageArea._experimentLabels.items.each(function(){
			//	this._drawLabels();
			//});
		},this);
		this.eeSelectionList.getSelectionModel().on('rowdeselect', function(selModel, rowIndex, record){
			this._selectedExperiments.remove(record.data.id);
			// update viz
			//this._imageArea._experimentLabels.items.each(function(){
			//	this._drawLabels();
			//});
		},this);
				
		this.on('experimentSelectionChange', function(){ // fired when an experiment is selected from the image
			var rec = this.eeSelectionList.getStore().getById(experimentId);
			var index = this.eeSelectionList.getStore().indexOfId(experimentId);
			if (this._selectedExperiments.indexOf(experimentId) == -1) {
				this.eeSelectionList.getSelectionModel().selectRecords([rec],true);
			}else{
				this.eeSelectionList.getSelectionModel().deselectRow(index);
			}
			this._toolPanels._selectionTabPanel.setActiveTab(0);
		});
				
		this.geneSelectionEditor = new Gemma.GeneMembersGrid({
			name: 'geneSelectionEditor',
			height: 200,
			hideHeaders: true,
			frame: false,
			allowSaveToSession: false
		});

		this.geneSelectionEditorWindow = new Ext.Window({
			closable : false,
			layout : 'fit',
			items : this.geneSelectionEditor,
			title : 'Edit Your Gene Selection'
		});
				
		this.geneSelectionEditor.on('doneModification', function() {
			this.geneSelectionEditorWindow.hide();
		}, this);
				
		Ext.apply(this,{
			
			launchGeneSelectionEditor : function() {

				var geneRecords = this.geneSelectionList.getSelectionModel().getSelections();
				if (!geneRecords || geneRecords === null || geneRecords.length === 0) {
					return;
				}
		
				this.geneSelectionEditorWindow.show();
		
				this.geneSelectionEditor.loadMask = new Ext.LoadMask(this.geneSelectionEditor.getEl(), {
							msg : "Loading genes ..."
						});
				this.geneSelectionEditor.loadMask.show();
				Ext.apply(this.geneSelectionEditor, {
							geneGroupId : null,
							selectedGeneGroup : null,
							groupName : null,
							taxonId : this.taxonId,
							taxonName : this.taxonName
						});
				this.geneSelectionEditor.loadGenes(this._selectedGenes, function() {
							this.geneSelectionEditor.loadMask.hide();
						}.createDelegate(this, [], false));
			}
		});
						
		this.eeSelectionEditor = new Gemma.ExpressionExperimentMembersGrid({
			name: 'eeSelectionEditor',
			height: 200,
			hideHeaders: true,
			frame: false,
			allowSaveToSession: false
		});

		this.eeSelectionEditorWindow = new Ext.Window({
			closable : false,
			layout : 'fit',
			items : this.eeSelectionEditor,
			title : 'Edit Your Experiment Selection'
		});
				
		this.eeSelectionEditor.on('doneModification', function() {
			this.eeSelectionEditorWindow.hide();
		}, this);
				
		Ext.apply(this,{
			
			launchExperimentSelectionEditor : function() {

				var eeRecords = this.eeSelectionList.getSelectionModel().getSelections();
				if (!eeRecords || eeRecords === null || eeRecords.length === 0) {
					return;
				}
		
				this.eeSelectionEditorWindow.show();
		
				this.eeSelectionEditor.loadMask = new Ext.LoadMask(this.eeSelectionEditor.getEl(), {
							msg : "Loading ees ..."
						});
				this.eeSelectionEditor.loadMask.show();
				Ext.apply(this.eeSelectionEditor, {
							eeGroupId : null,
							selectedExperimentGroup : null,
							groupName : null,
							taxonId : this.taxonId,
							taxonName : this.taxonName
						});
				this.eeSelectionEditor.loadExperiments(this._selectedExperiments, function() {
							this.eeSelectionEditor.loadMask.hide();
						}.createDelegate(this, [], false));
			}
		});


		/*************** end of selection grids **********************/



		this.TOTAL_NUMBER_OF_COLUMNS = 0;
		var datasetGroupIndex;
		for (datasetGroupIndex = 0; datasetGroupIndex < this.visualizationData.resultSetValueObjects.length; datasetGroupIndex++) {
			this.TOTAL_NUMBER_OF_COLUMNS = this.TOTAL_NUMBER_OF_COLUMNS +
					this.visualizationData.resultSetValueObjects[datasetGroupIndex].length;
		}
		this._heatMapWidth = this.TOTAL_NUMBER_OF_COLUMNS * 1 *
				(Gemma.MetaVisualizationConfig.cellWidth * 1 + Gemma.MetaVisualizationConfig.columnSeparatorWidth * 1) *
				1 + Gemma.MetaVisualizationConfig.groupSeparatorWidth *
				(this.visualizationData.resultSetValueObjects.length - 1) * 1;

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
				text : '<b>Bookmarkable Link</b>',
				//icon : '/Gemma/images/download.gif',
				//cls : 'x-btn-text-icon',
				qtip:'Get a link to re-run this search',
				handler : function() {
					this.getBookmarkableLink();
				},
				scope:this
			},'-',{
				xtype : 'button',
				x : 5,
				y : 5,
				text : '<b>Download</b>',
				icon : '/Gemma/images/download.gif',
				cls : 'x-btn-text-icon',
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
				closable : false,
				shadow : false,
				border : false,
				bodyBorder : false,
				hidden: true, // doesn't work for some reason
				bodyStyle : 'padding: 7px',
				html : '<span style="color:black;font-size:1.3em">Hover over the visualisation for quick details or click for more information.'+
						' <br><br>Hold down "ctrl" and click on a gene to select it.</span>',
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
				ref: '_toolPanels',
				height: Gemma.MetaVisualizationConfig.panelHeight - 30,
				width: 300,
				x: Gemma.MetaVisualizationConfig.panelWidth - 300,
				y: 0,
				layout: 'vbox',
				border: true,
				applicationRoot: this,
				items: [{
					title: 'Sort',
					ref:'_sortPanel',
					flex: 0,
					width: 300,
					collapsible: true,
					// collapsed:true,
					border: true,
					bodyBorder: true,
					layout: 'form',
					bodyStyle: 'padding:5px',
					defaults: {
						hideLabel: false
					},
					items: [{
						xtype: 'combo',
						ref:'_experimentSort',
						hiddenName: 'conditionSort',
						fieldLabel: 'Sort experiments by',
						mode: 'local',
						displayField: 'text',
						valueField: 'name',
						width: 150,
						editable: 'false',
						forceSelection: 'true',
						triggerAction: 'all',
						store: new Ext.data.ArrayStore({
							fields: ['name', 'text'],
							data: [['experiment', 'experiment'], ['qValues', 'sum of q Values'], ['specificity', 'specificity']],
							idIndex:0
						}),
						listeners: {
							select: function(field, record, selIndex){
							
								if (record.get('name') === 'qValues') {
									this._sortColumns('DESC', function(o1, o2){
									
										return o1.overallDifferentialExpressionScore -
										o2.overallDifferentialExpressionScore;
									});
									this.doLayout();
								}
								else 
									if (record.get('name') === 'specificity') {
										this._sortColumns('ASC', function(o1, o2){
										
											return o1.specificityScore - o2.specificityScore;
										});
										
										this.doLayout();
									}
									else 
										if (record.get('name') === 'experiment') {
											this._sortColumns('ASC', function(o1, o2){
											
												return (o1.datasetName >= o2.datasetName) ? 1 : -1;
											});
											
											this.doLayout();
										}
								this.refreshVisualization();
							},
							render: function(combo){
								combo.setValue('experiment');
							},
							scope: this
						}
					}, {
						xtype: 'combo',
						hiddenName: 'geneSort',
						ref:'_geneSort',
						fieldLabel: 'Sort genes by',
						mode: 'local',
						displayField: 'text',
						valueField: 'name',
						width: 150,
						editable: 'false',
						forceSelection: 'true',
						triggerAction: 'all',
						store: new Ext.data.ArrayStore({
							fields: ['name', 'text'],
							data: [['symbol', 'symbol'], ['score', 'gene score']],
							idIndex:0
						}),
						listeners: {
							select: function(field, record, selIndex){
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
								}
								else 
									if (record.get('name') === 'score') {
										// Sort genes : changes gene order
										for (i = 0; i < this.geneScores[0].length; i++) {
											this.geneScores[0][i].sort(function(o1, o2){
												return o2.score - o1.score;
											});
										}
										for (geneGroupIndex = 0; geneGroupIndex < this._imageArea._heatmapArea.geneNames.length; geneGroupIndex++) {
											this.geneOrdering[geneGroupIndex] = [];
											for (i = 0; i < this._imageArea._heatmapArea.geneIds[geneGroupIndex].length; i++) {
												// if
												// (this.geneScores[0][geneGroupIndex][i].score
												// !== 0) {
												this.geneOrdering[geneGroupIndex].push(this.geneScores[0][geneGroupIndex][i].index);
											// }
											}
										}
									}
								this.refreshVisualization();
							},
							render: function(combo){
								combo.setValue('symbol');
							},
							scope: this
						}
					}]
				}, {
					title: 'Filter',
					flex: 1,
					collapsible: true,
					collapsed: false,
					border: true,
					bodyBorder: true,
					autoScroll: true,
					layout: 'form', // to get font sizes matching
					items: [{
						xtype: 'checkbox',
						hideLabel: true,
						boxLabel: 'Show columns with no results.',
						checked: true,
						hidden: true,
						listeners: {
							check: function(target, checked){
								var filteringFn = null;
								if (!checked) { // hide columns without
									// results
									filteringFn = function(o){
									
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
								}
								else { // show columns without
									// results but respect
									// filtering by factor
									var checkedNodeIds = this.tree.getChecked('id');
									// if column has same factor name as
									// checked node, show it, otherwise
									// hide it
									filteringFn = function(o){
										if (checkedNodeIds.indexOf(o.factorName.toLowerCase()) > -1) {
											return false; // don't
										// filter it
										}
										else {
											return true;
										}
									};
								}
								
								var num = this.filterColumns(filteringFn);
								this.TOTAL_NUMBER_OF_HIDDEN_COLUMNS = num;
								this.doLayout();
								this.refreshVisualization();
							},
							scope: this
						}
					}, this.tree]
				}, {
					ref: '_selectionTabPanel',
					flex: 1,
					width: 300,
					xtype:'tabpanel',
					activeTab: 0,
					deferredRender: false,
					items: [{
						title: 'Select Experiments',
						layout:'fit',
						autoScroll: true,
						items: this.eeSelectionList,
						bbar:['Hold \'ctrl\' and click to select > 1','->',{
							text: 'Save',
							icon: '/Gemma/images/icons/disk.png',
							handler: this.launchExperimentSelectionEditor,
							scope: this
						},'-',{
							text:'Clear',
							handler: function(button, clickEvent){
								this.eeSelectionList.getSelectionModel().clearSelections();
								this.fireEvent('clearedExperimentSelections');
							},
							scope:this
						}]
					}, {
						title: 'Select Genes',
						autoScroll: true,
						layout:'fit',
						items: this.geneSelectionList,
						bbar:['Hold \'ctrl\' and click to select > 1','->',{
							text: 'Save',
							icon: '/Gemma/images/icons/disk.png',
							handler: this.launchGeneSelectionEditor,
							scope: this
						},'-',{
							text:'Clear',
							handler: function(button, clickEvent){
								this.geneSelectionList.getSelectionModel().clearSelections();
								this.fireEvent('clearedGeneSelections');
							},
							scope:this
						}]
					}]
					
				}]
			},{
				ref : '_imageArea',
				autoScroll : true,
				layout : 'absolute',
				frame : false,
				border : false,
				width : Gemma.MetaVisualizationConfig.panelWidth - 300,
				height : Gemma.MetaVisualizationConfig.panelHeight - 30,
				items : [{
				x:5,
				y:5,
				xtype:'panel',
				html:'<span style="color:dimGrey;font-size:0.9em;line-height:1.6em"><b>Hover</b> for quick info<br>'+
						'<b>Click</b> for details<br><b>"ctrl" + click</b> to select genes</span>',
				border:false,
				width: 180,
				height: 100
			},{
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

		this._hoverDetailsPanel.hide();
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

		//this._hoverDetailsPanel.show();
		
		if(this.initGeneSort){
			this._toolPanels._sortPanel._geneSort.setValue(this.initGeneSort);
		}
		if(this.initExperimentSort){
			this._toolPanels._sortPanel._experimentSort.setValue(this.initExperimentSort);
		}
		
		// this.MiniWindowTool.show();
		// this.el.on('mousemove', function(event,target) {
		// if (this.miniWindowButton.pressed == true) {
		// var x = event.getPageX()+10;
		// var y = event.getPageY()+15;
		// this.MiniWindowTool.setPosition(x,y);
		// }
		// }, this );

	},
	afterRender : function() {
		Gemma.MetaHeatmapApp.superclass.afterRender.apply(this, arguments);
		
		// restore sorting from URL
		var rec; var index;
		if(this.initGeneSort){
			this._toolPanels._sortPanel._geneSort.setValue(this.initGeneSort);
			rec = this._toolPanels._sortPanel._geneSort.getStore().getById(this.initGeneSort);
			index = this._toolPanels._sortPanel._geneSort.getStore().indexOfId(this.initGeneSort);
			this._toolPanels._sortPanel._geneSort.fireEvent('select',this._toolPanels._sortPanel._geneSort, rec, index);
		}
		if(this.initExperimentSort){
			this._toolPanels._sortPanel._experimentSort.setValue(this.initExperimentSort);
			rec = this._toolPanels._sortPanel._experimentSort.getStore().getById(this.initExperimentSort);
			index = this._toolPanels._sortPanel._experimentSort.getStore().indexOfId(this.initExperimentSort);
			this._toolPanels._sortPanel._experimentSort.fireEvent('select',this._toolPanels._sortPanel._experimentSort, rec, index);
		}
		
		// restore filtering from URL
		if(this.initFactorFilter){
			var j = 0;
			for(j = 0; j < this.initFactorFilter.length; j++){
				this.tree.getNodeById(this.initFactorFilter[j]).getUI().toggleCheck(false);
			}
		}

	},
	refreshVisualization : function() {
		this._imageArea.topLabelsPanel.refresh();
		this._imageArea._heatmapArea.refresh();
		this._imageArea._geneLabels.refresh();
	},
	getVizState: function(){
		var state = {};
		// get gene group ids
		// if there are any session-bound groups, get query that made them
		state.geneGroupIds = [];
		state.geneIds = [];
		var i; var ref; var k = 0;
		for(i = 0; i < this._visualizationData.geneGroupReferences.length;i++){
			ref = this._visualizationData.geneGroupReferences[i];
			if (typeof ref.type !== 'undefined') {
				if(ref.type === 'databaseBackedGene'){
					state.geneIds.push(ref.id);
				}else if (ref.type.toLowerCase().indexOf('session') === -1 && ref.type.toLowerCase().indexOf('group') !== -1) {
					state.geneGroupIds.push(ref.id);
				}else {
					this.usingSessionGroup = true;
				}
			}
		}
		if(this.experimentSessionGroupQueries){
			state.experimentSessionGroupQueries = this.experimentSessionGroupQueries;
		}
		if(this.geneSessionGroupQueries){
			state.geneSessionGroupQueries = this.geneSessionGroupQueries;
		}
		
		// get experiment group ids
		// if there are any session-bound groups, get queries that made them
		state.eeGroupIds = [];
		state.eeIds = [];
		for(i = 0; i < this._visualizationData.datasetGroupReferences.length;i++){
			ref = this._visualizationData.datasetGroupReferences[i];
			if (typeof ref.type !== 'undefined') {
				if(ref.type === 'databaseBackedExperiment'){
					state.eeIds.push(ref.id);
				}else if (ref.type.toLowerCase().indexOf('session') === -1 && ref.type.toLowerCase().indexOf('group') !== -1) {
					state.eeGroupIds.push(ref.id);
				}else {
					this.usingSessionGroup = true;
				}
			}
		}
		
		// gene sort state
		state.geneSort = this._toolPanels._sortPanel._geneSort.getValue();
		
		// experiment sort
		state.eeSort = this._toolPanels._sortPanel._experimentSort.getValue();
		
		// filters
		var toFilter =[];
		var children= this.tree.getRootNode().childNodes;
		for(i = 0; i < children.length; i++){
			if(!children[i].attributes.checked){
				toFilter.push(children[i].id);
			}
		}
		state.factorFilters = toFilter;
		
		state.taxonId = this._visualizationData.taxonId;
		state.pvalue = this.pvalue;
		return state;
	},
		
	/**
	 * Create a URL that can be used to query the system.
	 * 
	 * @param state should have format:
	 * 	state.geneIds = array of gene ids that occur singly (not in a group): [7,8,9]
	 *  state.geneGroupIds = array of db-backed gene group ids: [10,11,12]
	 *  ^same for experiments^
	 *  state.geneSort
	 *  state.eeSort
	 *  state.filters = list of filters applied, values listed should be filtered OUT (note this 
	 *  	is the opposite heuristic as in viz) (done to minimize url length)
	 *  state.taxonId
	 * @return url string
	 */
	getBookmarkableLink : function() {
		var state = this.getVizState();
		if (!state) {
			return null;
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		url = url.replace('home2','metaheatmap');
		
		var noGenes = true;
		var noExperiments = true;
		
		url += "?";
		if( typeof state.geneIds !== 'undefined' &&  state.geneIds !== null &&  state.geneIds.length !== 0){
			url += String.format("g={0}&", state.geneIds.join(","));
			noGenes=false;
		}		
		if (typeof state.geneGroupIds !== 'undefined' && state.geneGroupIds !== null && state.geneGroupIds.length !== 0) {
			url += String.format("gg={0}&", state.geneGroupIds.join(","));
			noGenes=false;
		}
		if( typeof state.eeIds !== 'undefined' &&  state.eeIds !== null &&  state.eeIds.length !== 0){
			url += String.format("e={0}&", state.eeIds.join(","));
			noExperiments = false;
		}	
		if (typeof state.eeGroupIds !== 'undefined' && state.eeGroupIds !== null && state.eeGroupIds.length !== 0) {
			url += String.format("eg={0}&", state.eeGroupIds.join(","));
			noExperiments = false;
		}	
		if (typeof state.experimentSessionGroupQueries !== 'undefined' && 
				state.experimentSessionGroupQueries !== null && 
				state.experimentSessionGroupQueries.length !== 0) {
			url += String.format("eq={0}&", state.experimentSessionGroupQueries.join(","));
			noExperiments = false;
		}
		if (typeof state.geneSessionGroupQueries !== 'undefined' && 
				state.geneSessionGroupQueries !== null && 
				state.geneSessionGroupQueries.length !== 0) {
			url += String.format("gq={0}&", state.geneSessionGroupQueries.join(","));
			noGenes=false;
		}
		if (typeof state.geneSort !== 'undefined' && state.geneSort !== null && state.geneSort.length !== 0) {
			url += String.format("gs={0}&", state.geneSort);
		}
		if (typeof state.eeSort !== 'undefined' && state.eeSort !== null && state.eeSort.length !== 0) {
			url += String.format("es={0}&", state.eeSort);
		}
		if (typeof state.factorFilters !== 'undefined' && state.factorFilters !== null && state.factorFilters.length !== 0) {
			url += String.format("ff={0}&", state.factorFilters.join(','));
		}
		url += String.format("t={0}&", state.taxonId);
		url += String.format("p={0}&", state.pvalue);

		// remove trailing '&'
		url = url.substring(0, url.length-1);
		
		var warning = (this.selectionsModified)? "Please note: you have unsaved modifications in one or more of your"+
							" experiment and/or gene groups. <b>These changes will not be saved in this link.</b>"+
							" In order to keep your modifications, please log in and save your unsaved groups.<br><br>":"";
									
		warning += (this.usingSessionGroup)? "Please note: you are using one or more unsaved group(s) in your search. "+
							"<b>Unsaved groups will not be included in this link.</b> "+
							"In order to keep these groups included in your visualization, please log in and save your unsaved group(s).<br><br>":"";
		
		if( (noGenes || noExperiments) && warning !== ""){
			url = "<b>Nothing to link to.</b> See notes above.";	
		}else if(noGenes || noExperiments){
			url = "Error creating link";
		}
		
		Ext.Msg.alert("Bookmark or sharable link",warning+"Use this link to re-run your search:<br> "+url);
		return url;
	},
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
									right : Gemma.MetaVisualizationConfig.groupSeparatorWidth,//0,
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
									}]
						});
				Gemma.MetaHeatmapControlWindow.superclass.initComponent.apply(this, arguments);
			},
			onRender : function() {
				Gemma.MetaHeatmapControlWindow.superclass.onRender.apply(this, arguments);
			}
		});


Gemma.MetaHeatmapDataSelection = Ext.extend(Ext.Panel, {

	constructor : function(searchCommand) {
		if(typeof searchCommand !== 'undefined'){
			Ext.apply(this, {
			param : searchCommand
				// get input from front-page search
			});
		}
		
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
		
		/*var progressWindow = new Ext.Window({
					width : 80,
					height : 50,
					closable:false,
					title: "Estimated time: " + (estimatedTime / 1000) + "s",
					items : [{
								xtype : 'progress',
								ref : 'progress_bar'
							}]
				});
		progressWindow.show();*/
		if(typeof this.param ==='undefined'){ // if not loading text from search interface (ex: when using a bookmarked link)
			var waitMsg = Ext.Msg.wait("","Loading your visualization...");
		}
		
		/*progressWindow.progress_bar.wait({
					interval : 1000,
					duration : estimatedTime,
					increment : estimatedTime / 1000,
					text : 'Building visualization...',
					scope : this,
					fn : function() {
					}
				});*/
		this._selectedDatasetGroups = [];
		this._selectedGeneGroups = [];

		if (!this.taxonId || this.taxonId === null) {
			// DO SOMETHING!!
		}
		this.geneReferences = this.geneGroupReferences;
		this.datasetReferences = this.datasetGroupReferences;
		
		if(this.initExperimentGroupReferences){
			this.datasetReferences = this.datasetReferences.concat(this.initExperimentGroupReferences);
		}
		if(this.initExperimentReferences){
			this.datasetReferences= this.datasetReferences.concat(this.initExperimentReferences);
		}
		if(this.initGeneGroupReferences){
			this.geneReferences = this.geneReferences.concat(this.initGeneGroupReferences);
		}
		if(this.initGeneReferences){
			this.geneReferences = this.geneReferences.concat(this.initGeneReferences);
		}
		if(typeof this.initGeneSessionGroupQueries === 'undefined'){
			this.initGeneSessionGroupQueries = [];
		}
		if(typeof this.initExperimentSessionGroupQueries === 'undefined'){
			this.initExperimentSessionGroupQueries = [];
		}

		DifferentialExpressionSearchController.differentialExpressionAnalysisVisualizationSearch(this.taxonId,
				this.datasetReferences, this.geneReferences, this.initGeneSessionGroupQueries,
				this.initExperimentSessionGroupQueries, function(data) {

					//progressWindow.hide();
					if(waitMsg){
						waitMsg.hide();
					}
					
					// to trigger loadmask on search form to hide
					this.fireEvent('visualizationLoaded');
					
					data.geneGroupReferences = this.geneGroupReferences;
					data.datasetGroupReferences = this.datasetGroupReferences;
					data.taxonId = this.taxonId;
					
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
						var title = '<b>Differential Expression Visualisation</b>'+
											' (Data available for ' +
											experimentCount + ((typeof this.param !== 'undefined')? 
												(' of ' + this.param.datasetCount + ' experiments)'):"experiments)");
						_metaVizApp = new Gemma.MetaHeatmapApp({
									tbarTitle : title,
									visualizationData : data,
									initGeneSort: (this.initGeneSort)? this.initGeneSort:null,
									initExperimentSort: (this.initExperimentSort)? this.initExperimentSort:null,
									initFactorFilter: (this.initFactorFilter)? this.initFactorFilter:null,
									applyTo : 'meta-heatmap-div',
									pvalue : this.pvalue,
									geneSessionGroupQueries :this.geneSessionGroupQueries,
									experimentSessionGroupQueries :this.experimentSessionGroupQueries
								});
						_metaVizApp.doLayout();
						_metaVizApp.refreshVisualization();
					}

				}.createDelegate(this));
	},
	/**
	 * Restore state from the URL (e.g., bookmarkable link)
	 * 
	 * @param {}
	 *            query
	 * @return {}
	 */
	initializeSearchFromURL : function(url) {
		var param = Ext.urlDecode(url);
		var arrs; var i;
		
		if (param.p) {
			this.pvalue = param.p;
		}if (param.t) {
			this.taxonId = param.t;
		}
		if (param.gs) {
			this.initGeneSort = param.gs;
		}
		if (param.es) {
			this.initExperimentSort = param.es;
		}
		if (param.ff) {
			this.initFactorFilter = param.ff.split(',');
		}
		if (param.gq) {
			this.initGeneSessionGroupQueries = param.gq.split(',');
		}
		if (param.eq) {
			this.initExperimentSessionGroupQueries = param.gq.split(',');
		}
		if (param.g) {
			arrs = param.g.split(',');
			for(i = 0; i< arrs.length; i++){
				// make a reference object for each id
				arrs[i] = {
					id: arrs[i],
					type: 'databaseBackedGene'  // TODO get this from the reference java object!!!
				};
			}
			this.initGeneReferences = arrs;
		}
		if (param.e) {
			arrs = param.e.split(',');
			for(i = 0; i< arrs.length; i++){
				// make a reference object for each id
				arrs[i] = {
					id: arrs[i],
					type: 'databaseBackedExperiment'  // TODO get this from the reference java object!!!
				};
			}
			this.initExperimentReferences = arrs;
		}
		if (param.gg) {
			arrs = param.gg.split(',');var k = 0;
			for(i = 0; i< arrs.length; i++){
				// make a reference object for each id
				arrs[i] = {
					id: arrs[i],
					type: 'databaseBackedGroup' // TODO get this from the reference java object!!!
				};
			}
			this.initGeneGroupReferences = arrs;
		}
		if (param.eg) {
			arrs = param.eg.split(',');
			for(i = 0; i< arrs.length; i++){
				// make a reference object for each id
				arrs[i] = {
					id: arrs[i],
					type: 'databaseBackedGroup'  // TODO get this from the reference java object!!!
				};
			}
			this.initExperimentGroupReferences = arrs;
		}
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
		
		var queryStart = document.URL.indexOf("?");
		if (queryStart > -1) {
			this.initializeSearchFromURL(document.URL.substr(queryStart + 1));
		}

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
			if (this.param.geneSessionGroupQueries) {
				this.geneSessionGroupQueries = this.param.geneSessionGroupQueries;
			}
			if (this.param.experimentSessionGroupQueries) {
				this.experimentSessionGroupQueries = this.param.experimentSessionGroupQueries;
			}
			if (this.param.pvalue) {
				this.pvalue = this.param.pvalue;
			}
			if (this.param.selectionsModified){
				this.selectionsModified = this.param.selectionsModified;
			}
		}

		Gemma.MetaHeatmapDataSelection.superclass.initComponent.apply(this, arguments);

		this.doVisualization();
			
		if (this.param && this.param.geneReferences && this.param.geneNames && this.param.datasetReferences &&
			this.param.datasetNames && this.param.taxonId) {
		//	this.doVisualization();
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
