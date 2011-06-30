Ext.namespace('Gemma');

// ANALYSIS LABELS
Gemma.MetaHeatmapRotatedLabels = Ext.extend(Ext.BoxComponent, {
	initComponent : function() {
		Ext.apply(this, {
			
			/*autoEl : {
				tag : 'canvas',
			},*/
			autoEl : 'canvas',
			/*	width : this.applicationRoot._heatMapWidth * 1 +
						Math.floor(Gemma.MetaVisualizationConfig.labelBaseYCoor / 
							Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180)) + 80,
				height : Gemma.MetaVisualizationConfig.columnLabelHeight,
			*/_data : this.visualizationData,
			_datasetGroupNames : this.datasetGroupNames,
			applicationRoot : this.applicationRoot,

			_drawTopLabels : function(hiDatasetGroup, hiColumnGroup, hiColumn, hiFactorValue) {				
				var mainHeatmapPanel = this.applicationRoot._imageArea._heatmapArea;
				var newWidth = mainHeatmapPanel.getWidth() + Gemma.MetaVisualizationConfig.labelExtraSpace;
				var height = Gemma.MetaVisualizationConfig.columnLabelHeight;
				
				var ctx = Gemma.MetaVisualizationUtils.getCanvasContext(this.el.dom);
				ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
				ctx.canvas.width = newWidth;
				ctx.canvas.height = height;
				this.setWidth(newWidth);
				this.setHeight(height);
				
				CanvasTextFunctions.enable(ctx);

				var xPosition = Gemma.MetaVisualizationConfig.cellWidth;				
					
				for (var currentDatasetGroupIndex = 0; currentDatasetGroupIndex < mainHeatmapPanel.items.getCount(); currentDatasetGroupIndex++) {
					var dsPanel = mainHeatmapPanel.items.get(currentDatasetGroupIndex);
					var startPosition = xPosition;

					var alternateColors = 0;
					for (var currentDatasetColumnGroupIndex = 0; currentDatasetColumnGroupIndex < dsPanel.items
							.getCount(); currentDatasetColumnGroupIndex++) {
						var datasetColumnGroupPanel = dsPanel.items.get(currentDatasetColumnGroupIndex);
						var datasetShortName = datasetColumnGroupPanel.datasetShortName;

						if (datasetColumnGroupPanel.isFiltered === false) {
							if (alternateColors == 1) {
								MiniPieLib.drawFilledRotatedRectangle(ctx, xPosition -
										Gemma.MetaVisualizationConfig.cellWidth,
										Gemma.MetaVisualizationConfig.labelBaseYCoor, datasetColumnGroupPanel
												.getWidth(), 300, Gemma.MetaVisualizationConfig.labelAngle,
										Gemma.MetaVisualizationConfig.analysisLabelBackgroundColor1);
								alternateColors = 0;
							} else {
								MiniPieLib.drawFilledRotatedRectangle(ctx, xPosition -
												Gemma.MetaVisualizationConfig.cellWidth,
										Gemma.MetaVisualizationConfig.labelBaseYCoor, datasetColumnGroupPanel
												.getWidth(), 300, Gemma.MetaVisualizationConfig.labelAngle,
										Gemma.MetaVisualizationConfig.analysisLabelBackgroundColor2);
								alternateColors = 1;
							}
							if (hiDatasetGroup === currentDatasetGroupIndex &&
									hiColumnGroup == datasetColumnGroupPanel._columnGroupIndex) {
								ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
										Gemma.MetaVisualizationConfig.labelAngle,
										Gemma.MetaVisualizationConfig.columnLabelFontSize,
										Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
										"                          " + Ext.util.Format.ellipsis(datasetShortName, 10, false));
							} else {
								ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
										Gemma.MetaVisualizationConfig.labelAngle,
										Gemma.MetaVisualizationConfig.columnLabelFontSize, this._fontColor,
										"                          " + Ext.util.Format.ellipsis(datasetShortName, 10, false));
							}
						}

						var alternateColorsAnalysis = 0;
						for (var currentAnalysisColumnGroupIndex = 0; currentAnalysisColumnGroupIndex < datasetColumnGroupPanel.items
								.getCount(); currentAnalysisColumnGroupIndex++) {
							var analysisColumnGroupPanel = datasetColumnGroupPanel.items
									.get(currentAnalysisColumnGroupIndex);
							if (analysisColumnGroupPanel.isFiltered === true) {
								continue;
							}
							if (alternateColorsAnalysis == 1) {
								MiniPieLib.drawFilledRectangle(ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth +
												2, Gemma.MetaVisualizationConfig.labelBaseYCoor,
										analysisColumnGroupPanel.getWidth() - 2, 3, 'rgba(10,100,10, 0.9)');
								alternateColorsAnalysis = 0;
							} else {
								MiniPieLib.drawFilledRectangle(ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth +
												2, Gemma.MetaVisualizationConfig.labelBaseYCoor,
										analysisColumnGroupPanel.getWidth() - 2, 3, 'rgba(10,100,100, 0.9)');
								alternateColorsAnalysis = 1;
							}

							for (var currentColumn = 0; currentColumn < analysisColumnGroupPanel.items.getCount(); currentColumn++) {
								var dColumn = analysisColumnGroupPanel.items.get(currentColumn);
								if (dColumn.isFiltered === true) {
									continue;
								}
								if (dColumn.expandButton_.pressed) {
									for (var i = 0; i < dColumn.factorValueIds.length; i++) {
										if (i === 0) {
											if(dColumn.miniPieValue >= 0){
												MiniPieLib.drawMiniPie(ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth + 5, Gemma.MetaVisualizationConfig.columnLabelHeight-5, 9,
													Gemma.MetaVisualizationConfig.miniPieColor, dColumn.miniPieValue);
											}else{
												MiniPieLib.drawMiniPie(ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth + 5, Gemma.MetaVisualizationConfig.columnLabelHeight-5, 9,
													Gemma.MetaVisualizationConfig.miniPieColorInvalid, 360);
											}
											
										}
										if (hiDatasetGroup === currentDatasetGroupIndex &&
												hiColumnGroup === datasetColumnGroupPanel._columnGroupIndex &&
												i === hiFactorValue && currentColumn === hiColumn) {
											ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
													Gemma.MetaVisualizationConfig.labelAngle,
													Gemma.MetaVisualizationConfig.columnLabelFontSize,
													Gemma.MetaVisualizationConfig.analysisLabelHighlightColor,
													Ext.util.Format.ellipsis( dColumn.factorValueNames[dColumn.factorValueIds[i]] , 25, false ));
										} else if (dColumn.factorValueIds[i] == dColumn.baselineFactorValueId) {
											ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
													Gemma.MetaVisualizationConfig.labelAngle,
													Gemma.MetaVisualizationConfig.columnLabelFontSize,
													Gemma.MetaVisualizationConfig.baselineFactorValueColor,
													Ext.util.Format.ellipsis( dColumn.factorValueNames[dColumn.factorValueIds[i]] , 25, false ));
										} else {
											ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
													Gemma.MetaVisualizationConfig.labelAngle,
													Gemma.MetaVisualizationConfig.columnLabelFontSize,
													Gemma.MetaVisualizationConfig.factorValueDefaultColor,
													Ext.util.Format.ellipsis( dColumn.factorValueNames[dColumn.factorValueIds[i]] , 25, false ));
										}
										xPosition += Gemma.MetaVisualizationConfig.cellWidth;
									}
									xPosition += Gemma.MetaVisualizationConfig.columnSeparatorWidth;
								} else {
									if (!dColumn.isFiltered) {
										var factorText = dColumn.factorCategory ? dColumn.factorCategory : dColumn.factorName;
										factorText = Ext.util.Format.ellipsis(factorText, 25, false);
										
										if (dColumn.miniPieValue >= 0) {
											MiniPieLib.drawMiniPie(ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth + 5, Gemma.MetaVisualizationConfig.columnLabelHeight - 5,
											 9, Gemma.MetaVisualizationConfig.miniPieColor, dColumn.miniPieValue);
										}
										else {
											MiniPieLib.drawMiniPie(ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth + 5, Gemma.MetaVisualizationConfig.columnLabelHeight - 5,
											 9, Gemma.MetaVisualizationConfig.miniPieColorInvalid, 360);
										}
										if (hiDatasetGroup === currentDatasetGroupIndex &&
										hiColumnGroup === columnGroup._columnGroupIndex &&
										hiColumn === currentColumn) {
											ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor, 
												Gemma.MetaVisualizationConfig.labelAngle, Gemma.MetaVisualizationConfig.columnLabelFontSize, 
												Gemma.MetaVisualizationConfig.analysisLabelHighlightColor, factorText);
										}
										else {
											ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor, 
												Gemma.MetaVisualizationConfig.labelAngle, Gemma.MetaVisualizationConfig.columnLabelFontSize, 
												Gemma.MetaVisualizationConfig.defaultLabelColor, factorText);
										}
										xPosition += Gemma.MetaVisualizationConfig.cellWidth;
										xPosition += Gemma.MetaVisualizationConfig.columnSeparatorWidth;
									}
								}
							}
						}
					}

					// get the x coordinate of the middle of the experiment
					// group's labels (not columns)
					var columnCentre = Math.floor(startPosition + (xPosition - startPosition) / 2);
					var lamda = Math.floor(Gemma.MetaVisualizationConfig.labelBaseYCoor /
							Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180));
					var labelCentre = Math.floor(columnCentre + lamda);

					// line to mark end of group
					MiniPieLib.drawFilledRotatedRectangle(ctx, xPosition - Gemma.MetaVisualizationConfig.cellWidth,
							Gemma.MetaVisualizationConfig.labelBaseYCoor + 2, 2, Gemma.MetaVisualizationConfig.columnLabelHeight+80,
							Gemma.MetaVisualizationConfig.labelAngle, 'rgba(10,100,100, 0.9)');

					// draw background for titles
					ctx.fillStyle = 'white';
					ctx.fillRect(startPosition + lamda - 3 * Gemma.MetaVisualizationConfig.cellWidth, 0,
							Math.max((xPosition - startPosition) * 2 + Gemma.MetaVisualizationConfig.columnSeparatorWidth,200), 
								Gemma.MetaVisualizationConfig.columnLabelHeight - Gemma.MetaVisualizationConfig.labelBaseYCoor + 15);

					// adjust for text width
					var textStart = labelCentre - (Gemma.MetaVisualizationConfig.columnLabelFontSize + 2) *
							 this._datasetGroupNames[currentDatasetGroupIndex].length / 2;

					// draw experiment group titles
					ctx.drawText('', Gemma.MetaVisualizationConfig.columnLabelFontSize, startPosition + lamda -
									Gemma.MetaVisualizationConfig.cellWidth,
							Gemma.MetaVisualizationConfig.columnLabelFontSize + 2,
							this._datasetGroupNames[currentDatasetGroupIndex]);

					xPosition += Gemma.MetaVisualizationConfig.groupSeparatorWidth;
				}
				this._widthOfColumns = xPosition - Gemma.MetaVisualizationConfig.cellWidth -
						Gemma.MetaVisualizationConfig.groupSeparatorWidth;
			}

		});

		Gemma.MetaHeatmapRotatedLabels.superclass.initComponent.apply(this, arguments);
	},
	__calculateIndexFromXY : function(x, y) {
		var columnWidth = Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth; 
		var adjustedx = x - 2;
		var adjustedY = y * -1 + Gemma.MetaVisualizationConfig.labelBaseYCoor;
		var lamda = Math.floor(adjustedY / Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180));
		var column = Math.floor((adjustedx * 1 - lamda) / columnWidth);
		return column;
	},
				
	getAnalysisPanelByX : function ( x ) {
		//TODO: refactor me!
		var datasetGroupPanels = this.applicationRoot._imageArea._heatmapArea.items;
		for (var i = 0; i < datasetGroupPanels.getCount(); i++) {
			var datasetGroupPanel = datasetGroupPanels.get(i);
			var XY = datasetGroupPanel.getPosition();
			if ( x >= XY[0] && x<XY[0] + datasetGroupPanel.getWidth() ) {
				var experimentPanels = datasetGroupPanel.items;
				for (var j = 0; j < experimentPanels.getCount(); j++) {
					var experimentPanel = experimentPanels.get(j);
					XY = experimentPanel.getPosition();
					if ( x >= XY[0] && x<XY[0] + experimentPanel.getWidth() ) {
						var analysisPanels = experimentPanel.items;						
						for (var k = 0; k < analysisPanels.getCount(); k++) {
							var analysisPanel = analysisPanels.get(k);
							XY = analysisPanel.getPosition();
							if ( x >= XY[0] && x<XY[0] + analysisPanel.getWidth() ) {
								var columnPanels = analysisPanel.items;						
								for (var p = 0; p < columnPanels.getCount(); p++) {
									var columnPanel = columnPanels.get(p);
									XY = columnPanel.getPosition();
									if ( x >= XY[0] && x<XY[0] + columnPanel.getWidth() ) {return columnPanel;}
								}
							}						
						}					
					}	
				}
			}			
		}
		return null;
	},	
	
	onRender : function() {
		Gemma.MetaHeatmapRotatedLabels.superclass.onRender.apply(this, arguments);
		
				
		this.syncSize();

		this.el.on('click', function(e, t){
			var y = e.getPageY() - Ext.get(t).getY();
			var adjustedX = e.getPageX() - (Gemma.MetaVisualizationConfig.labelBaseYCoor - y) / Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180);
			var analysisObj = this.getAnalysisPanelByX(adjustedX);
			
			if (analysisObj !== null && analysisObj !== undefined) {
				var popup = Gemma.MetaVisualizationPopups.makeDatasetInfoWindow(analysisObj._dataColumn.datasetName, analysisObj._dataColumn.datasetShortName, analysisObj._dataColumn.datasetId);
			}
			
		}, this);

		this.el.on('mouseover', function(e, t) {
			document.body.style.cursor = 'pointer';
		});
		this.el.on('mouseout', function(e, t) {
			document.body.style.cursor = 'default';
			this.applicationRoot._imageArea._hoverDetailsPanel.hide();
		},this);
		this.el.on('mousemove', function(e, t) {

			// if mouse is in non-label area of component, don't show pointer
			// cursor
			var x = e.getPageX() - Ext.get(t).getX();
			var y = e.getPageY() - Ext.get(t).getY();
			if (y > 20 &&
					(x < Gemma.MetaVisualizationConfig.labelBaseYCoor *
							Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180) && (Gemma.MetaVisualizationConfig.labelBaseYCoor - y) > 
							Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180) * x) ||
					(x > this._widthOfColumns && (Gemma.MetaVisualizationConfig.labelBaseYCoor - y) < (Math
							.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180) * (x - this._widthOfColumns)))) {
				document.body.style.cursor = 'default';
			} else {
				document.body.style.cursor = 'pointer';
				
				
				var adjustedX = e.getPageX() - (Gemma.MetaVisualizationConfig.labelBaseYCoor - y) / Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180);
				var analysisObj = this.getAnalysisPanelByX(adjustedX);
				

				if (analysisObj !== null && analysisObj !== undefined) {

					// get the factor values into a readable form
					var factorValues = [];
					for (k = 0; k < analysisObj.factorValueIds.length; k++) {
						factorValues.push(" " + analysisObj.factorValueNames[analysisObj.factorValueIds[k]]);
					}

					this.applicationRoot._imageArea._hoverDetailsPanel.show();
					this.applicationRoot._imageArea._hoverDetailsPanel.setPagePosition(e.getPageX()+20 , e.getPageY()+20 );
					// if hovering over a mini pie, show specificity info
					if(y < Gemma.MetaVisualizationConfig.columnLabelHeight - 10 ){ // 10 is mini-pie height
						this.applicationRoot._imageArea._hoverDetailsPanel.update({
						type: 'experiment',
						datasetName: analysisObj.dataColumn.datasetName,
						datasetShortName: analysisObj.dataColumn.datasetShortName,
						datasetId: analysisObj.dataColumn.datasetId,
						factorName: analysisObj.dataColumn.factorName,
						factorCategory: analysisObj.dataColumn.factorCategory,
						baseline: (analysisObj.dataColumn.baselineFactorValue === "null")?"-":analysisObj.dataColumn.baselineFactorValue,
						factorValues: factorValues
					
					});
					}else{
						this.applicationRoot._imageArea._hoverDetailsPanel.update({
						type: 'minipie',
						numberOfProbesTotal: analysisObj.dataColumn.numberOfProbesTotal,
						numberOfProbesDiffExpressed: analysisObj.dataColumn.numberOfProbesDiffExpressed, 
						numberOfProbesDownRegulated: analysisObj.dataColumn.numberOfProbesDownRegulated, 
						numberOfProbesUpRegulated: analysisObj.dataColumn.numberOfProbesUpRegulated
					});
					}
					
				}
			}

		}, this);
	},

	refresh : function() {
		this._drawTopLabels();
	}

});

Ext.reg('metaVizRotatedLabels', Gemma.MetaHeatmapRotatedLabels);
