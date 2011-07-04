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
	prevVizWindowX: null,
	prevVizWindowY: null,
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
		// Display the tree.
		this.tree = new Gemma.FactorSelectTree(this.treeData);
		Ext.apply(this.tree, {
					autoScroll : true,
					bodyStyle : 'padding-bottom:5px'
				});
		this.tree.on('checkchange', function(node, checked) { // fired every
					// time a checkbox changes state
					var filteringFn = function(o) {
						var factorId = o.factorCategory ? o.factorCategory : o.factorName;
						if (factorId.toLowerCase() == node.text.toLowerCase()) {
							return !checked; // should return true if you
							// want to filter
						}
						return null; // don't do anything to a node that
						// wasn't checked
					};
					this.filterColumns (filteringFn);
					this._sortColumns (this.currentSortingDirection, this.currentSortingFn);
					this.doLayout();
					this.refreshVisualization();
				}, this);

		/************************* Selection grids ****************************************/
		
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
				geneSelectionData.push( [ this.visualizationData.geneIds[i][j], 
										  this.visualizationData.geneNames[i][j], 
										  this.visualizationData.geneFullNames[i][j] ] );
			}
		}
		geneSelectionStore.loadData( geneSelectionData );
		
		this.eeSelectionList = new Ext.grid.GridPanel({ // Tried listView but it was very very hard to select an entry by gene id.
			store : eeSelectionStore,
			multiSelect : true,
			hideHeaders : true,
			stripeRows : true,
			viewConfig : {
				forceFit : true
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
			_selectedExperiments : [] // array of ids of selected experiments
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
									this.drawHeatmapSubColumn_();									
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
									this.drawHeatmapSubColumn_();									
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
			hideHeaders: true,
			frame: false,
			allowSaveToSession: false
		});

		this.geneSelectionEditorWindow = new Ext.Window({
			closable : false,
			layout : 'fit',
			width : 450,
			height : 500,
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
			width : 450,
			height : 500,
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
		

		/************** visualizer sizing *****************/
		var pageHeight =  window.innerHeight !== null ? window.innerHeight : document.documentElement && 
			document.documentElement.clientHeight ? document.documentElement.clientHeight : document.body !== null ? document.body.clientHeight : null;
			
		var pageWidth = window.innerWidth !== null? window.innerWidth : document.documentElement && 
			document.documentElement.clientWidth ? document.documentElement.clientWidth : document.body !== null ? document.body.clientWidth : null;

		var adjPageWidth = ((pageWidth - Gemma.MetaVisualizationConfig.windowPadding) > Gemma.MetaVisualizationConfig.minAppWidth)? 
								(pageWidth - Gemma.MetaVisualizationConfig.windowPadding - 30) : Gemma.MetaVisualizationConfig.minAppWidth;
								// not sure why need extra -30 here and not below, but otherwise it doesn't fit 
		var adjPageHeight = ((pageHeight - Gemma.MetaVisualizationConfig.windowPadding) > Gemma.MetaVisualizationConfig.minAppHeight)? 
								(pageHeight - Gemma.MetaVisualizationConfig.windowPadding) : Gemma.MetaVisualizationConfig.minAppHeight;
		// resize all elements with browser window resize
		Ext.EventManager.onWindowResize(function(width, height){
			// -50 so that window fits nicely
			var adjWidth = ((width - Gemma.MetaVisualizationConfig.windowPadding) > Gemma.MetaVisualizationConfig.minAppWidth)? 
								(width - Gemma.MetaVisualizationConfig.windowPadding): Gemma.MetaVisualizationConfig.minAppWidth;
			var adjHeight = ((height - Gemma.MetaVisualizationConfig.windowPadding) > Gemma.MetaVisualizationConfig.minAppHeight)? 
								(height - Gemma.MetaVisualizationConfig.windowPadding): Gemma.MetaVisualizationConfig.minAppHeight;
			this.setSize(adjWidth, adjHeight);
			var adjHeightInner = adjHeight - 30; // for top-bar
			this._imageArea.setSize(adjWidth-Gemma.MetaVisualizationConfig.toolPanelWidth,adjHeightInner);
			this._toolPanels.setPosition(adjWidth-Gemma.MetaVisualizationConfig.toolPanelWidth,0);
			this._toolPanels.setSize(Gemma.MetaVisualizationConfig.toolPanelWidth,adjHeightInner);
			this._toolPanels.doLayout();
			
			this._imageArea.colorLegend.setPosition(adjWidth-Gemma.MetaVisualizationConfig.toolPanelWidth-215, 0);
			
			this.doLayout();
		}, this);
		
		/*********** END: visualizer sizing *****************/

		Ext.apply(this, {
			width : adjPageWidth,
			height : adjPageHeight,
			//layout : 'absolute',
			layout:'border',
			_visualizationData : this.visualizationData,
			geneScores : this.visualizationData.geneScores,
			geneOrdering : null,
			
			visibleMissingValuesGeneScore : null,

			// TOTAL_NUMBER_OF_COLUMNS: null,
			TOTAL_NUMBER_OF_ROWS : null,
			//TOTAL_NUMBER_OF_HIDDEN_COLUMNS : 0,

			filterColumns : function(filteringFn) {
				return this._imageArea._heatmapArea.filterColumns(filteringFn);
			},
			currentSortingFn_ : null,			
			_sortColumns : function(asc_desc, sortingFn) {
				this._imageArea._heatmapArea._sortColumns(asc_desc, sortingFn);
			},
			tbar : [this.tbarTitle, 
							{
								hidden: true,
								xtype: 'button',
								text: 'change',
								scope:this,
								handler: function(){
									var win = new Gemma.DifferentialExpressionSearchOptions({
											//threshold: this.pvalue //we don't use this yet
										}).show();
									win.on('rerunSearch',function(threshold){
										//this.pvalue = threshold; //we don't use this yet
										if (this.loadedFromURL) {
											var url = this.getBookmarkableLink();
											if (url !== null) {
												window.location = this.getBookmarkableLink();
											}
											else {
												this.getBookmarkableLinkMsg();
											}
										}
										else {
											
										}
									},this);
								}
							},'->', {
				xtype : 'button',
				x : 5,
				y : 5,
				text : '<b>Color Legend</b>',
				tooltip:'Show/hide the color legend',
				handler : function() {
					if (this._imageArea.colorLegend.hidden){
						this._imageArea.colorLegend.y=0;
						this._imageArea.colorLegend.x=adjPageWidth-Gemma.MetaVisualizationConfig.toolPanelWidth-215;
						this._imageArea.colorLegend.show();
					}else{
						this._imageArea.colorLegend.hide();
					}
				},
				scope:this
			},'-', {
				xtype : 'button',
				x : 5,
				y : 5,
				text : '<b>Bookmarkable Link</b>',
				tooltip:'Get a link to re-run this search',
				handler : function() {
					this.getBookmarkableLinkMsg();
				},
				scope:this
			},'-',{
				xtype : 'button',
				x : 5,
				y : 5,
				text : '<b>Download</b>',
				icon : '/Gemma/images/download.gif',
				cls : 'x-btn-text-icon',
				tooltip:'Download a formatted text version of your search results',
				listeners: {
					click: function(){
						var textWindow = new Gemma.MetaHeatmapDownloadWindow();
						textWindow.show();
						textWindow.loadData(this._imageArea._heatmapArea);
					},
					scope: this
				}
			},'-',{
				xtype : 'button',
				x : 5,
				y : 5,
				icon : '/Gemma/images/icons/question_blue.png',
				cls : 'x-btn-icon',
				tooltip:'Click here for documentation on how to use this visualizer.',
				listeners: {
					click: function(){
						window.open('http://www.chibi.ubc.ca/faculty/pavlidis/wiki/download/attachments/26706691/differentialExpressionVisualizationImageMap-howTo3.png'); 
					},
					scope: this
				}
			}
			],
			items : [{
				ref: '_toolPanels',
				region:'east',
				split:true,
				//collapsible:true,
				collapseMode:'mini',
				useSplitTips : true,
				height: adjPageHeight - 30,
				width: Gemma.MetaVisualizationConfig.toolPanelWidth,
				x: adjPageWidth - Gemma.MetaVisualizationConfig.toolPanelWidth,
				y: 0,
				border: true,
				applicationRoot: this,
				parentShrunk: false,
				//layout: 'vbox',
				layout:'ux.accordionvbox',
				defaults: {
					collapsible: true,
					flex: 3 // The sizes of panels are divided according to the flex index
				},
			    layoutConfig: {
					align: 'stretch',
					pack: 'start',
					animate: false,
					titleCollapse: false
				},
				items: [{
					title: 'Sort',
					ref:'_sortPanel',
					flex: 1, // because more space for this panel doesn't help
					//height:100,
					width: Gemma.MetaVisualizationConfig.toolPanelWidth,
					collapsible: true,
					// collapsed:true,
					border: true,
					bodyBorder: true,
					layout: 'form',
					labelWidth: 115,
					bodyStyle: 'padding:5px',
					defaults: {
						hideLabel: false
					},
					items: [{
						xtype: 'combo',
						ref:'_experimentSort',
						hiddenName: 'conditionSort',
						fieldLabel: 'Sort experiments by',
						fieldTipTitle: 'Sort Experiments By:',
						fieldTipHTML:'<br><b>Full Name</b>: official descriptive title<br><br>'+
							'<b>Short Name</b>: short name or ID (ex: GSE1234)<br><br>'+
							'<b>q Value</b>: confidence that the selected genes are differentially expressed<br><br>'+
							'<b>Diff. Exp. Specificity</b>: within each column, this is the proportion of probes that are differentially expressed '+
							'versus the total number of expressed probes. This measure is represented by each column\'s pie chart. Experiments are ordered based '+
							'on their columns\' average specificity.<br><br>',
						fieldTip:'Name: official descriptive title.  q Value: confidence in the expression levels of the selected genes.  '+
							'Diff. Exp. Specificity: the proportion of probes that are differentially expressed '+
							'across each experimental factor '+
							'versus the total number of expressed probes. This measure is represented by each column\'s pie chart. Experiments are ordered based '+
							'on their column\'s average specificity.',
						mode: 'local',
						displayField: 'text',
						valueField: 'name',
						width: 150,
						editable: 'false',
						forceSelection: 'true',
						triggerAction: 'all',
						store: new Ext.data.ArrayStore({
							fields: ['name', 'text'],
							data: [['experimentName', ' -- '],['shortName', 'short name'], ['qValues', 'q values'], ['specificity', 'diff. exp. specificity']],
							idIndex:0
						}),
						listeners: {
							select: function(field, record, selIndex){
						
								if (record.get('name') === 'qValues') {
									this.currentSortingFn = function(o1, o2){									
										return o1.overallDifferentialExpressionScore - o2.overallDifferentialExpressionScore;
									};
									this.currentSortingDirection = 'DESC';
									this._sortColumns('DESC', this.currentSortingFn);
									this.doLayout();
								}
								else if (record.get('name') === 'specificity') {
									this.currentSortingFn = function(o1, o2){										
										return o1.specificityScore - o2.specificityScore;
									};
									this.currentSortingDirection = 'ASC';
									this._sortColumns('ASC', this.currentSortingFn);									
									this.doLayout();
								}
								else if (record.get('name') === 'experimentName') {
									this.currentSortingFn = function(o1, o2){										
										return (o1.datasetName >= o2.datasetName) ? 1 : -1;
									};
									this.currentSortingDirection = 'ASC';
									this._sortColumns('ASC', this.currentSortingFn);										
									this.doLayout();
								}
								else if (record.get('name') === 'shortName') {
									this.currentSortingFn = function(o1, o2){										
										return (o1.datasetShortName >= o2.datasetShortName) ? 1 : -1;
									}
									this.currentSortingDirection = 'ASC';
									this._sortColumns('ASC', this.currentSortingFn); 																				
									this.doLayout();
								}
								this.refreshVisualization();
							},
							render: function(combo){
								combo.setValue('--');
							},
							scope: this
						}
					}, {
						xtype: 'combo',
						hiddenName: 'geneSort',
						ref:'_geneSort',
						fieldLabel: 'Sort genes by',
						fieldTipTitle: 'Sort Genes By:',
						fieldTipHTML:'<b>Symbol</b>: official gene symbol<br>'+
										'<b>q Values</b>: confidence that the gene is differentially expressed, averaged across the queried experiments<br><br>',
						fieldTip:'Symbol: official gene symbol.  '+
										'q Values: confidence that the gene is differentially expressed, averaged across the queried experiments',
						mode: 'local',
						displayField: 'text',
						valueField: 'name',
						width: 150,
						editable: 'false',
						forceSelection: 'true',
						triggerAction: 'all',
						store: new Ext.data.ArrayStore({
							fields: ['name', 'text'],
							data: [['symbol', 'symbol'], ['score', 'q values']],
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
										var sortByScoreFn = function(o1, o2){
											return o2.score - o1.score;
										}; 
										for (i = 0; i < this.geneScores[0].length; i++) {
											this.geneScores[0][i].sort(sortByScoreFn);
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
					ref:'_filterPanel',
					collapsible: true,
					collapsed: false,
					border: true,
					bodyBorder: true,
					autoScroll: true,
					layout: 'form', // to get font sizes matching
					items: [this.tree]
				}, {
						ref: '_selectionTabPanel',
						width: Gemma.MetaVisualizationConfig.toolPanelWidth,
						xtype: 'tabpanel',
						defaults: {
							layout: 'fit'
						},
						layoutConfig: {
							monitorResize: true
						},
						activeTab: 0,
						deferredRender: false,
						items: [{
							title: 'Select Experiments',
							layout: 'fit',
							autoScroll: true,
							items: this.eeSelectionList,
							bbar: ['Hold \'ctrl\' to select > 1', '->', {
								text: 'Save Selection',
								icon: '/Gemma/images/icons/disk.png',
								handler: this.launchExperimentSelectionEditor,
								scope: this,
								tooltip: 'Create a group of experiments from your selection'
							}, '-', {
								text: 'Clear',
								handler: function(button, clickEvent){
									this.eeSelectionList.getSelectionModel().clearSelections();
									this.fireEvent('clearedExperimentSelections');
								},
								scope: this
							}]
						}, {
							title: 'Select Genes',
							autoScroll: true,
							layout: 'fit',
							items: this.geneSelectionList,
							bbar: ['Hold \'ctrl\' to select > 1', '->', {
								text: 'Save Selection',
								icon: '/Gemma/images/icons/disk.png',
								handler: this.launchGeneSelectionEditor,
								scope: this,
								tooltip: 'Create a group of genes from your selection'
							}, '-', {
								text: 'Clear',
								handler: function(button, clickEvent){
									this.geneSelectionList.getSelectionModel().clearSelections();
									this.fireEvent('clearedGeneSelections');
								},
								scope: this
							}]
						}]
					
				}
				
				
				
				]
			},{
				ref : '_imageArea',
				region:'center',
				autoScroll : true,
				layout : 'absolute',
				frame : false,
				border : false,
				width : adjPageWidth - Gemma.MetaVisualizationConfig.toolPanelWidth,
				height : adjPageHeight - 30,
				items : [
				
				new Gemma.ColorLegend({
					ref: "colorLegend",
					discreteColorRangeObject: Gemma.MetaVisualizationConfig.basicColourRange,
					discreteColorRangeObject2: Gemma.MetaVisualizationConfig.contrastsColourRange,
					cellHeight: 14,
					cellWidth: 14,
					colorValues: [[null, "No Data"], [0.1, "0.5~0.25"], [0.2, "0.1"], [0.3, "0.05"], [0.4, "0.01"], [0.6, "0.001"], [0.8, "0.0001"], [0.9, "0.00001"], [1, "< 0.00001"]],
					colorValues2: [[null, "No Data"], [3, "Up"], [-3, "Down"]],
					vertical: true,
					canvasId: 'canvas1',
					canvasId2: 'canvas12',
					legendTitle: 'q-value',
					legendTitle2: 'direction',
					textWidthMax: 80,
					textOffset: 1,
					fontSize: 12,
					x: adjPageWidth - Gemma.MetaVisualizationConfig.toolPanelWidth - 215,
					y: 0,
					constrain: true
				
				
				}),{
				// a window for displaying details as elements of the image are
				// hovered over
				//title : 'Details <span style="color:grey">(Drag me!)</span>',
				ref : '_hoverDetailsPanel',
				xtype : 'window',
				//height : 200,
				width : 350,
				autoScroll : true,
				closable : false,
				shadow : false,
				border : false,
				bodyBorder : false,
				hidden: true,
				bodyStyle : 'padding: 7px',
				html : '<span style="color:black;font-size:1.3em">Hover over the visualisation for quick details or click for more information.'+
						' <br><br>Hold down "ctrl" and click on a gene to select it.</span>',
				tpl : new Ext.XTemplate(
						'<span style="font-size: 12px ">',
						'<tpl for=".">',
						'<tpl if="type==\'experiment\'">',
						'<b>Experiment</b>: {datasetShortName} {datasetName}<br><br>',
						'<b>Factor</b>: {factorCategory} - {factorDescription}<br> <br>',
						'<b>Factor Values</b>: {factorValues}<br><br> ',
						'<b>Baseline</b>: {baseline}<br><br>',
						'</tpl>',
						'<tpl if="type==\'minipie\'">',
						'{percentProbesDiffExpressed} of probes are differentially expressed.<br><br>',
						'({numberOfProbesDiffExpressed} of {numberOfProbesTotal}) Click for details.',
						'</tpl>',
						'<tpl if="type==\'gene\'">',
						'<b>Gene</b>: {geneSymbol} {geneFullName}<br><br> ',
						'</tpl>',
						'<tpl if="type==\'contrastCell\'">',
						'<b>Gene</b>: {geneSymbol} {geneFullName}<br><br> ',
						'<b>Experiment</b>: {datasetShortName}</a> {datasetName}<br><br>',
						'<b>Factor</b>:{factorCategory} - {factorDescription}<br><br> ', '<b>Log2 fold change</b>: {foldChange}<br>', '<b>pValue</b>: {contrastPvalue} <br>',
						'</tpl>',
						'<tpl if="type==\'cell\'">',
						'<b>Gene</b>: {geneSymbol} {geneFullName}<br><br> ',
						'<b>Experiment</b>: {datasetShortName} {datasetName}<br><br>',
						'<b>Factor</b>:{factorCategory} - {factorDescription}<br><br> ', '<b>q Value</b>: {qvalue}',
						'</tpl>', '</tpl></span>'),
				tplWriteMode : 'overwrite'
			}, {
							x:5,
							y:5,
							xtype:'panel',
							html:'<span style="color:dimGrey;font-size:0.9em;line-height:1.6em"><b>Hover</b> for quick info<br>'+
							'<b>Click</b> for details<br><b>"ctrl" + click</b> to select genes</span>',
							border:false,
							width: 180,
							height: 100
						}, {
							xtype : 'metaVizGeneLabels',
							height : Gemma.MetaVisualizationUtils
									.calculateGeneLabelColumnHeight(this.visualizationData.geneNames),
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
							height : 260,
							x : 80,
							y : 0,
							applicationRoot : this,
							visualizationData : this.visualizationData.resultSetValueObjects,
							datasetGroupNames : this.visualizationData.datasetGroupNames,
							ref : 'topLabelsPanel'
						},{
							xtype : 'metaVizScrollableArea',
							height : Gemma.MetaVisualizationUtils.calculateGeneLabelColumnHeight( this.visualizationData.geneNames ),
							x : 80,
							y : Gemma.MetaVisualizationConfig.columnLabelHeight+2,
							dataDatasetGroups : this.visualizationData.resultSetValueObjects,
							geneNames : this.visualizationData.geneNames,
							geneIds : this.visualizationData.geneIds,
							applicationRoot : this,
							ref : '_heatmapArea'							
						} ]
			}]
		});
		Gemma.MetaHeatmapApp.superclass.initComponent.apply(this, arguments);

		this._imageArea._hoverDetailsPanel.hide();
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
	},
	onRender : function() {
		Gemma.MetaHeatmapApp.superclass.onRender.apply(this, arguments);

		if(this.initGeneSort){
			this._toolPanels._sortPanel._geneSort.setValue(this.initGeneSort);
		}
		if(this.initExperimentSort){
			this._toolPanels._sortPanel._experimentSort.setValue(this.initExperimentSort);
		}
		
	},
	afterRender : function() {
		Gemma.MetaHeatmapApp.superclass.afterRender.apply(this, arguments);
		
		// restore sorting from URL
		var rec; var index;
		if(this.initGeneSort){
			this._toolPanels._sortPanel._geneSort.setValue(this.initGeneSort);
			rec = this._toolPanels._sortPanel._geneSort.getStore().getById(this.initGeneSort);
			index = this._toolPanels._sortPanel._geneSort.getStore().indexOfId(this.initGeneSort);
			if (index !== -1) {
				this._toolPanels._sortPanel._geneSort.fireEvent('select', this._toolPanels._sortPanel._geneSort, rec, index);
			}
		}
		if(this.initExperimentSort){
			this._toolPanels._sortPanel._experimentSort.setValue(this.initExperimentSort);
			rec = this._toolPanels._sortPanel._experimentSort.getStore().getById(this.initExperimentSort);
			index = this._toolPanels._sortPanel._experimentSort.getStore().indexOfId(this.initExperimentSort);
			if (index !== -1) {
				this._toolPanels._sortPanel._experimentSort.fireEvent('select', this._toolPanels._sortPanel._experimentSort, rec, index);
			}
			
		}
		
		// restore filtering from URL
		if(this.initFactorFilter){
			var j = 0;
			for(j = 0; j < this.initFactorFilter.length; j++){
				this.tree.getNodeById(this.initFactorFilter[j]).getUI().toggleCheck(false);
			}
		}

		this._imageArea.colorLegend.show();

	},
	refreshVisualization : function() {
		this._imageArea.topLabelsPanel.refresh();
		this._imageArea._heatmapArea.refresh();
		this._imageArea._geneLabels.refresh();
	},
	getVizState: function(){
		var state = {};
		// Get gene group ids.
		// If there are any session-bound groups, get query that made them.
		state.geneGroupIds = [];
		state.geneIds = [];
		var i; var ref; var k = 0;
		for (i = 0; i < this._visualizationData.geneGroupReferences.length;i++) {
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
		
		// Get experiment group ids.
		// If there are any session-bound groups, get queries that made them.
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
		
		// Gene sort state.
		state.geneSort = this._toolPanels._sortPanel._geneSort.getValue();
		
		// Experiment sort state.
		state.eeSort = this._toolPanels._sortPanel._experimentSort.getValue();
		if(state.eeSort === '--'){// TODO make this less fragile
			state.eeSort = null;
		}
		
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
	 * @return url string or null if error or nothing to link to
	 */
	getBookmarkableLink : function() {
		var state = this.getVizState();
		if (!state) {
			return null;
		}
		var queryStart = document.URL.indexOf("?");
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		url = url.replace('home','metaheatmap');
		url = url.replace('html#','html'); // added by IE sometimes
		
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
				!(state.experimentSessionGroupQueries.length === 1 && typeof state.experimentSessionGroupQueries[0] === 'undefined') && 
				state.experimentSessionGroupQueries !== null && 
				state.experimentSessionGroupQueries.length !== 0) {
			url += String.format("eq={0}&", state.experimentSessionGroupQueries.join(","));
			noExperiments = false;
		}
		if (typeof state.geneSessionGroupQueries !== 'undefined' && 
				!(state.geneSessionGroupQueries.length === 1 && typeof state.geneSessionGroupQueries[0] === 'undefined') && 
				state.geneSessionGroupQueries !== null && 
				state.geneSessionGroupQueries.length > 0) {
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

		// remove trailing '&'
		url = url.substring(0, url.length-1);

		if(noGenes || noExperiments){
			return null;
		}
		return url;
	},		
	getBookmarkableLinkMsg : function() {
		
		url = this.getBookmarkableLink();

		
		var warning = (this.selectionsModified)? "Please note: you have unsaved modifications in one or more of your"+
							" experiment and/or gene groups. <b>These changes will not be saved in this link.</b>"+
							" In order to keep your modifications, please log in and save your unsaved groups.<br><br>":"";
									
		/*warning += (this.usingSessionGroup && )? "Please note: you are using one or more unsaved group(s) in your search. "+
							"<b>Unsaved groups will not be included in this link.</b> "+
							"In order to keep these groups included in your visualization, please log in and save your unsaved group(s).<br><br>":"";
		*/
		if(url === null && warning === ""){
			url= "Error creating your link.";
		}
		new Ext.Window({
			closeAction:'close',
			title:"Bookmark or sharable link",
			html:'<b>Use this link to re-run your search:</b><br> <a target="_blank" href="'+url+'">'+url+'<\a>',
			width:650,
			padding:10
		}).show();
	},
	/**
	 * 
	 */
	getDownloadLink : function() {
		// TODO: Refactor!
		// That was a quick way to reuse bookmarkable link logic. We should take shared code out into a separate function.
		var url = this.getBookmarkableLink();
		if (url != null) {
			url = url.replace('metaheatmap.html','downloadText/downloadMetaheatmapData.html');
		}
		return url;
	}			
	
});



