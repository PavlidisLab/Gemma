Ext.namespace('Gemma');

//
// Column of heatmap cells for one gene group
//  
//
Gemma.MetaHeatmapColumn = Ext.extend(Ext.BoxComponent, {
	initComponent : function() {
		Ext.apply(this, {
					autoEl : {
						tag : 'canvas',
						width : Gemma.MetaVisualizationConfig.cellWidth,
						height : Gemma.MetaVisualizationConfig.cellHeight * this.visualizationSubColumnData.length
					},
					margins : {
						top : 0,
						right : 0,
						bottom : Gemma.MetaVisualizationConfig.groupSeparatorHeight,
						left : 0
					},

					applicationRoot : this.applicationRoot,

					cellHeight : Gemma.MetaVisualizationConfig.cellHeight, // shorter
					// name
					// to
					// simplify
					// access
					cellWidth : Gemma.MetaVisualizationConfig.cellWidth, // shorter
					// name
					// to
					// simplify
					// access

					geneGroupIndex : this.rowGroup, // gene group index
					columnIndex : this.columnIndex, // index within analysis
					// panel
					analysisColumnGroupIndex : this.columnGroupIndex, // index
					// of
					// analysis
					// panel
					datasetColumnGroupIndex : this.datasetColumnGroupIndex, // index
					// of
					// dataset
					// column
					// group
					// panel
					datasetGroupIndex : this.datasetGroupIndex, // dataset group
					// index

					_visualizationValues : this.visualizationSubColumnData,
					pValues : this.pValuesSubColumnData,

					_contrastsVisualizationValues : this.visualizationContrastsSubColumnData,
					// contrastsFoldChanges: this.contrastsFoldChanges,
					_contrastsFactorValues : this.factorValues,

					_discreteColorRange : Gemma.MetaVisualizationConfig.basicColourRange,
					_discreteColorRangeContrasts : Gemma.MetaVisualizationConfig.contrastsColourRange,
					_isExpanded : false,

					overallDifferentialExpressionScore : null,
					missingValuesScore : null
				});
		Gemma.MetaHeatmapColumn.superclass.initComponent.apply(this, arguments);
	},

	/**
	 * 
	 * @param {}
	 *            doResize
	 * @param {}
	 *            highlightRow
	 */
	_drawHeatmapColumn : function(doResize, highlightRow) {
		var expandableColumnPanel = this.ownerCt;

		var ctx = this.el.dom.getContext("2d");
		var oldWidth = ctx.canvas.width;
		var newWidth = this.cellWidth;
		ctx.canvas.width = newWidth;
		ctx.clearRect(0, 0, this.el.dom.width, this.el.dom.height);

		for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupIndex].length; i++) {
			var color = this._discreteColorRange
					.getCellColorString(this._visualizationValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][i]]);
			this._drawHeatmapCell(ctx, color, i, 0);
			var geneId = this.applicationRoot.visualizationData.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][i]];
			if (this.applicationRoot._selectedGenes.indexOf(geneId) != -1) {
				this._drawHeatmapSelectedRowCell(ctx, i, 0);
			}
			if (highlightRow === i) {
				this._drawHeatmapCellBox(ctx, highlightRow, 0);
			}
		}

		if (doResize) {
			var widthChange = newWidth - oldWidth;
			expandableColumnPanel.setWidth(newWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth);
			expandableColumnPanel.updateAllParentContainersWidthBy(widthChange);
		}

		this.setWidth(newWidth);
	},

	/**
	 * 
	 * @param {}
	 *            doResize
	 * @param {}
	 *            highlightRow
	 * @param {}
	 *            highlightColumn
	 */
	_drawContrasts : function(doResize, highlightRow, highlightColumn) {
		var ctx = this.el.dom.getContext("2d");

		var expandableColumnPanel = this.ownerCt;

		var oldWidth = ctx.canvas.width;
		var newWidth = this.cellWidth * this._contrastsFactorValues.length;

		// Resize and clear canvas
		ctx.canvas.width = newWidth;
		ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);

		// Draw cells
		for (var factorValueIndex = 0; factorValueIndex < this._contrastsFactorValues.length; factorValueIndex++) {
			for (var geneIndex = 0; geneIndex < this.applicationRoot.geneOrdering[this.geneGroupIndex].length; geneIndex++) {
				// var color =
				// this._discreteColorRangeContrasts.getCellColorString(this._contrastsVisualizationValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][geneIndex]][factorValueIndex]);
				this._drawHeatmapCell(ctx, color, geneIndex, factorValueIndex);
				if (highlightRow === geneIndex && highlightColumn === factorValueIndex) {
					this._drawHeatmapCellBox(ctx, highlightRow, highlightColumn);
				}
			}
		}

		if (doResize) {
			var widthChange = newWidth - oldWidth;
			expandableColumnPanel.setWidth(expandableColumnPanel.getWidth() + widthChange);
			expandableColumnPanel.updateAllParentContainersWidthBy(widthChange);
		}

		this.setWidth(newWidth);
	},

	/**
	 * 
	 * @param {}
	 *            ctx
	 * @param {}
	 *            rowIndex
	 * @param {}
	 *            columnIndex
	 */
	_drawHeatmapCellBox : function(ctx, rowIndex, columnIndex) {
		ctx.save();
		ctx.strokeStyle = Gemma.MetaVisualizationConfig.cellHighlightColor;
		ctx.strokeRect(this.cellWidth * columnIndex, this.cellHeight * rowIndex, this.cellWidth, this.cellHeight);
		ctx.restore();
	},
	
	/**
	 * 
	 * @param {}
	 *            ctx
	 * @param {}
	 *            rowIndex
	 * @param {}
	 *            columnIndex
	 */
	_drawHeatmapSelectedRowCell : function(ctx, rowIndex, columnIndex) {
		ctx.save();
		ctx.strokeStyle = Gemma.MetaVisualizationConfig.rowCellSelectColor;
		ctx.beginPath();
		ctx.moveTo(this.cellWidth * columnIndex, this.cellHeight * rowIndex);
		ctx.lineTo(this.cellWidth * columnIndex + this.cellWidth, this.cellHeight * rowIndex);
		ctx.moveTo(this.cellWidth * columnIndex, this.cellHeight * (rowIndex+1) );
		ctx.lineTo(this.cellWidth * columnIndex + this.cellWidth, this.cellHeight * (rowIndex+1));
		ctx.stroke();
		ctx.restore();
	},

	/**
	 * 
	 * @param {}
	 *            ctx
	 * @param {}
	 *            color
	 * @param {}
	 *            rowIndex
	 * @param {}
	 *            columnIndex
	 */
	_drawHeatmapCell : function(ctx, color, rowIndex, columnIndex) {
		ctx.fillStyle = color;
		ctx.fillRect(columnIndex * this.cellWidth, rowIndex * this.cellHeight, this.cellWidth, this.cellHeight);
	},

	/**
	 * 
	 * @param {}
	 *            x
	 * @param {}
	 *            y
	 * @return {}
	 */
	__calculateIndexFromXY : function(x, y) {
		var row = Math.floor(y / this.cellHeight);
		var column = Math.floor(x / this.cellWidth);
		return {
			'row' : row,
			'column' : column
		};
	},

	/**
	 * @Override
	 */
	onRender : function() {

		// After the component has been rendered, disable the default browser
		// context menu
		// problem: this disables right click everywhere on the page (not just
		// over component), pretty annoying
		// Ext.getBody().on("contextmenu", Ext.emptyFn, null, {preventDefault:
		// true});

		Gemma.MetaHeatmapColumn.superclass.onRender.apply(this, arguments);
		this._drawHeatmapColumn();

		this.el.on('click', function(e, t) {
			var index = this.__calculateIndexFromXY(e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());

			/*
			 * What to show? COLLAPSED - Dataset name / link - Analysis type /
			 * link to design - Factor - Gene / link to gene page - Number of
			 * probes matching this gene? - Specificity? (in overall dataset
			 * sense) - Expression profile
			 * 
			 * EXPANDED - Dataset name / link - Analysis type / link to design -
			 * Factor - Baseline - Factor Value - Gene / link to gene page -
			 * Fold change
			 * 
			 */

			// if user held down ctrl while clicking, select column or gene
			// instead of popping up window
			// if (e.ctrlKey == true) {
			// Ext.Msg.alert('CTRL Used', 'Ctrl+Click was used');
			// }
			var vizWindow = new Gemma.VisualizationWithThumbsWindow({
						title : 'Gene Expression',
						thumbnails : false
					});
			var eeId = this.ownerCt._dataColumn.datasetId;
			var _datasetGroupPanel = this.ownerCt.ownerCt.ownerCt;
			vizWindow.show({
				params : [
						[eeId],
						[this.applicationRoot._imageArea._heatmapArea.geneIds[this.rowGroup][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]]]
			});
		}, this);

		// detect right click.
		// this.el.on('contextmenu', function() {
		// Ext.Msg.alert("Right-o!","You right-clicked!");
		// });

		this.el.on('mouseover', function(e, t) {
					document.body.style.cursor = 'pointer';
				});
		this.el.on('mouseout', function(e, t) {
					document.body.style.cursor = 'default';
					this.applicationRoot._imageArea._geneLabels.unhighlightGene(this.rowGroup);
				}, this);
		this.el.on('mousemove', function(e, t) {
			var index = this.__calculateIndexFromXY(e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());
			if (this._isExpanded) {
				this._drawContrasts(false, index.row, index.column);
				// this.applicationRoot._rotatedLabelsBox._drawTopLabels (
				// this._datasetGroupIndex, this._columnGroupIndex,
				// this._columnIndex, index.column );
				this.applicationRoot.MiniWindowTool.setTitle("Gene: " + " Experiment: " + " Factor: "+
						" Factor value: ");
				this.applicationRoot.MiniWindowTool.specificity.setText("Specificity: " + 100 *
						this.ownerCt.miniPieValue / 360);
				this.applicationRoot.MiniWindowTool.pValue.setText("pValue: " + this.pValues[index.row]);
				this.applicationRoot.MiniWindowTool.foldChange.setText("Fold change: " +
						this.contrastsFoldChanges[index.row][index.column]);
			} else {
				this._drawHeatmapColumn(false, index.row);
				// this.applicationRoot._rotatedLabelsBox._drawTopLabels (
				// this._datasetGroupIndex, this._columnGroupIndex,
				// this._columnIndex );
				this.applicationRoot.MiniWindowTool.setTitle("Gene: " + " Experiment: " + " Factor: ");
				this.applicationRoot.MiniWindowTool.specificity.setText("Specificity: " + 100 *
						this.ownerCt.miniPieValue / 360);
				this.applicationRoot.MiniWindowTool.pValue.setText("pValue: " +
						this.pValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]);
				// this.applicationRoot.MiniWindowTool.foldChange.setText("Fold
				// change: " +
				// this._contrastsVisualizationValues[index.row][index.column]);
			}
			this.applicationRoot._imageArea._geneLabels.highlightGene(this.rowGroup, index.row); // highlights
			// the
			// gene
			// symbol
			// in
			// red

			// format p value
			formatPVal = function(p) {
				if (p === null) {
					return '-';
				}
				if (p < 0.0001) {
					return sprintf("%.3e", p);
				} else {
					return sprintf("%.3f", p);
				}
			};
			this.applicationRoot._hoverDetailsPanel.setPagePosition(e.getPageX()+20 , e.getPageY()+20 );
			this.applicationRoot._hoverDetailsPanel.update({
				type : 'cell',
				// row: index.row,
				// column: this.rowGroup,
				// specificity: 100 * this.ownerCt.miniPieValue / 360,
				pvalue : formatPVal(this.pValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]),
				// baselineFactorValue: this.ownerCt.baselineFactorValue,
				factorName : this.ownerCt._dataColumn.factorName, // can also
				// get
				// factor
				// values,
				// using
				// this.ownerCt.contrastsFactorValues
				// and
				// this.ownerCt.contrastsFactorValueIds
				factorCategory : this.ownerCt._dataColumn.factorCategory,
				factorDescription : this.ownerCt._dataColumn.factorDescription,
				factorId : this.ownerCt._dataColumn.factorId,
				datasetId : this.ownerCt._dataColumn.datasetId,
				datasetName : this.ownerCt._dataColumn.datasetName,
				datasetShortName : this.ownerCt._dataColumn.datasetShortName,
				// numberOfProbes: this.ownerCt.numberOfProbes,
				// numberOfProbesDiffExpressed:
				// this.ownerCt.numberOfProbesDiffExpressed,
				// numberOfProbesDownRegulated:
				// this.ownerCt.numberOfProbesDownRegulated,
				// numberOfProbesUpRegulated:
				// this.ownerCt.numberOfProbesUpRegulated,
				// numberOfProbesTotal: this.ownerCt.numberOfProbesTotal,
				geneSymbol : this.applicationRoot._imageArea._geneLabels.labels[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]],
				geneId : this.applicationRoot._imageArea._heatmapArea.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]],
				geneFullName : this.applicationRoot.visualizationData.geneFullNames[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]
			});
		}, this);

		this.el.on('mouseover', function(e, t) {
			var index = this.__calculateIndexFromXY(e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());
			if (this._isExpanded) {
				// this.applicationRoot._rotatedLabelsBox._drawTopLabels (
				// this._datasetGroupIndex, this._columnGroupIndex,
				// this._columnIndex, index.column );
			} else {
				// this.applicationRoot._rotatedLabelsBox._drawTopLabels (
				// this._datasetGroupIndex, this._columnGroupIndex,
				// this._columnIndex );
			}
		}, this);

		this.el.on('mouseout', function(e, t) {
			if (this._isExpanded) {
				this._drawContrasts(false);
			} else {
				this._drawHeatmapColumn(false);
			}
				// this.applicationRoot._imageArea._geneLabels.highlightGene (
				// this.rowGroup, -1 );
				// this.applicationRoot._rotatedLabelsBox._drawTopLabels (
				// this._datasetGroupIndex );
			}, this);
	}

});

Ext.reg('metaVizColumn', Gemma.MetaHeatmapColumn);

// COLUMN ( visualization for each row group

/**
 * 
 * @class Gemma.MetaHeatmapExpandableColumn
 * @extends Ext.Panel
 */
Gemma.MetaHeatmapExpandableColumn = Ext.extend(Ext.Panel, {
	initComponent : function() {
		Ext.apply(this, {
			applicationRoot : this.applicationRoot, // reference to the root
			// panel of the application

			border : false,
			bodyBorder : false,

			width : Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth,

			_dataColumn : this.dataColumn,
			_numberOfRowGroups : this.dataColumn.visualizationValues.length,
			_columnIndex : this.columnIndex,
			_columnGroupIndex : this.columnGroupIndex,
			_datasetGroupIndex : this.datasetGroupIndex,
			_columnHidden : false,

			miniPieValue : (this.dataColumn.numberOfProbesTotal === 0)? -1: 360.0 * this.dataColumn.numberOfProbesDiffExpressed / this.dataColumn.numberOfProbesTotal,
			sumOfPvalues : 0.0,

			factorValueIds : this.dataColumn.contrastsFactoreValueIds,
			factorId : this.dataColumn.factorId,
			factorName : this.dataColumn.factorName,
			factorCategory : this.dataColumn.factorCategory,
			factorDescription : this.dataColumn.factorDescription,
			factorValueNames : this.dataColumn.contrastsFactorValues,
			baselineFactorValue : this.dataColumn.baselineFactorValue,
			baselineFactorValueId : this.dataColumn.baselineFactorValueId,

			contrastsFoldChanges : this.dataColumn.constrastsFoldChangeValues,

			updateAllParentContainersWidthBy : function(delta) {
				this.ownerCt.changePanelWidthBy(delta);
			},

			_visualizationColumns : [],
			missingValuesScore : null,

			datasetName : null,
			datasetId : null,
			analysisId : null,
			analysisType : null,

			drawExpandButton : function(ctx, color) {
				ctx.fillStyle = color;
				ctx.moveTo(8, 8);
				ctx.beginPath();
				ctx.lineTo(8, 1);
				ctx.lineTo(1, 8);
				ctx.lineTo(8, 8);
				ctx.fill();
			},

			drawCollapseButton : function(ctx, width, color) {
				width = width - 5;
				ctx.strokeStyle = color;
				ctx.moveTo(1, 8);
				ctx.beginPath();

				ctx.lineTo(3, 6);
				ctx.lineTo(1, 4);
				ctx.lineTo(1, 8);

				ctx.moveTo(2, 6);
				ctx.lineTo(width + 1, 6);
				ctx.moveTo(width + 2, 8);

				ctx.lineTo(width + 2, 4);
				ctx.lineTo(width, 6);
				ctx.lineTo(width + 2, 8);
				ctx.stroke();
			},

			layout : 'vbox',
			items : [{
				xtype : 'button',
				ref : '_expandButton',
				enableToggle : true,
				height : 10,
				width : 10,
				template : new Ext.Template('<div id="{1}"><canvas {0}></canvas></div>'),
				buttonSelector : 'canvas:first-child',
				getTemplateArgs : function() {
					return [this.cls, this.id];
				},
				listeners : {
					toggle : function(target, checked) {
						var ctx;
						var doResize;
						var geneGroupSubColumnIndex;

						if (checked) {
							doResize = true;
							for (geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {
								this._visualizationColumns[geneGroupSubColumnIndex]._drawContrasts(doResize);
								this._visualizationColumns[geneGroupSubColumnIndex]._isExpanded = true;
								doResize = false;
							}

							var width = this.getWidth();
							ctx = this._expandButton.btnEl.dom.getContext("2d");
							this._expandButton.setWidth(width);
							ctx.canvas.width = width;
							ctx.clearRect(0, 0, width, 10);
							this.drawCollapseButton(ctx, width, 'rgba(10,100,10, 0.8)');

						} else {
							ctx = this._expandButton.btnEl.dom.getContext("2d");
							this._expandButton.setWidth(10);
							ctx.canvas.width = 10;
							ctx.clearRect(0, 0, 10, 10);
							this.drawExpandButton(ctx, 'rgba(10,100,10, 0.8)');

							doResize = true;
							for (geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {
								this._visualizationColumns[geneGroupSubColumnIndex]._drawHeatmapColumn(doResize);
								this._visualizationColumns[geneGroupSubColumnIndex]._isExpanded = false;
								doResize = false;
							}
						}
						this.applicationRoot._imageArea.topLabelsPanel._drawTopLabels();
						this.applicationRoot._imageArea._heatmapArea.doLayout();
					},
					scope : this
				}
			}]
		});

		Gemma.MetaHeatmapExpandableColumn.superclass.initComponent.apply(this, arguments);
	},

	onRender : function() {
		Gemma.MetaHeatmapExpandableColumn.superclass.onRender.apply(this, arguments);

		for (var geneGroupIndex = 0; geneGroupIndex < this._numberOfRowGroups; geneGroupIndex++) {
			var subColumn = new Gemma.MetaHeatmapColumn({
						applicationRoot : this.applicationRoot,
						visualizationSubColumnData : this._dataColumn.visualizationValues[geneGroupIndex],
						pValuesSubColumnData : this._dataColumn.pValues[geneGroupIndex],
						// visualizationContrastsSubColumnData:
						// this._dataColumn.contrastsVisualizationValues [
						// geneGroupIndex ],
						factorValues : this._factorValueNames,
						// contrastsFoldChanges: this.contrastsFoldChanges[
						// geneGroupIndex ],
						rowGroup : geneGroupIndex,
						columnIndex : this._columnIndex,
						columnGroupIndex : this._columnGroupIndex,
						datasetGroupIndex : this._datasetGroupIndex
					});

			this._visualizationColumns.push(subColumn);
			this.add(subColumn);
			if(this.lastColumnInGroup){
				
				var spacerColumn = new Gemma.MetaHeatmapSpacerColumn({
						applicationRoot : this.applicationRoot,
						visualizationSubColumnData : this._dataColumn.visualizationValues[geneGroupIndex],
						pValuesSubColumnData : this._dataColumn.pValues[geneGroupIndex],
						factorValues : this._factorValueNames,
						rowGroup : geneGroupIndex,
						columnIndex : this._columnIndex,
						columnGroupIndex : this._columnGroupIndex,
						datasetGroupIndex : this._datasetGroupIndex
					});
					
				this.add(spacerColumn);
			}
		}

		this.overallDifferentialExpressionScore = 0;
		this.missingValuesScore = 0;
		for (var i = 0; i < this._dataColumn.visualizationValues.length; i++) {
			for (var j = 0; j < this._dataColumn.visualizationValues[i].length; j++) {
				if (this._dataColumn.visualizationValues[i][j] === null) {
					this.missingValuesScore++;
					this.overallDifferentialExpressionScore += 0;
				} else {
					this.overallDifferentialExpressionScore += this._dataColumn.visualizationValues[i][j];
				}
			}
		}

		this.ownerCt.overallDifferentialExpressionScore += this.overallDifferentialExpressionScore;
		this.ownerCt.ownerCt.overallDifferentialExpressionScore += this.overallDifferentialExpressionScore;

		this.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.specificityScore);
		this.ownerCt.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.ownerCt.specificityScore);

		this.el.on('mouseover', function(e, t) {
					var ctx = this._expandButton.btnEl.dom.getContext("2d");
					if (this._expandButton.pressed) {
						this.drawCollapseButton(ctx, this.getWidth(), 'rgb(255,10,10)');
					} else {
						this.drawExpandButton(ctx, 'rgb(255,10,10)');
					}
				}, this);
		this.el.on('mouseout', function(e, t) {
					var ctx = this._expandButton.btnEl.dom.getContext("2d");
					if (this._expandButton.pressed) {
						this.drawCollapseButton(ctx, this.getWidth(), 'rgb(10,100,10)');
					} else {
						this.drawExpandButton(ctx, 'rgb(10,100,10)');
					}
				}, this);
	},

	updateParentsScores : function() {
		this.ownerCt.overallDifferentialExpressionScore -= this.overallDifferentialExpressionScore;
		this.ownerCt.ownerCt.overallDifferentialExpressionScore -= this.overallDifferentialExpressionScore;

		// this.ownerCt.specificityScore = Math.max(this.miniPieValue,
		// this.ownerCt.specificityScore);
		// this.ownerCt.ownerCt.specificityScore = Math.max(this.miniPieValue,
		// this.ownerCt.ownerCt.specificityScore);
	},

	refresh : function() {
		var ctx = this._expandButton.btnEl.dom.getContext("2d");
		this._expandButton.setWidth(10);
		ctx.canvas.width = 10;
		this.drawExpandButton(ctx);

		for (var geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {
			this._visualizationColumns[geneGroupSubColumnIndex]._drawHeatmapColumn(false);
		}
	}
});


/**
 * SPACER COLUMN (goes between groups and indicates the selected rows)
 * @class Gemma.MetaHeatmapExpandableColumn
 * @extends Ext.Panel
 */
Gemma.MetaHeatmapSpacerColumn = Ext.extend(Ext.Panel, {
	initComponent : function() {
		
		Ext.apply(this, {
					autoEl : {
						tag : 'canvas',
						width : Gemma.MetaVisualizationConfig.groupSeparatorWidth,
						height : Gemma.MetaVisualizationConfig.cellHeight * this.visualizationSubColumnData.length
					},
					margins : {
						top : 0,
						right : 0,
						bottom : Gemma.MetaVisualizationConfig.groupSeparatorHeight,
						left : 0
					},

					applicationRoot : this.applicationRoot,

					cellHeight : Gemma.MetaVisualizationConfig.cellHeight, 
					cellWidth : Gemma.MetaVisualizationConfig.cellWidth, 

					geneGroupIndex : this.rowGroup, // gene group index
					columnIndex : this.columnIndex, // index within analysis panel

				});
		Gemma.MetaHeatmapSpacerColumn.superclass.initComponent.apply(this, arguments);
	},

	/**
	 * 
	 * @param {}
	 *            doResize
	 * @param {}
	 *            highlightRow
	 */
	_drawHeatmapColumn : function(doResize, highlightRow) {

		var ctx = this.el.dom.getContext("2d");
		var oldWidth = ctx.canvas.width;
		var newWidth = Gemma.MetaVisualizationConfig.groupSeparatorWidth;
		ctx.canvas.width = newWidth;
		ctx.clearRect(0, 0, this.el.dom.width, this.el.dom.height);

		for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupIndex].length; i++) {
			var geneId = this.applicationRoot.visualizationData.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][i]];
			if (this.applicationRoot._selectedGenes.indexOf(geneId) != -1) {
				this._drawHeatmapSelectedRowCell(ctx, i, 0);
				
			}
		}

	},

	/**
	 * 
	 * @param {}
	 *            ctx
	 * @param {}
	 *            rowIndex
	 * @param {}
	 *            columnIndex
	 */
	_drawHeatmapSelectedRowCell : function(ctx, rowIndex, columnIndex) {
		ctx.save();
		ctx.strokeStyle = Gemma.MetaVisualizationConfig.rowCellSelectColor;
		ctx.strokeRect(0, this.cellHeight * rowIndex, Gemma.MetaVisualizationConfig.groupSeparatorWidth, this.cellHeight);
		ctx.restore();
	},

	/**
	 * 
	 * @param {}
	 *            x
	 * @param {}
	 *            y
	 * @return {}
	 */
	__calculateIndexFromXY : function(x, y) {
		var row = Math.floor(y / this.cellHeight);
		var column = Math.floor(x / this.cellWidth);
		return {
			'row' : row,
			'column' : column
		};
	},

	/**
	 * @Override
	 */
	onRender : function() {

		Gemma.MetaHeatmapSpacerColumn.superclass.onRender.apply(this, arguments);
		
		this._drawHeatmapColumn();

	}

});

/**
 * 
 * @class Gemma.MetaHeatmapExpandButton
 * @extends Ext.Button
 */
Gemma.MetaHeatmapExpandButton = Ext.extend(Ext.Button, {
			initComponent : function() {
				Ext.apply(this, {});
				Gemma.MetaHeatmapExpandButton.superclass.initComponent.apply(this, arguments);
			},
			onRender : function() {
				Gemma.MetaHeatmapExpandButton.superclass.onRender.apply(this, arguments);

			}
		});
