Ext.namespace('Gemma.Metaheatmap');
/**
 * Summary
 * 
 * 
 * Public methods: + draw() + redraw() + + +
 * 
 * Configuration: + + + +
 * 
 * Events: + +
 */
Gemma.Metaheatmap.VisualizationPanel = Ext.extend ( Ext.Panel, {
	// need to use hbox so the heatmap box resizes when the control panel is collapsed
	layout : 'hbox',
	layoutConfig: {
		align: 'stretch',
		pack: 'start'
	},
	frame: false,
	border: false,
	defaults: {
		border: false
	},
	initComponent : function () {
		
		Ext.apply (this, {
			isLegendShown: false,
			//autoScroll: false,
			geneTree: this.geneTree,
			conditionTree: this.conditionTree,
			cells: this.cells,
			items: [{
				xtype: 'panel',
				name: 'fixedWidthCol',
				ref: 'fixedWidthCol',
				flex: 0,
				width: 135,
				layout: 'vbox',
				layoutConfig: {
					align: 'stretch',
					pack: 'start'
				},
				defaults: {
					border: false
				},
				items: [{
					xtype: 'panel',
					ref: 'pnlMiniControl',
					autoScroll:true,
					// html : '<span style="color:dimGrey;font-size:0.9em;line-height:1.6em"><b>Hover</b> for quick info<br>'+
					//   '<b>Click</b> for details<br><b>"ctrl" + click</b> to select genes</span>',
					border: false,
					defaults: {
						border: false,
						style:'margin-left:10px;margin-top:5px;'
					},
					items: [{
						xtype: 'label',
						text: 'Column zoom'
					}, {
						xtype: 'slider',
						ref: 'sldHorizontalZoom',
						width: 80,
						height: 20,
						value: Gemma.Metaheatmap.defaultConditionZoom,
						increment: 1,
						minValue: 2,
						maxValue: 15,
						listeners: {
							changecomplete: function(slider, newValue, thumb){
								this.redraw();
							//this.fireEvent("horizontal_zoom_change", newValue);
							},
							scope: this
						}
					}, {
						xtype: 'label',
						text: 'Row zoom'
					}, {
						xtype: 'slider',
						ref: 'sldVerticalZoom',
						//vertical : true,
						width: 80,
						height: 20,
						value: Gemma.Metaheatmap.defaultGeneZoom,
						increment: 1,
						minValue: 2,
						maxValue: 15,
						listeners: {
							changecomplete: function(slider, newValue, thumb){
								this.redraw();
							//this.fireEvent("vertical_zoom_change", newValue);
							},
							scope: this
						}
					}, {
						xtype: 'button',
						text: 'Dock popup',
						disabled : false,
						hidden : false,
						enableToggle: true,
						width: 80,
						toggleHandler: function(btn, toggle){
							if (toggle) {
								this.hoverWindow.isDocked = true;
								this.hoverWindow.setTitle('Docked popup: click + drag to move');
								//this.hoverWindow.setPagePosition ( 800, 100);
								btn.setText("Undock popup");
							} else {
								this.hoverWindow.isDocked = false;
								this.hoverWindow.setTitle('');
								
								btn.setText("Dock popup");
							}
						},
						scope: this
					}, {
						xtype: 'button',
						text: 'Show fold change',
						width: 95,
						handler: function(btn, e){
							this.variableWidthCol.boxHeatmap.isShowPvalue = !this.variableWidthCol.boxHeatmap.isShowPvalue;
							
							if (this.variableWidthCol.boxHeatmap.isShowPvalue) {
								btn.setText("Show fold change");
								if (this.isLegendShown) {
									this.variableWidthCol.colorLegendFoldChange.hide();
									this.variableWidthCol.colorLegendPvalue.show();
								}
							} else {
								btn.setText("Show p-value");
								if (this.isLegendShown) {
									this.variableWidthCol.colorLegendFoldChange.show();
									this.variableWidthCol.colorLegendPvalue.hide();
								}
							}
							this.variableWidthCol.boxHeatmap.draw();
						},
						scope: this
					}, {
						xtype: 'button',
						text: 'Flip axes',
						disabled : false,
						hidden : false,
						width: 80,
						handler: function(btn, e){
							this.flipLabels();
							this.redraw();
						},
						scope: this
					}]
				}, {
					xtype: 'Metaheatmap.LabelBox',
					ref: 'boxSideLabels',
					border: false,
					orientation: 'vertical',
					tree: this.geneTree,
					drawItemLabel_: this.drawGeneLabel,
					onClick: this.onClickGeneLabel
				}]
			}, {
				xtype: 'panel',
				name: 'variableWidthCol',
				ref: 'variableWidthCol',
				flex:1,
				layout:'vbox',
				layoutConfig: {
					align: 'stretch',
					pack: 'start'
				},
				defaults: {
					border: false
				},
				items: [{
					xtype: 'Metaheatmap.LabelBox',
					ref: 'boxTopLabels',
					border: false,
					orientation: 'horizontal',
					tree: this.conditionTree,
					drawItemLabel_: this.drawConditionLabel,
					onClick: this.onClickConditionLabel
				}, {
					xtype: 'Metaheatmap.HeatmapBox',
					ref: 'boxHeatmap',
					border: false,
					geneTree: this.geneTree,
					conditionTree: this.conditionTree,
					cells: this.cells
				}, {
					xtype: 'Metaheatmap.ColorLegend',
					ref: 'colorLegendPvalue',
					constrain: true,
					x: 440,
					y: 0,
					title: 'P-value',
					cellSize: 10,
					scaleLabels: ["No Data", "1~0.1", "0.05", "0.01", "0.001", "0.0001", "0.00001", "< 0.00001"],
					scaleValues: [null, 0, 2, 3, 5, 8, 9, 10],
					
					discreteColorRangeObject: Gemma.Metaheatmap.Config.basicColourRange,
					orientation: 'vertical',
					fontSize: 12
				}, {
					xtype: 'Metaheatmap.ColorLegend',
					ref: 'colorLegendFoldChange',
					constrain: true,
					x: 440,
					y: 0,
					title: 'Log fold change',
					cellSize: 10,
					
					scaleLabels: ["No Data", "-3", "-2", "-1", "0", "1", "2", "3"],
					scaleValues: [null, -3, -2, -1, 0, 1, 2, 3],
					
					discreteColorRangeObject: Gemma.Metaheatmap.Config.contrastsColourRange,
					orientation: 'vertical',
					fontSize: 12
				}]
			}]
		});

		Gemma.Metaheatmap.VisualizationPanel.superclass.initComponent.apply (this, arguments);		

	},
		
	setGeneTree : function (tree) {
		this.geneTree = tree;
		this.variableWidthCol.boxHeatmap.geneTree = tree;
		if (this.isGeneOnTop) {
			this.variableWidthCol.boxTopLabels.tree = tree;						
		} else {
			this.fixedWidthCol.boxSideLabels.tree = tree;						
		}		
	},
	
	setConditionTree : function (tree) {
		this.conditionTree = tree;
		this.variableWidthCol.boxHeatmap.conditionTree = tree;
		if (this.isGeneOnTop) {
			this.fixedWidthCol.boxSideLabels.tree = tree;						
		} else {
			this.variableWidthCol.boxTopLabels.tree = tree;						
		}						
	},
	
	flipLabels : function () {
  	    if (!this.variableWidthCol.boxHeatmap.isGeneOnTop) {
      	    this.variableWidthCol.boxHeatmap.isGeneOnTop = true;
					
      	    this.fixedWidthCol.boxSideLabels.tree = this.conditionTree;       		 							
      	    this.fixedWidthCol.boxSideLabels.drawItemLabel_ = this.drawConditionLabel;       		           	   
      	    this.fixedWidthCol.boxSideLabels.onClick = this.onClickConditionLabel;  
					
      	    this.variableWidthCol.boxTopLabels.tree = this.geneTree;
      	    this.variableWidthCol.boxTopLabels.drawItemLabel_ = this.drawGeneLabel;       		           	   
      	    this.variableWidthCol.boxTopLabels.onClick = this.onClickGeneLabel;   
			
		    		       		           	           		       		           	    
  	    } else {
  	    	this.variableWidthCol.boxHeatmap.isGeneOnTop = false;
  	    	
  	    	this.fixedWidthCol.boxSideLabels.tree = this.geneTree;
  	    	this.fixedWidthCol.boxSideLabels.drawItemLabel_ = this.drawGeneLabel;       		           	   
  	    	this.fixedWidthCol.boxSideLabels.onClick = this.onClickGeneLabel;     		           	   
					
  	    	this.variableWidthCol.boxTopLabels.tree = this.conditionTree;
  	    	this.variableWidthCol.boxTopLabels.drawItemLabel_ = this.drawConditionLabel;       		           	   
  	    	this.variableWidthCol.boxTopLabels.onClick = this.onClickConditionLabel;
			
  	    }		
		// update size of top left control panel so that gene labels line up with data rows 
		this.updatePnlMiniControlSize();
	},	
	
	/**
	 * update size of top left control panel so that side labels line up with data rows 
	 */
	updatePnlMiniControlSize: function(){
		
		// update size of top left control panel so that gene labels line up with data rows 
		var topPadding = 0; // TODO get actual top padding from somewhere
		this.fixedWidthCol.pnlMiniControl.setHeight(this.variableWidthCol.boxTopLabels.getHeight() + topPadding);
	},
	
	redraw : function () {
  	    if (this.variableWidthCol.boxHeatmap.isGeneOnTop) {
      	    this.conditionTree.applyZoom (this.fixedWidthCol.pnlMiniControl.sldVerticalZoom.getValue());
      	    this.geneTree.applyZoom (this.fixedWidthCol.pnlMiniControl.sldHorizontalZoom.getValue());
  	    } else {
  	    	this.conditionTree.applyZoom (this.fixedWidthCol.pnlMiniControl.sldHorizontalZoom.getValue());
  	    	this.geneTree.applyZoom (this.fixedWidthCol.pnlMiniControl.sldVerticalZoom.getValue());
  	    }
  	    
		this.fixedWidthCol.boxSideLabels.resizeAndPosition();
		this.variableWidthCol.boxTopLabels.resizeAndPosition();
		this.variableWidthCol.boxHeatmap.resizeAndPosition();
		
		// update size of top left control panel so that gene labels line up with data rows 
		this.updatePnlMiniControlSize();

		this.updateVisibleScores(); // TODO: do this only if filtering options
									// have changed
		
		this.doLayout();
		
		this.draw();
	},
		
	draw : function () {		
		this.variableWidthCol.boxHeatmap.draw();
		this.fixedWidthCol.boxSideLabels.draw();
		this.variableWidthCol.boxTopLabels.draw();		
	},

	onClickGeneLabel : function (label,e ) {
		// If user held down ctrl while clicking, select column or gene instead
		// of popping up window.
		if (e.ctrlKey === true) {
			if (label.item.isSelected) {
				label.item.isSelected = false;
			} else {
				label.item.isSelected = true;				
			}
			this.draw();
		} else {
			Gemma.MetaVisualizationPopups.makeGeneInfoWindow(label.item.name, label.item.id);	
		}
	},
	
	onClickConditionLabel : function (label, e) {
		// If user held down ctrl while clicking, select column or gene instead
		// of popping up window.
		if (e.ctrlKey === true) {
			if (label.item.isSelected) {
				label.item.isSelected = false;
			} else {
				label.item.isSelected = true;				
			}
			this.draw();
		} else {
			Gemma.MetaVisualizationPopups.makeDatasetInfoWindow(label.item.datasetName, label.item.datasetShortName, label.item.datasetId);       		           		   
		}
	},
	
	downloadImage : function () {
		var ctxMain = this.variableWidthCol.boxHeatmap.ctx;
		var ctxSide = this.fixedWidthCol.boxSideLabels.ctx;
		var ctxTop = this.variableWidthCol.boxTopLabels.ctx;
		
		// create temporaray canvas element
		var canvas = document.createElement('canvas');
		canvas.width = ctxMain.canvas.width + 1 + ctxSide.canvas.width;
		canvas.height = ctxMain.canvas.height + 1 + ctxTop.canvas.height;
		var ctx = canvas.getContext('2d');

		var topImage  =  ctxTop.getImageData(0, 0, ctxTop.canvas.width, ctxTop.canvas.height);
		var sideImage = ctxSide.getImageData(0, 0, ctxSide.canvas.width, ctxSide.canvas.height);
		var mainImage = ctxMain.getImageData(0, 0, ctxMain.canvas.width, ctxMain.canvas.height);

		ctx.drawImage (ctxTop.canvas, ctxSide.canvas.width+1, 0);
		ctx.drawImage (ctxSide.canvas, 0, ctxTop.canvas.height+1);
		ctx.drawImage (ctxMain.canvas, ctxSide.canvas.width+1, ctxTop.canvas.height+1);
		
		var image_url = canvas.toDataURL("image/png");
		var win = new Ext.Window({
		    html: '<img src="'+image_url+'" />',
		    height: 700,
		    widht: 900
		});
		win.show();
	},	
	
	
	drawGeneLabel : function (gene, size, orientation, backgroundColor) {
		gene.miniPieValue = null;
		
		var maxSize = {
			'width' : this.getWidth(),
			'height' : this.getHeight()
		};
		
		gene.display.label = {};
		gene.display.label.drawFn = Gemma.Metaheatmap.ConditionLabel.constructDrawLabelFunction (this.ctx, gene, gene.name, orientation, size, maxSize, backgroundColor) ; 
		gene.display.label.drawFn (false);
	},
	
	drawConditionLabel : function (condition, size, orientation, backgroundColor) {

		condition.miniPieValue = (condition.numberOfProbesOnArray === 0) ? -1 : 360.0 * condition.numberDiffExpressedProbes / condition.numberOfProbesOnArray;
		condition.miniBarValue = (condition.numberOfProbesOnArray === 0) ? 0 : 9.0 * (condition.numberDiffExpressedProbes / condition.numberOfProbesOnArray);

		var maxSize = {
			'width' : this.getWidth(),
			'height' : this.getHeight()
		};
		
		condition.display.label = {};
		condition.display.label.drawFn = Gemma.Metaheatmap.ConditionLabel.constructDrawLabelFunction (this.ctx, condition, condition.contrastFactorValue, orientation, size, maxSize, backgroundColor) ; 
		condition.display.label.drawFn (false);
	},

	// Some gene/condition scores depend on what is currently visible: % of
	// missing values, inverse of p values sum
	// Other scores are not affected by it: specificity
	updateVisibleScores : function () {
		var i, j;
		// Calculate visible scores for conditons
		for (i = 0; i < this.conditionTree.items.length; i++) {
			var condition = this.conditionTree.items[i];
			var sumPvalue = 0;
			var numProbesMissing = 0;
			for (j = 0; j < this.geneTree.items.length; j++) {				
				var gene = this.geneTree.items[j];
				var cell = this.cells.getCell (gene,condition);
				if (cell !== null) {
					if (cell.isProbeMissing) {
						numProbesMissing++;
					} else {
						if (cell.pValue !== null) {
							sumPvalue += 1 - cell.pValue;
						}
					}
				}
			}
			condition.inverseSumPvalue = sumPvalue / (this.geneTree.items.length - numProbesMissing);
			condition.percentProbesMissing = numProbesMissing / this.geneTree.items.length;			
		}	

		// Calculate visible scores for genes
		for (j = 0; j < this.geneTree.items.length; j++) {
			var gene = this.geneTree.items[j];
			var sumPvalue = 0;
			var numProbesMissing = 0;
			for (i = 0; i < this.conditionTree.items.length; i++) {
				var condition = this.conditionTree.items[i];
				var cell = this.cells.getCell (gene,condition);
				if (cell !== null) {
					if (cell.isProbeMissing)  {
						numProbesMissing++;
					} else {
						sumPvalue += 1 - cell.pValue;						
					}
				}				
			}
			gene.inverseSumPvalue =  sumPvalue / (this.conditionTree.items.length - numProbesMissing);
			gene.percentProbesMissing = numProbesMissing / this.conditionTree.items.length;			
		}			
	},	

	constructHoverWindowContent : function (type, item) {
		var msg;
		if (type === 'gene') {
			msg = {
					type 	     : 'gene',
					geneSymbol   : item.name,
					geneId 		 : item.id,
					geneFullName : item.fullName
			};
		} else if (type === 'condition') {
			var specificity = Gemma.Metaheatmap.Utils.formatPercent(item.numberDiffExpressedProbes, item.numberOfProbesOnArray, 2);
			msg = {
					type 			  : 'condition',
					factorCategory	  : item.factorCategory,
					factorName 		  : item.factorName,				
					factorDescription : item.factorDescription,
					datasetName 	  : item.datasetName,
					datasetShortName 	  : item.datasetShortName,
					
					specificityPercent  : specificity,
					numberDiffExpressedProbes : item.numberDiffExpressedProbes,
					numberOfProbesOnArray     : item.numberOfProbesOnArray
			};
		} else {
			msg = {
					type 			  : 'cell',
					factorCategory	  : item.condition.factorCategory,
					factorName 		  : item.condition.factorName,				
					factorDescription : item.condition.factorDescription,
					datasetName 	  : item.condition.datasetName,
					datasetShortName  : item.condition.datasetShortName,

					geneSymbol   : item.gene.name,
					geneId 		 : item.gene.id,
					geneFullName : item.gene.fullName,

					pvalue : item.pValue,
					foldChange : item.foldChange
			};
		}
		return msg;
	},
	
	getSelectedGenes : function () {
		var geneIds = [];
		for (var i = 0; i < this.geneTree.items.length; i++) {
			var gene = this.geneTree.items[i];
			if (gene.isSelected) {
				geneIds.push (gene.id);
			}
		}
		return geneIds;
	},
	
	onRender: function() {
		Gemma.Metaheatmap.VisualizationPanel.superclass.onRender.apply ( this, arguments );
		
		this.hoverWindow = new Gemma.Metaheatmap.HoverWindow();
		this.hoverWindow.isDocked = false;
		this.hoverWindow.hide();
		
		this.addEvents ('gene_zoom_change', 'condition_zoom_change');
		
		this.variableWidthCol.boxTopLabels.on('label_mouse_in', function (label,e,t) {		
			var labelType = this.variableWidthCol.boxHeatmap.isGeneOnTop ?  'gene' : 'condition';			
			var msg = this.constructHoverWindowContent (labelType, label.item);
			this.hoverWindow.show();
			if (!this.hoverWindow.isDocked) {
				this.hoverWindow.setPagePosition ( e.getPageX() + 20, e.getPageY() + 20 );
			}
			this.hoverWindow.update (msg);		
		}, this );

		this.fixedWidthCol.boxSideLabels.on('label_mouse_in', function (label,e,t) {		
			var labelType = this.variableWidthCol.boxHeatmap.isGeneOnTop ?  'condition' : 'gene';			
			var msg = this.constructHoverWindowContent (labelType, label.item);
			this.hoverWindow.show();
			if (!this.hoverWindow.isDocked) {
				this.hoverWindow.setPagePosition ( e.getPageX() + 20, e.getPageY() + 20 );
			}
			this.hoverWindow.update (msg);		
		}, this );
		
		this.variableWidthCol.boxTopLabels.on('label_mouse_out', function (label,e,t) {		
			if (this.hoverWindow && !this.hoverWindow.isDocked) {
				this.hoverWindow.hide();
			}			
		}, this );

		this.fixedWidthCol.boxSideLabels.on('label_mouse_out', function (label,e,t) {		
			if (this.hoverWindow && !this.hoverWindow.isDocked) {
				this.hoverWindow.hide();
			}	
		}, this );
		
		
		this.variableWidthCol.boxHeatmap.on('cell_mouse_in', function (cell,e,t) {		
			var msg = this.constructHoverWindowContent ('cell', cell);

			this.hoverWindow.show();
			if (!this.hoverWindow.isDocked) {
				this.hoverWindow.setPagePosition ( e.getPageX() + 20, e.getPageY() + 20 );
			}
			this.hoverWindow.update (msg);
		
			cell.gene.display.label.drawFn (true);
			cell.condition.display.label.drawFn (true);
		}, this );
		
		this.variableWidthCol.boxHeatmap.on ('cell_mouse_out', function (cell,e,t) {
			cell.gene.display.label.drawFn(false);
			cell.condition.display.label.drawFn(false);
			if (!this.hoverWindow.isDocked) {
				this.hoverWindow.hide();
			}
		}, this);
		
		this.variableWidthCol.boxHeatmap.on ('cell_click', function (cell) {
			if (cell !== null) {
				var expressionDetailsWindow = new Gemma.VisualizationWithThumbsWindow ({
					title : 'Gene Expression',
					thumbnails : false,
					downloadLink: String.format ("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1}", cell.condition.datasetId, cell.gene.id)
				});
				expressionDetailsWindow.show ({
					params : [ [cell.condition.datasetId], [cell.gene.id] ]
				});
			}
		});
		
	}

});

Ext.reg('Metaheatmap.VisualizationPanel', Gemma.Metaheatmap.VisualizationPanel);
