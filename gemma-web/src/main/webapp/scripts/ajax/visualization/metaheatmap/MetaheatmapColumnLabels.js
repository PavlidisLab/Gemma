Ext.namespace('Gemma');

// ANALYSIS LABELS
Gemma.MetaHeatmapRotatedLabels = Ext.extend(Ext.BoxComponent, {
	initComponent : function() {
		Ext.apply(this, {
			autoEl : {
				tag : 'canvas',
				width : this.applicationRoot._heatMapWidth * 1 +
						Math.floor(Gemma.MetaVisualizationConfig.labelBaseYCoor / 
							Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180)) + 80,
				height : Gemma.MetaVisualizationConfig.columnLabelHeight
			},
			_data : this.visualizationData,
			_datasetGroupNames : this.datasetGroupNames,
			_heatmapContainer : null,
			_setHeatmapContainer : function(container) {
				this._heatmapContainer = container;

			},

			_drawTopLabels : function(hiDatasetGroup, hiColumnGroup, hiColumn, hiFactorValue) {
				var ctx = this.el.dom.getContext("2d");
				CanvasTextFunctions.enable(ctx);
				ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);

				var xPosition = Gemma.MetaVisualizationConfig.cellWidth;

				for (var currentDatasetGroupIndex = 0; currentDatasetGroupIndex < this._heatmapContainer.items
						.getCount(); currentDatasetGroupIndex++) {
					var dsPanel = this._heatmapContainer.items.get(currentDatasetGroupIndex);
					var startPosition = xPosition;

					var alternateColors = 0;
					for (var currentDatasetColumnGroupIndex = 0; currentDatasetColumnGroupIndex < dsPanel.items
							.getCount(); currentDatasetColumnGroupIndex++) {
						var datasetColumnGroupPanel = dsPanel.items.get(currentDatasetColumnGroupIndex);
						var datasetShortName = dsPanel.dataColumns[currentDatasetColumnGroupIndex].datasetShortName;

						if (datasetColumnGroupPanel._hidden === false) {
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
										"                          " + datasetShortName);
							} else {
								ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
										Gemma.MetaVisualizationConfig.labelAngle,
										Gemma.MetaVisualizationConfig.columnLabelFontSize, this._fontColor,
										"                          " + datasetShortName);
							}
						}

						var alternateColorsAnalysis = 0;
						for (var currentAnalysisColumnGroupIndex = 0; currentAnalysisColumnGroupIndex < datasetColumnGroupPanel.items
								.getCount(); currentAnalysisColumnGroupIndex++) {
							var analysisColumnGroupPanel = datasetColumnGroupPanel.items
									.get(currentAnalysisColumnGroupIndex);
							if (analysisColumnGroupPanel._hidden === true) {
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
								if (dColumn._expandButton.pressed) {
									for (var i = 0; i < dColumn._factorValueNames.length; i++) {
										if (i === 0) {
											if(dColumn.miniPieValue>0){
												MiniPieLib.drawMiniPie(ctx, xPosition - 4, Gemma.MetaVisualizationConfig.columnLabelHeight-5, 9,
													Gemma.MetaVisualizationConfig.miniPieColor, dColumn.miniPieValue);
											}else{
												MiniPieLib.drawMiniPie(ctx, xPosition - 4, Gemma.MetaVisualizationConfig.columnLabelHeight-5, 9,
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
													dColumn.factorValueNames[dColumn.factorValueIds[i]]);
										} else if (dColumn.factorValueIds[i] == dColumn.baselineFactorValueId) {
											ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
													Gemma.MetaVisualizationConfig.labelAngle,
													Gemma.MetaVisualizationConfig.columnLabelFontSize,
													Gemma.MetaVisualizationConfig.baselineFactorValueColor,
													dColumn.factorValueNames[dColumn.factorValueIds[i]]);
										} else {
											ctx.drawRotatedText(xPosition, Gemma.MetaVisualizationConfig.labelBaseYCoor,
													Gemma.MetaVisualizationConfig.labelAngle,
													Gemma.MetaVisualizationConfig.columnLabelFontSize,
													Gemma.MetaVisualizationConfig.factorValueDefaultColor,
													dColumn.factorValueNames[dColumn.factorValueIds[i]]);
										}
										xPosition += Gemma.MetaVisualizationConfig.cellWidth;
									}
									xPosition += Gemma.MetaVisualizationConfig.columnSeparatorWidth;
								} else {
									if (!dColumn.hidden) {
										var factorText = dColumn.factorCategory ? dColumn.factorCategory : dColumn.factorName;
										factorText = Ext.util.Format.ellipsis(factorText, 25, false);
										
										if (dColumn.miniPieValue > 0) {
											MiniPieLib.drawMiniPie(ctx, xPosition - 4, Gemma.MetaVisualizationConfig.columnLabelHeight - 5,
											 9, Gemma.MetaVisualizationConfig.miniPieColor, dColumn.miniPieValue);
										}
										else {
											MiniPieLib.drawMiniPie(ctx, xPosition - 4, Gemma.MetaVisualizationConfig.columnLabelHeight - 5,
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

					// ctx.font =
					// (Gemma.MetaVisualizationConfig.columnLabelFontSize + 2) +
					// "px sans-serif";
					// var name =
					// this._datasetGroupNames[currentDatasetGroupIndex];
					// var metrics = ctx.measureText(name);
					// labelCentre = labelCentre - metrics.width/2;

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
		var columnWidth = Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth; // shorter
		// name
		// to
		// simplify
		// access
		var adjustedx = x - 2;
		var adjustedY = y * -1 + Gemma.MetaVisualizationConfig.labelBaseYCoor;
		var lamda = Math.floor(adjustedY / Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180));
		var column = Math.floor((adjustedx * 1 - lamda) / columnWidth);
		return column;
	},
	getAnalysisObject : function(e, t) {
		var rawColumnNumber = this.__calculateIndexFromXY(e.getPageX() - Ext.get(t).getX(), e.getPageY() -
						Ext.get(t).getY());

		// if mouse is outside heatmap area, return null
		if (rawColumnNumber >= (this.applicationRoot.TOTAL_NUMBER_OF_COLUMNS - this.applicationRoot.TOTAL_NUMBER_OF_HIDDEN_COLUMNS)) {
			return null;
		}

		var columnGroups = this.applicationRoot._imageArea._heatmapArea.items.items; // for
		// ease
		// of
		// access

		// get [column group number] and [experiment index within the group]
		var columnGroupIndex = 0;
		var columnWithinGroup = rawColumnNumber;
		var experimentWithinGroupIndex = 0;
		var columnWithinExperiment = 0;
		var prevColSum = 0;

		// get the group index and the column index within the group
		while (columnWithinGroup >= (columnGroups[columnGroupIndex].dataColumns.length - columnGroups[columnGroupIndex]._columnsHidden)) {
			columnWithinGroup = columnWithinGroup -
					(columnGroups[columnGroupIndex].dataColumns.length - columnGroups[columnGroupIndex]._columnsHidden);
			columnGroupIndex++;
		}

		var colSum = columnGroups[columnGroupIndex].items.items[experimentWithinGroupIndex].dataColumns.length -
				columnGroups[columnGroupIndex].items.items[experimentWithinGroupIndex]._columnsHidden;

		// get the experiment index within the group
		while (columnWithinGroup >= colSum) {
			prevColSum = colSum;
			experimentWithinGroupIndex++;
			colSum = colSum + columnGroups[columnGroupIndex].items.items[experimentWithinGroupIndex].dataColumns.length -
					columnGroups[columnGroupIndex].items.items[experimentWithinGroupIndex]._columnsHidden;
		}
		// get the column number within the experiment
		columnWithinExperiment = columnWithinGroup - prevColSum;

		analysisObj = this.applicationRoot._imageArea._heatmapArea.items.items[columnGroupIndex].items.items[experimentWithinGroupIndex].dataColumns[columnWithinExperiment];
		return analysisObj;

	},
	onRender : function() {
		Gemma.MetaHeatmapRotatedLabels.superclass.onRender.apply(this, arguments);
		this.syncSize();

		this.el.on('click', function(e, t) {
					var analysisObj = this.getAnalysisObject(e, t);

					if (analysisObj !== null && analysisObj !== undefined) {
						var popup = Gemma.MetaVisualizationPopups.makeDatasetInfoWindow(analysisObj.datasetName,
								analysisObj.datasetShortName, analysisObj.datasetId);
						// popup.show();
					}

				}, this);

		this.el.on('mouseover', function(e, t) {
					document.body.style.cursor = 'pointer';
				});
		this.el.on('mouseout', function(e, t) {
					document.body.style.cursor = 'default';
				});
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

				var analysisObj = this.getAnalysisObject(e, t);

				if (analysisObj !== null && analysisObj !== undefined) {

					// get the factor values into a readable form
					var factorValues = [];
					for (k = 0; k < analysisObj.contrastsFactorValueIds.length; k++) {
						factorValues.push(" " + analysisObj.contrastsFactorValues[analysisObj.contrastsFactorValueIds[k]]);
					}

					this.applicationRoot._hoverDetailsPanel.setPagePosition(e.getPageX()+20 , e.getPageY()+20 );
					this.applicationRoot._hoverDetailsPanel.update({
								type : 'experiment',
								datasetName : analysisObj.datasetName,
								datasetShortName : analysisObj.datasetShortName,
								datasetId : analysisObj.datasetId,
								factorName : analysisObj.factorName,
								factorCategory : analysisObj.factorCategory,
								baseline : analysisObj.baselineFactorValue,
								factorValues : factorValues

							});
				}
			}

		}, this);
	},

	refresh : function() {
		this._drawTopLabels();
	}

});

Ext.reg('metaVizRotatedLabels', Gemma.MetaHeatmapRotatedLabels);
