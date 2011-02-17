Ext.namespace('Gemma');

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
			},
			
		});
		
		Gemma.MetaHeatmapScrollableArea.superclass.initComponent.apply(this, arguments);		
	},
	
	onRender: function() {				
		Gemma.MetaHeatmapScrollableArea.superclass.onRender.apply(this, arguments);
		for ( var i = 0; i < this.dataDatasetGroups.length; i++ ) {			
			this.add(new Gemma.MetaHeatmapDatasetGroupPanel(
								{ applicationRoot: this.applicationRoot,
								  height: this.height,
								  dataFactorColumns : this.dataDatasetGroups[i],
								  datasetGroupIndex: i,
								  geneNames: this.geneNames[i],
								  geneIds: this.geneIds[i]}));
		}
		
	}
	
});
Ext.reg('metaVizScrollableArea', Gemma.MetaHeatmapScrollableArea);

//ANALYSIS LABELS
Gemma.MetaHeatmapRotatedLabels = Ext.extend(Ext.BoxComponent, {	
	initComponent: function() {
		Ext.apply(this, {
			autoEl: { tag: 'canvas',
			  		  width: 1200,
			  		  height: 260 ,
			},			
			_data: this.visualizationData,			
			_datasetGroupNames: this.datasetGroupNames,
			_angle: 310.0,
			_fontSize: 7,
			_fontColor: 'black',
			_heatmapContainer: null,
			_setHeatmapContainer: function(c) {
				this._heatmapContainer = c;
				
			},
									
			_drawTopLabels : function(hiDatasetGroup, hiColumnGroup, hiColumn, hiFactorValue) {			
				var ctx = this.el.dom.getContext("2d");
				CanvasTextFunctions.enable(ctx);
				ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
				
				var xPosition = Gemma.MetaVisualizationConfig.cellWidth;
				
				for ( var currentDatasetGroupIndex = 0; currentDatasetGroupIndex < this._heatmapContainer.items.getCount(); currentDatasetGroupIndex++ ) {
					var dsPanel = this._heatmapContainer.items.get(currentDatasetGroupIndex);
					ctx.drawText('', this._fontSize, xPosition, 10, this._datasetGroupNames[currentDatasetGroupIndex]);

					var alternateColors = 0;					
					for ( var currentDatasetColumnGroupIndex = 0; currentDatasetColumnGroupIndex < dsPanel.items.getCount(); currentDatasetColumnGroupIndex++ ) {
						var datasetColumnGroupPanel = dsPanel.items.get( currentDatasetColumnGroupIndex ); 						
						
						if (datasetColumnGroupPanel._hidden == false) {
							if (alternateColors == 1 ) {
								MiniPieLib.drawFilledRectangle( ctx, xPosition-Gemma.MetaVisualizationConfig.cellWidth, 10,
																datasetColumnGroupPanel.getWidth(), 238, 'rgba(10,100,10, 0.1)');		                     
		                        alternateColors = 0;
		                    } else {
		                    	MiniPieLib.drawFilledRectangle( ctx, xPosition-Gemma.MetaVisualizationConfig.cellWidth, 10,
		                    									datasetColumnGroupPanel.getWidth(), 238, 'rgba(10,100,10, 0.05)');
		                        alternateColors = 1;                        
		                    }            							
							if ( hiDatasetGroup == currentDatasetGroupIndex 
									&& hiColumnGroup == datasetColumnGroupPanel._columnGroupIndex ) {
								ctx.drawRotatedText( xPosition, 149, 270.0, this._fontSize,
													 Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
													 datasetColumnGroupPanel.datasetName);
							} else {
								ctx.drawRotatedText( xPosition, 149, 270.0, this._fontSize,
													 this._fontColor,
													 datasetColumnGroupPanel.datasetName);
							}
						}
						
						var alternateColorsAnalysis = 0;
						for ( var currentAnalysisColumnGroupIndex = 0; currentAnalysisColumnGroupIndex < datasetColumnGroupPanel.items.getCount(); currentAnalysisColumnGroupIndex++ ) {
							var analysisColumnGroupPanel = datasetColumnGroupPanel.items.get( currentAnalysisColumnGroupIndex ); 						

							if (alternateColorsAnalysis == 1 ) {
								MiniPieLib.drawFilledRectangle( ctx, xPosition-Gemma.MetaVisualizationConfig.cellWidth, 150,
																analysisColumnGroupPanel.getWidth(), 15, 'rgba(10,100,10, 0.6)');		                     
								alternateColorsAnalysis = 0;
		                    } else {
		                    	MiniPieLib.drawFilledRectangle( ctx, xPosition-Gemma.MetaVisualizationConfig.cellWidth, 150,
		                    									analysisColumnGroupPanel.getWidth(), 15, 'rgba(10,100,10, 0.3)');
		                    	alternateColorsAnalysis = 1;                        
		                    }            							
							
							for (var currentColumn = 0; currentColumn < analysisColumnGroupPanel.items.getCount(); currentColumn++) {
								var dColumn = analysisColumnGroupPanel.items.get( currentColumn );
								if ( dColumn._expandButton.pressed ) {
									for (var i = 0; i < dColumn._factorValueNames.length; i++) {
										if (i == 0) MiniPieLib.drawMiniPie( ctx, xPosition-4, 255, 9,
																			Gemma.MetaVisualizationConfig.miniPieColor,
																			dColumn.miniPieValue );
										if ( hiDatasetGroup == currentDatasetGroupIndex 
											 && hiColumnGroup == datasetColumnGroupPanel._columnGroupIndex											 
											 && i == hiFactorValue 
											 && currentColumn == hiColumn )
										{
											ctx.drawRotatedText( xPosition, 248, this._angle, this._fontSize,
																 Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
																 dColumn._factorValueNames[i]);																			
										} else if ( dColumn._factorValueNames[i] == dColumn._baselineFactorValue ) {
											ctx.drawRotatedText( xPosition, 248, this._angle, this._fontSize,
																 'rgb(128, 0, 0)',
																 dColumn._factorValueNames[i] );									
										} else {
											ctx.drawRotatedText( xPosition, 248, this._angle, this._fontSize,
																 'rgb(46,139,87)',
																 dColumn._factorValueNames[i]);
										}
										xPosition += Gemma.MetaVisualizationConfig.cellWidth;
									}
									xPosition += Gemma.MetaVisualizationConfig.columnSeparatorWidth;
								}
								else {
									if (! dColumn.hidden) {
										MiniPieLib.drawMiniPie( ctx, xPosition-4, 255, 9,
																Gemma.MetaVisualizationConfig.miniPieColor,
																dColumn.miniPieValue);
										if (hiDatasetGroup == currentDatasetGroupIndex 
												&& hiColumnGroup == columnGroup._columnGroupIndex
												&& hiColumn == currentColumn )
										{
											ctx.drawRotatedText( xPosition, 248, this._angle, this._fontSize,
																 Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
																 dColumn._factorName );										
										} else {									
											ctx.drawRotatedText( xPosition, 248, this._angle, this._fontSize,
																 this._fontColor,
																 dColumn._factorName );
										}
										xPosition += Gemma.MetaVisualizationConfig.cellWidth;
										xPosition += Gemma.MetaVisualizationConfig.columnSeparatorWidth;
									}
								}												
							}
							
						}	
					}
					xPosition += Gemma.MetaVisualizationConfig.groupSeparatorWidth;
				}
			},
			
		});
		
		//this.syncSize();//setWidth(900);	
		Gemma.MetaHeatmapRotatedLabels.superclass.initComponent.apply(this, arguments);		
	},
	
	onRender: function() {		
		Gemma.MetaHeatmapRotatedLabels.superclass.onRender.apply(this, arguments);
		this.syncSize();
		
		this.el.on('click', function(e,t) {
			

			var popup = Gemma.MetaVisualizationPopups.makeDatasetInfoWindow(datasetName, datasetId);
			popup.show();
		}, this);		

		
	}
				
});

Ext.reg('metaVizRotatedLabels', Gemma.MetaHeatmapRotatedLabels);


// Gene group
Gemma.MetaHeatmapLabelGroup = Ext.extend(Ext.BoxComponent, {	
	initComponent: function() {
		Ext.apply(this, {
			applicationRoot: this.applicationRoot,
			geneNames: this.labels,
			autoEl: { tag: 'canvas',
			  		  width: 80,
			  		  height: this.labels.length*10,
			},
			
			geneGroupName: this.geneGroupName,
			geneGroupId: this.geneGroupId,
			
			_drawRotatedText : function(ctx, x, y, ang, fontSize, fontColor, text) {				
			    if (!fontColor) {
			        fontColor = 'black';
			    }
			    ctx.save();
			    // relocate to draw spot
			    ctx.translate(x, y);
			    ctx.rotate(ang * 2 * Math.PI / 360); // rotate to vertical
			    ctx.strokeStyle = fontColor;
			    ctx.drawText('', fontSize, 0, 0, text);
			    ctx.restore();
			},
			getIndexFromY : function (y) {
		    	return Math.floor(y/Gemma.MetaVisualizationConfig.cellHeight);
			},
			_drawLabels : function (highlightRow) {
				var ctx = this.el.dom.getContext("2d");
				ctx.clearRect(0, 0, this.el.dom.width, this.el.dom.height);		
				CanvasTextFunctions.enable(ctx);
				ctx.drawRotatedText(10, this.getHeight() - 10, 270.0, 9, 'black', this.geneGroupName);
				
				// Some genes can be hidden. Genes can be sorted in different ways.
				// Gene ordering is mapping that capture order and number of shown genes.
				for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupId].length; i++) {
					var geneName = this.geneNames[this.applicationRoot.geneOrdering[this.geneGroupId][i]];
					if (highlightRow == i) {
						ctx.save();
						ctx.strokeStyle = Gemma.MetaVisualizationConfig.geneLabelHighlightColor;
						ctx.drawTextRight( '', Gemma.MetaVisualizationConfig.geneLabelFontSize, 77,
										   (i+1)*Gemma.MetaVisualizationConfig.cellHeight,
										   geneName);
						ctx.restore();
					} else {
						ctx.drawTextRight(  '', Gemma.MetaVisualizationConfig.geneLabelFontSize, 77,
											(i+1)*Gemma.MetaVisualizationConfig.cellHeight,
											geneName);						
					}					
				}									
			},
		});
		Gemma.MetaHeatmapLabelGroup.superclass.initComponent.apply ( this, arguments );		
		
},
	
	onRender: function() {
		Gemma.MetaHeatmapLabelGroup.superclass.onRender.apply ( this, arguments );
		this._drawLabels();
		
		this.el.on('mousemove', function(e,t) { 						
			var index = this.getIndexFromY(e.getPageY() - Ext.get(t).getY());
			this._drawLabels(index);
		}, this );		
		
		this.el.on('click', function(e,t) {
			var index = this.getIndexFromY(e.getPageY() - Ext.get(t).getY());
			var geneId = this.applicationRoot.geneOrdering[this.geneGroupId][index];
			var geneName = this.geneNames[geneId];
			var popup = Gemma.MetaVisualizationPopups.makeGeneInfoWindow(geneName, geneId);
			popup.show();
		}, this);		
	}
});


// Gene Labels
Gemma.MetaHeatmapLabelsColumn = Ext.extend(Ext.Panel, {	
	initComponent: function() {
		Ext.apply(this, {
			applicationRoot: this.applicationRoot,
 			layout: 'vbox',
 			layoutConfig: {
				defaultMargins: {top:0, right:0, bottom:4, left:0}
			},
			labels: this.labels,
			geneGroupNames: this.geneGroupNames,
			highlightGene: function (geneGroup, row) {
				this.items.get(geneGroup)._drawLabels(row);
			},			
		});
						
		Gemma.MetaHeatmapLabelsColumn.superclass.initComponent.apply(this, arguments);		
	},
	
	onRender: function() {		
		Gemma.MetaHeatmapLabelsColumn.superclass.onRender.apply(this, arguments);
		
		for (var groupIndex = 0; groupIndex < this.labels.length; groupIndex++) {
			this.add( new Gemma.MetaHeatmapLabelGroup(
								{ applicationRoot: this.applicationRoot,
								  labels: this.labels[groupIndex],
								  geneGroupName: this.geneGroupNames[groupIndex],
								  geneGroupId: groupIndex }) );
		}
	}
				
});

Ext.reg('metaVizGeneLabels', Gemma.MetaHeatmapLabelsColumn);

Ext.reg('taxonCombo', Gemma.TaxonCombo);

Gemma.MetaHeatmapControlWindow = Ext.extend(Ext.Window, {	
	initComponent: function() {
		Ext.apply(this, {
			title: 'Visualization settings',
			layout: 'accordion',
			layoutConfig: {
		        titleCollapse: false,
		        animate: true,
		        activeOnTop: true
		    },
		    items: [{title: 'Data selection', xtype: 'metaVizDataSelection', ref:'selectionPanel'},
		            {title: 'Filter/Sort', xtype: 'metaVizSortFilter', ref:'sortPanel'},],
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
	initComponent: function() {
		Ext.apply(this, {
			_selectedGeneGroups: [],
			_selectedDatasetGroups: [],
			_metaVizApp: null,
			_sortPanel: null,
			height: 450,
			layout: 'fit',
			items: [
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
				  				 var genePicker = new Gemma.GeneGroupCombo({width : 200});
				  				 genePicker.setTaxon(this._taxonCombo.getTaxon());					
				  				 this._selectedGeneGroups.push(genePicker);
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
				  			var datasetPicker = new Gemma.DatasetGroupCombo({width : 200});
				  			this._selectedDatasetGroups.push(datasetPicker);
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
					click: function(target) {
		  				var geneGroups =[];
		  				var geneGroupNames=[];
		  				for (var i = 0; i < this._selectedGeneGroups.length; i++) {
		  					geneGroups.push(this._selectedGeneGroups[i].getGeneGroup().id);
		  					geneGroupNames.push(this._selectedGeneGroups[i].getGeneGroup().name);
		  				}
		    	
		  				var datasetGroups = [];
		  				var datasetGroupNames = [];
		  				for (var i = 0; i < this._selectedDatasetGroups.length; i++) {
		  					datasetGroups.push(this._selectedDatasetGroups[i].getSelected().id);
		  					datasetGroupNames.push(this._selectedDatasetGroups[i].getSelected().get("name"))
		  				}		  			
		  				DifferentialExpressionSearchController.getVisualizationTestData(this._taxonCombo.getSelected().id, datasetGroups, geneGroups, function(data) {
		  					data.geneGroupNames = geneGroupNames;
		  					data.datasetGroupNames = datasetGroupNames;		  					
		  							  							  					
		  					_metaVizApp = new Gemma.MetaHeatmapApp({visualizationData: data, applyTo:'meta-heatmap-div'});
		  					_metaVizApp.doLayout();		  						  				
		  					
		  					_sortPanel.add({xtype:'checkbox',
		  									boxLabel:'Sort by gene score.',
		  									listeners: { 
		  										check : function(target, checked) {
													if (checked) {
														//Sort genes : changes gene order
														for (var i = 0; i < this.geneScores[0].length; i++) {
															this.geneScores[0][i].sort( function ( o1, o2 ) { return o1.score - o2.score; } );																	
														}
														for (var geneGroupIndex = 0; geneGroupIndex < this._heatmapArea.geneNames.length; geneGroupIndex++) {			
															this.geneOrdering[geneGroupIndex] = [];
															for (var i = 0; i < this._heatmapArea.geneIds[geneGroupIndex].length; i++) {
																if ( this.geneScores[0][geneGroupIndex][i].score != 0 ) {
																	this.geneOrdering[geneGroupIndex].push( this.geneScores[0][geneGroupIndex][i].index );
																}
															}
														}														
													} else {														
														//Default geneOrdering
														for (var geneGroupIndex = 0; geneGroupIndex < this._heatmapArea.geneNames.length; geneGroupIndex++) {			
															this.geneOrdering[geneGroupIndex] = [];														
															for (var i = 0; i < this._heatmapArea.geneIds[geneGroupIndex].length; i++) {
																this.geneOrdering[geneGroupIndex].push(i);
															}
														}
													}
												}, scope: _metaVizApp
		  									}		  					
		  					});
		  					_sortPanel.add({xtype:'checkbox', 
		  									boxLabel:'Hide columns with no results.',
		  									listeners: { 
		  										check : function(target, checked) {
		  											if (checked) {
		  												var filteringFn = function(o) {return (o.overallDifferentialExpressionScore == 0);};
		  												this.filterColumns(filteringFn);
		  												this.doLayout();
		  											} else {
//		  												this._unfilterColumns();
//		  												this.doLayout();		  												
		  											}
		  										}, scope: _metaVizApp
		  									},
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
		  															this._sortColumns('ASC', function(o1, o2){return o1.overallDifferentialExpressionScore - o2.overallDifferentialExpressionScore;});
		  															this.doLayout();
		  														}
		  													}, scope: _metaVizApp
		  												},
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
		  													}, scope: _metaVizApp
		  												},
		  						  					}]

		  					
		  					}); 

		  				});
					}, scope: this
				}
			}]			
		});
					    		
		Gemma.MetaHeatmapDataSelection.superclass.initComponent.apply(this, arguments);
	},
	onRender: function() {
		Gemma.MetaHeatmapDataSelection.superclass.onRender.apply(this, arguments);
		
		_sortPanel = this.ownerCt.sortPanel;
	}
});


Ext.reg('metaVizDataSelection', Gemma.MetaHeatmapDataSelection);

// MetaHeatmap Application
// Consist of 3 main panels:
// - gene labels
// - anlaysis labels
// - main visualization area
// It is controlled by window that allows sorting/filtering and choosing data.
Gemma.MetaHeatmapApp = Ext.extend(Ext.Panel, {	
	initComponent: function() {
	
		Ext.apply(this, {
			width: 1700,
			height: 850,
			_visualizationData : this.visualizationData,
			geneScores: this.visualizationData.geneScores,
			geneOrdering: null,
			
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
				y:25,
				ref: 'miniWindowButton',					
				text: 'tool #1',
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
					  ref: '_geneLabels',
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
			}],			
		});
		Gemma.MetaHeatmapApp.superclass.initComponent.apply(this, arguments);
		
		this.topLabelsPanel._setHeatmapContainer ( this._heatmapArea );
		this._heatmapArea._setTopLabelsBox ( this._rotatedLabelsBox );	

		//Default geneOrdering
		this.geneOrdering = new Array(this.visualizationData.geneNames.length);
		for (var geneGroupIndex = 0; geneGroupIndex < this.visualizationData.geneNames.length; geneGroupIndex++) {			
			this.geneOrdering[geneGroupIndex] = [];
			for (var i = 0; i < this.visualizationData.geneNames[geneGroupIndex].length; i++) {
				this.geneOrdering[geneGroupIndex].push(i);
			}
		}
		
		this.MiniWindowTool = new Ext.Window(
					{ width: 200,
			      	  height: 100,
			      	  border: false,
			      	  resizable: false,
			      	  draggable: false,
			      	  closable: false,
			      	  
			      	  title:"",
			      	  layout:"vbox",
			      	  items:[{xtype:'label', ref:'specificity', text:"Specificity: "},
			      	         {xtype:'label', ref:'pValue', text:"pValue: "},
			      	         {xtype:'label', ref:'foldChange', text:"Fold change: "},			      	         
					  ],			      			                                                                     												      	  
		});
		
	},
	onRender: function() {
		Gemma.MetaHeatmapApp.superclass.onRender.apply(this, arguments);	

		//this.MiniWindowTool.show();
		this.el.on('mousemove', function(event,target) {
			if (this.miniWindowButton.pressed == true) {
				var x = event.getPageX()+10;
				var y = event.getPageY()+15;
				this.MiniWindowTool.setPosition(x,y);
			}
		}, this );

	}

});
