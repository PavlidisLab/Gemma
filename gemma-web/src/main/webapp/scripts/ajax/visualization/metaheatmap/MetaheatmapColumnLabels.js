Ext.namespace('Gemma');


//ANALYSIS LABELS
Gemma.MetaHeatmapRotatedLabels = Ext.extend( Ext.BoxComponent, {	
	initComponent: function() {
		Ext.apply(this, {
			autoEl: { tag: 'canvas',
				  width: 1200,
			          height: 260
			},			
			_data: this.visualizationData,			
			_datasetGroupNames: this.datasetGroupNames,			
			_heatmapContainer: null,
			_setHeatmapContainer: function(container) {
				this._heatmapContainer = container;
				
			},
									
			_drawTopLabels : function ( hiDatasetGroup, hiColumnGroup, hiColumn, hiFactorValue ) {			
				var ctx = this.el.dom.getContext("2d");
				CanvasTextFunctions.enable(ctx);
				ctx.clearRect ( 0, 0, ctx.canvas.width, ctx.canvas.height );
				
				var xPosition = Gemma.MetaVisualizationConfig.cellWidth;
				
				for ( var currentDatasetGroupIndex = 0; currentDatasetGroupIndex < this._heatmapContainer.items.getCount(); currentDatasetGroupIndex++ ) {
					var dsPanel = this._heatmapContainer.items.get(currentDatasetGroupIndex);
					ctx.drawText('', Gemma.MetaVisualizationConfig.columnLabelFontSize,
								 xPosition, Gemma.MetaVisualizationConfig.columnLabelFontSize + 2,
								 this._datasetGroupNames[currentDatasetGroupIndex]);

					var alternateColors = 0;					
					for ( var currentDatasetColumnGroupIndex = 0; currentDatasetColumnGroupIndex < dsPanel.items.getCount(); currentDatasetColumnGroupIndex++ ) {
						var datasetColumnGroupPanel = dsPanel.items.get( currentDatasetColumnGroupIndex ); 						
						
						if (datasetColumnGroupPanel._hidden == false) {
							if (alternateColors == 1 ) {
								MiniPieLib.drawFilledRotatedRectangle( ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth, 246,
																	datasetColumnGroupPanel.getWidth(), 300,
																	Gemma.MetaVisualizationConfig.labelAngle,
																	Gemma.MetaVisualizationConfig.analysisLabelBackgroundColor1);		                     
		                        alternateColors = 0;
		                    } else {
		                    	MiniPieLib.drawFilledRotatedRectangle( ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth, 246,
		                    										datasetColumnGroupPanel.getWidth(), 300,
		                    										Gemma.MetaVisualizationConfig.labelAngle,
		                    										Gemma.MetaVisualizationConfig.analysisLabelBackgroundColor2);
		                        alternateColors = 1;                        
		                    }            							
							if ( hiDatasetGroup == currentDatasetGroupIndex 
									&& hiColumnGroup == datasetColumnGroupPanel._columnGroupIndex ) {
								ctx.drawRotatedText( xPosition, 246,
													 Gemma.MetaVisualizationConfig.labelAngle,
													 Gemma.MetaVisualizationConfig.columnLabelFontSize,
													 Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
													 "                          "+datasetColumnGroupPanel.datasetName);
							} else {
								ctx.drawRotatedText( xPosition, 246,
													 Gemma.MetaVisualizationConfig.labelAngle,
													 Gemma.MetaVisualizationConfig.columnLabelFontSize,
													 this._fontColor,
													 "                          "+datasetColumnGroupPanel.datasetName);
							}
						}
						
						var alternateColorsAnalysis = 0;
						for ( var currentAnalysisColumnGroupIndex = 0; currentAnalysisColumnGroupIndex < datasetColumnGroupPanel.items.getCount(); currentAnalysisColumnGroupIndex++ ) {
							var analysisColumnGroupPanel = datasetColumnGroupPanel.items.get( currentAnalysisColumnGroupIndex ); 						
							if (analysisColumnGroupPanel._hidden == true) continue;
								if (alternateColorsAnalysis == 1 ) {
									MiniPieLib.drawFilledRectangle( ctx, xPosition-Gemma.MetaVisualizationConfig.cellWidth + 2, 246,
																analysisColumnGroupPanel.getWidth() - 2, 3, 'rgba(10,100,10, 0.9)');		                     
									alternateColorsAnalysis = 0;
								} else {
									MiniPieLib.drawFilledRectangle( ctx, xPosition-Gemma.MetaVisualizationConfig.cellWidth+2, 246,
		                    									analysisColumnGroupPanel.getWidth() - 2, 3, 'rgba(10,100,100, 0.9)');
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
												ctx.drawRotatedText( xPosition, 246,
																	 Gemma.MetaVisualizationConfig.labelAngle, 
																	 Gemma.MetaVisualizationConfig.columnLabelFontSize,
																	 Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
																	 dColumn.factorValueNames[ dColumn.factorValueIds[i] ] );																			
											} else if ( dColumn.factorValueIds[i] == dColumn.baselineFactorValueId ) {
												ctx.drawRotatedText( xPosition, 246,
																	 Gemma.MetaVisualizationConfig.labelAngle,
																	 Gemma.MetaVisualizationConfig.columnLabelFontSize,
																	 Gemma.MetaVisualizationConfig.baselineFactorValueColor,
																	 dColumn.factorValueNames[ dColumn.factorValueIds[i] ] );									
											} else {
												ctx.drawRotatedText( xPosition, 246,
																	 Gemma.MetaVisualizationConfig.labelAngle, 
																	 Gemma.MetaVisualizationConfig.columnLabelFontSize,
																	 Gemma.MetaVisualizationConfig.factorValueDefaultColor,
																	 dColumn.factorValueNames[ dColumn.factorValueIds[i] ] );
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
												ctx.drawRotatedText( xPosition, 246,
																	 Gemma.MetaVisualizationConfig.labelAngle,
																	 Gemma.MetaVisualizationConfig.columnLabelFontSize,
																	 Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
																	 dColumn.factorName );										
											} else {									
												ctx.drawRotatedText( xPosition, 246,
																	 Gemma.MetaVisualizationConfig.labelAngle,
																	 Gemma.MetaVisualizationConfig.columnLabelFontSize,
																	 Gemma.MetaVisualizationConfig.defaultLabelColor,
																	 dColumn.factorName );
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
			}
			
		});
		
		Gemma.MetaHeatmapRotatedLabels.superclass.initComponent.apply(this, arguments);		
	},
	
	onRender: function() {		
		Gemma.MetaHeatmapRotatedLabels.superclass.onRender.apply(this, arguments);
		this.syncSize();
		
		this.el.on('click', function(e,t) {
			var popup = Gemma.MetaVisualizationPopups.makeDatasetInfoWindow(datasetName, datasetId);
			popup.show();
		}, this);		
	},
	
	refresh: function() {
		this._drawTopLabels();
	}
				
});

Ext.reg('metaVizRotatedLabels', Gemma.MetaHeatmapRotatedLabels);
