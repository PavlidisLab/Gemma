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
				xtype : 'Metaheatmap.HoverWindow',
				ref : 'hoverWindow'
			    },{
				xtype: 'panel',
				name: 'fixedWidthCol',
				ref: 'fixedWidthCol',
				flex: 0,
				//width: 135,
				layout: 'vbox',
				layoutConfig: {
					align: 'stretch',
					pack: 'start'
				},
				defaults: {
					border: false
				},
				items: [{
					xtype : 'panel',
					ref : 'pnlMiniControl',
					autoScroll : true,
					// html : '<span style="color:dimGrey;font-size:0.9em;line-height:1.6em"><b>Hover</b> for quick info<br>'+
					//   '<b>Click</b> for details<br><b>"ctrl" + click</b> to select genes</span>',
					border : false,
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
						width: 95,
						toggleHandler: function(btn, toggle){
							if (toggle) {
								this.hoverWindow.isDocked = true;
								this.hoverWindow.setTitle('Docked popup: click & drag to move');
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
						ref: 'showFoldChangeToggle',
						text: 'Hide p-value',
						width: 95,
						handler: function(btn, e){
							this.variableWidthCol.boxHeatmap.isShowPvalue = !this.variableWidthCol.boxHeatmap.isShowPvalue;
							
							if (this.variableWidthCol.boxHeatmap.isShowPvalue) {
								btn.setText("Hide p-value");
//								if (this.isLegendShown) {
//									this.variableWidthCol.colorLegendFoldChange.hide();
//									this.variableWidthCol.colorLegendPvalue.show();
//								}
							} else {
								btn.setText("Show p-value");
//								if (this.isLegendShown) {
//									this.variableWidthCol.colorLegendFoldChange.show();
//									this.variableWidthCol.colorLegendPvalue.hide();
//								}
							}
							this.variableWidthCol.boxHeatmap.draw();
						},
						scope: this
					}, {
						xtype: 'button',
						text: 'Flip axes',
						disabled : true,
						hidden : true,
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
				}, 
				{
					xtype: 'Metaheatmap.ColorLegend',
					ref: 'colorLegend',
					x: 200,
					y: 0,
					title: 'Log fold change & p-value',
					cellSize: 10,
					
					foldChangeLabels: ["No Data", "-3", "-2", "-1", "0", "1", "2", "3"],
					foldChangeValues: [null, -3, -2, -1, 0, 1, 2, 3],
					
					discreteColorRangeObject: Gemma.Metaheatmap.Config.contrastsColourRange,
					
					pValueLabels: ["1~0.1", "0.05", "0.01", "0.001", "0.0001", "0.00001", "< 0.00001"],
					pValueValues: [0, 2, 3, 5, 8, 9, 10],
					
					fontSize: 12
				}
				]
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
		// need "Math.max(140, ...)" so that panel doesn't disappear when all columns are filtered out
		this.fixedWidthCol.pnlMiniControl.setHeight(Math.max(140, this.variableWidthCol.boxTopLabels.getHeight()) + topPadding);
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

		//this.updateVisibleScores(); // TODO: do this only if filtering options
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
		
		// create temporary canvas element
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
			title : 'Right-click the image and save the image as file.',
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
	// TODO: this needs to be rethought! i didn't think it through well enough
	updateVisibleScores : function () {
		var i, j;
		var numOverThresholdInSet=0, numProbesInSet=0;
		// Calculate visible scores for conditons
		for (i = 0; i < this.conditionTree.items.length; i++) {
			var condition = this.conditionTree.items[i];
			condition.experimentSpecificity = condition.numberDiffExpressedProbes / condition.numberOfProbesOnArray;			
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
							numProbesInSet += cell.numberOfProbes;
							numOverThresholdInSet += cell.numberOfProbesDiffExpressed;
						}
					}
				}
			}
//			condition.inverseSumPvalue = sumPvalue / (this.geneTree.items.length - numProbesMissing);
			condition.inverseSumPvalue = GemmaStatUtils.computeOraPvalue( condition.numberOfProbesOnArray, numProbesInSet, numOverThresholdInSet, condition.numberDiffExpressedProbes );
			condition.ora = condition.inverseSumPvalue;

			condition.percentProbesMissing = numProbesMissing / this.geneTree.items.length;
		}	

		// Calculate visible scores for genes
		for (j = 0; j < this.geneTree.items.length; j++) {
			gene = this.geneTree.items[j];			
			var pValues = [];
			numProbesMissing = 0;
			for (i = 0; i < this.conditionTree.items.length; i++) {
				condition = this.conditionTree.items[i];
				cell = this.cells.getCell (gene,condition);
				if (cell !== null) {
					if (cell.isProbeMissing)  {
						numProbesMissing++;
					} else {
						pValues.push(cell.pValue);
						//sumPvalue += 1 - cell.pValue;						
					}
				}				
			}
			//gene.inverseSumPvalue = sumPvalue / (this.conditionTree.items.length - numProbesMissing);
			gene.inverseSumPvalue = GemmaStatUtils.computeMetaPvalue (pValues);
			gene.metaPvalue = gene.inverseSumPvalue;
			gene.percentProbesMissing = numProbesMissing / this.conditionTree.items.length;
			
			gene.metaPvalueBarChart = this.calculateBarChartValueBasedOnPvalue (gene.metaPvalue);
			
		}			
	},	

    calculateBarChartValueBasedOnPvalue : function ( pValue ) {
        var visualizationValue = 0;
        if ( pValue < 0.5 && pValue >= 0.25 )
            visualizationValue = 1;
        else if ( pValue < 0.25 && pValue >= 0.1 )
            visualizationValue = 2;
        else if ( pValue < 0.1 && pValue >= 0.05 )
            visualizationValue = 3;
        else if ( pValue < 0.05 && pValue >= 0.01 )
            visualizationValue = 4;
        else if ( pValue < 0.01 && pValue >= 0.001 )
            visualizationValue = 5;
        else if ( pValue < 0.001 && pValue >= 0.0001 )
            visualizationValue = 6;
        else if ( pValue < 0.0001 && pValue >= 0.00001 )
            visualizationValue = 7;
        else if ( pValue < 0.00001 ) visualizationValue = 8;
        return visualizationValue;
    },
	
	
	constructHoverWindowContent : function (type, item) {
		var msg;
		if (type === 'gene') {
			msg = {
					type 	     : 'gene',
					geneSymbol   : item.name,
					geneId 		 : item.id,
					geneFullName : item.fullName,
					geneMetaPvalue :  item.metaPvalue,
			};
		} else if (type === 'condition') {
			var specificity = Gemma.Metaheatmap.Utils.formatPercent(item.numberDiffExpressedProbes, item.numberOfProbesOnArray, 2);
			msg = {
					type 			  : 'condition',
					factorCategory	  : item.factorCategory,
					factorName 		  : item.factorName,				
					factorDescription : item.factorDescription,
					datasetName 	  : item.datasetName,
					datasetShortName  : item.datasetShortName,
					
					contrastFactorValue	: item.contrastFactorValue,
					baselineFactorValue : item.baselineFactorValue,				
					
					ora					: item.ora,
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

					contrastFactorValue	: item.condition.contrastFactorValue,
					baselineFactorValue : item.condition.baselineFactorValue,				

					geneSymbol   : item.gene.name,
					geneId 		 : item.gene.id,
					geneFullName : item.gene.fullName,

					numberOfProbes : item.numberOfProbes,
					numberOfProbesDiffExpressed : item.numberOfProbesDiffExpressed,
					
					pvalue : (item.isProbeMissing)?'No data':item.pValue,
					foldChange : (item.isProbeMissing)?'No data':item.foldChange
			};
		}
		return msg;
	},
	
	getSelectedGeneIds : function () {
		var geneIds = [];
		for (var i = 0; i < this.geneTree.items.length; i++) {
			var gene = this.geneTree.items[i];
			if (gene.isSelected) {
				geneIds.push (gene.id);
			}
		}
		return geneIds;
	},
		
	getSelectedDatasetIds : function () {
		var dsIds = [];
		for (var i = 0; i < this.conditionTree.items.length; i++) {
			var condition = this.conditionTree.items[i];
			if (condition.isSelected) {
				dsIds.push (condition.datasetId);
			}
		}
		return dsIds;
	},
	
	
	afterRender : function () {
		Gemma.Metaheatmap.VisualizationPanel.superclass.afterRender.apply ( this, arguments );

		this.mask = new Ext.LoadMask(this.getEl(), {msg:"Filtering..."});

		this.hoverWindow.isDocked = false;
		this.hoverWindow.hide();
	},
	
	onRender: function() {
		Gemma.Metaheatmap.VisualizationPanel.superclass.onRender.apply ( this, arguments );

		//this.hoverWindow = new Gemma.Metaheatmap.HoverWindow();
		
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
					closableAction : 'hide',
					downloadLink: String.format ("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1}", cell.condition.datasetId, cell.gene.id)
				});
				expressionDetailsWindow.show ({
					params : [ [cell.condition.datasetId], [cell.gene.id] ]
				});
				var xy = expressionDetailsWindow.getPosition();
				expressionDetailsWindow.setPosition (xy[0] + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20, xy[1] + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20);
				Gemma.MetaVisualizationPopups.cascadeLayoutCounter++;
				if (Gemma.MetaVisualizationPopups.cascadeLayoutCounter > 4) {
					Gemma.MetaVisualizationPopups.cascadeLayoutCounter = 0;
				}

				
			}
		});
	}

});

Ext.reg('Metaheatmap.VisualizationPanel', Gemma.Metaheatmap.VisualizationPanel);


//TODO: Move me to a separate file

GemmaStatUtils = {};
GemmaStatUtils.Constants = {};

GemmaStatUtils.Constants.MACHEP = 1.11022302462515654042E-16;
GemmaStatUtils.Constants.MAXLOG = 7.09782712893383996732E2;
GemmaStatUtils.Constants.MINLOG = -7.451332191019412076235E2;
GemmaStatUtils.Constants.MAXGAM = 171.624376956302725;
GemmaStatUtils.Constants.SQTPI = 2.50662827463100050242E0;
GemmaStatUtils.Constants.SQRTH = 7.07106781186547524401E-1;
GemmaStatUtils.Constants.LOGPI = 1.14472988584940017414;

GemmaStatUtils.Constants.BIG = 4.503599627370496e15;
GemmaStatUtils.Constants.BIG_INVERSE = 2.22044604925031308085e-16;

/**
 * Fisher's method for combining p values. (Cooper and Hedges 15-8)
 * 
 * @param pValues
 * @return double upper tail
 */
GemmaStatUtils.computeMetaPvalue = function (pValues) {
        var metaPvalue = 0.0;
        for (var i = 0; i < pValues.length; i++ ) {
        	metaPvalue += Math.log( pValues[i] );
        }
        metaPvalue *= -2.0;
        // NOTE: dof is first argument.
        return GemmaStatUtils.chiSquareComplemented( 2.0 * pValues.length, metaPvalue );    
};

GemmaStatUtils.chiSquareComplemented = function (v, x) {
  if (x < 0.0 || v < 1.0) {
    return 0.0;
  }
  return GemmaStatUtils.incompleteGammaComplement (v / 2.0, x / 2.0);
};

/**
 * Returns the Complemented Incomplete Gamma function; formerly named <tt>igamc</tt>.
 *
 * @param alpha the shape parameter of the gamma distribution.
 * @param x the integration start point.
 */
GemmaStatUtils.incompleteGammaComplement = function (alpha, x) {

  if (x <= 0 || alpha <= 0) {
    return 1.0;
  }

  if (x < 1.0 || x < alpha) {
    return 1.0 - GemmaStatUtils.incompleteGamma(alpha, x);
  }

  var ax = alpha * Math.log(x) - x - GemmaStatUtils.logGamma(alpha);
  if (ax < -GemmaStatUtils.Constants.MAXLOG) {
    return 0.0;
  }

  ax = Math.exp(ax);

  /* continued fraction */
  var y = 1.0 - alpha;
  var z = x + y + 1.0;
  var c = 0.0;
  var pkm2 = 1.0;
  var qkm2 = x;
  var pkm1 = x + 1.0;
  var qkm1 = z * x;
  var ans = pkm1 / qkm1;

  var t;
  do {
    c += 1.0;
    y += 1.0;
    z += 2.0;
    var yc = y * c;
    var pk = pkm1 * z - pkm2 * yc;
    var qk = qkm1 * z - qkm2 * yc;
    if (qk !== 0) {
      var r = pk / qk;
      t = Math.abs((ans - r) / r);
      ans = r;
    } else {
      t = 1.0;
    }

    pkm2 = pkm1;
    pkm1 = pk;
    qkm2 = qkm1;
    qkm1 = qk;
    if (Math.abs(pk) > GemmaStatUtils.Constants.BIG) {
      pkm2 *= GemmaStatUtils.Constants.BIG_INVERSE;
      pkm1 *= GemmaStatUtils.Constants.BIG_INVERSE;
      qkm2 *= GemmaStatUtils.Constants.BIG_INVERSE;
      qkm1 *= GemmaStatUtils.Constants.BIG_INVERSE;
    }
  } while (t > GemmaStatUtils.Constants.MACHEP);

  return ans * ax;
};

/** Returns the natural logarithm of the gamma function; formerly named <tt>lgamma</tt>. */
GemmaStatUtils.logGamma = function (x) {
  var p;
  var q;
  var z;

  var aCoefficient = [
    8.11614167470508450300E-4,
    -5.95061904284301438324E-4,
    7.93650340457716943945E-4,
    -2.77777777730099687205E-3,
    8.33333333333331927722E-2
  ];
  var bCoefficient = [
    -1.37825152569120859100E3,
    -3.88016315134637840924E4,
    -3.31612992738871184744E5,
    -1.16237097492762307383E6,
    -1.72173700820839662146E6,
    -8.53555664245765465627E5
  ];
  var cCoefficient = [
    /* 1.00000000000000000000E0, */
    -3.51815701436523470549E2,
    -1.70642106651881159223E4,
    -2.20528590553854454839E5,
    -1.13933444367982507207E6,
    -2.53252307177582951285E6,
    -2.01889141433532773231E6
  ];

  if (x < -34.0) {
    q = -x;
    var w = GemmaStatUtils.logGamma(q);
    p = Math.floor(q);
    if (p === q) {
      throw "ArithmeticException::Overflow";
    }
    z = q - p;
    if (z > 0.5) {
      p += 1.0;
      z = p - q;
    }
    z = q * Math.sin(Math.PI * z);
    if (z === 0.0) {
      throw "ArithmeticException::Overflow";
    }
    z = GemmaStatUtils.Constants.LOGPI - Math.log(z) - w;
    return z;
  }

  if (x < 13.0) {
    z = 1.0;
    while (x >= 3.0) {
      x -= 1.0;
      z *= x;
    }
    while (x < 2.0) {
      if (x === 0.0) {
        throw "ArithmeticException::Overflow";
      }
      z /= x;
      x += 1.0;
    }
    if (z < 0.0) {
      z = -z;
    }
    if (x == 2.0) {
      return Math.log(z);
    }
    x -= 2.0;
    p = x * GemmaStatUtils.polevl(x, bCoefficient, 5) / GemmaStatUtils.p1evl(x, cCoefficient, 6);
    return Math.log(z) + p;
  }

  if (x > 2.556348e305) {
    throw "ArithmeticException::Overflow";
  }

  q = (x - 0.5) * Math.log(x) - x + 0.91893853320467274178;
  //if( x > 1.0e8 ) return( q );
  if (x > 1.0e8) {
    return q;
  }

  p = 1.0 / (x * x);
  if (x >= 1000.0) {
    q += ((7.9365079365079365079365e-4 * p - 2.7777777777777777777778e-3) * p + 0.0833333333333333333333) / x;
  } else {
    q += GemmaStatUtils.polevl(p, aCoefficient, 4) / x;
  }
  return q;
};

/**
 * Returns the Incomplete Gamma function; formerly named <tt>igamma</tt>.
 *
 * @param alpha the shape parameter of the gamma distribution.
 * @param x the integration end point.
 * @return The value of the unnormalized incomplete gamma function.
 */
GemmaStatUtils.incompleteGamma = function (alpha, x) {
  if (x <= 0 || alpha <= 0) {
    return 0.0;
  }

  if (x > 1.0 && x > alpha) {
    return 1.0 - GemmaStatUtils.incompleteGammaComplement(alpha, x);
  }

  /* Compute  x**a * exp(-x) / gamma(a)  */
  var ax = alpha * Math.log(x) - x - GemmaStatUtils.logGamma(alpha);
  if (ax < -GemmaStatUtils.Constants.MAXLOG) {
    return 0.0;
  }

  ax = Math.exp(ax);

  /* power series */
  var r = alpha;
  var c = 1.0;
  var ans = 1.0;

  do {
    r += 1.0;
    c *= x / r;
    ans += c;
  }
  while (c / ans > GemmaStatUtils.Constants.MACHEP);

  return ans * ax / alpha;

};

GemmaStatUtils.polevl = function (x, coef, N) {
  var ans = coef[0];

  for (var i = 1; i <= N; i++) {
    ans = ans * x + coef[i];
  }
 
  return ans;
};

GemmaStatUtils.computeOraPvalue = function ( numGenesTotal, numGenesInSet, numOverThresholdInSet, numOverThresholdTotal ) {

    var oraPvalue = Number.NaN;

    if ( numOverThresholdInSet > 0 ) {
    	oraPvalue = 0.0;
        // sum probs of N or more successes up to max possible.
        for ( var i = numOverThresholdInSet; i <= Math.min( numOverThresholdTotal, numGenesInSet ); i++ ) {
            oraPvalue += GemmaStatUtils.dhyper( i, numGenesInSet, numGenesTotal - numGenesInSet, numOverThresholdTotal );
        }

        if ( isNaN( oraPvalue ) ) {
            // binomial approximation
            var pos_prob = numGenesInSet / numGenesTotal;
            oraPvalue = 0.0;
            for ( var i = numOverThresholdInSet; i <= Math.min( numOverThresholdTotal, numGenesInSet ); i++ ) {
                oraPvalue += GemmaStatUtils.dbinom( i, numOverThresholdTotal, pos_prob );
            }
        }
    } else {
        oraPvalue = 1.0;
    }
    
    return oraPvalue;
};

GemmaStatUtils.dhyper = function (x, r, b, n) {
	var p, q, p1, p2, p3;

	if ( r < 0 || b < 0 || n < 0 || n > r + b ) { throw "IllegalArgument"; }

	if ( x < 0 ) {return 0.0;}

	if ( n < x || r < x || n - x > b ) {return 0;}
	if ( n === 0 ) {return ( ( x === 0 ) ? 1 : 0 );}

	p = n / ( r + b );
	q = ( r + b - n ) / ( r + b );

	p1 = GemmaStatUtils.dbinom_raw( x, r, p, q );
	p2 = GemmaStatUtils.dbinom_raw( n - x, b, p, q );
	p3 = GemmaStatUtils.dbinom_raw( n, r + b, p, q );

	return p1 * p2 / p3;
};

GemmaStatUtils.dbinom_raw = function (x, n, p, q) {
    var f, lc;

    if ( p === 0 ) {return ( ( x === 0 ) ? 1 : 0 );}
    if ( q === 0 ) {return ( ( x === n ) ? 1 : 0 );}

    if ( x === 0 ) {
    	    if ( n === 0 ) {return 1;}
        lc = ( p < 0.1 ) ? -GemmaStatUtils.bd0( n, n * q ) - n * p : n * Math.log( q );
        return ( Math.exp( lc ) );
    }
    if ( x == n ) {
        lc = ( q < 0.1 ) ? -GemmaStatUtils.bd0( n, n * p ) - n * q : n * Math.log( p );
        return ( Math.exp( lc ) );
    }
    if ( x < 0 || x > n ) {return ( 0 );}

    lc = GemmaStatUtils.stirlerr( n ) - GemmaStatUtils.stirlerr( x ) - GemmaStatUtils.stirlerr( n - x ) - GemmaStatUtils.bd0( x, n * p ) - GemmaStatUtils.bd0( n - x, n * q );
    f = ( 2 * Math.PI * x * ( n - x ) ) / n;

    return Math.exp( lc ) / Math.sqrt( f );
};

GemmaStatUtils.stirlerr = function ( n ) {

    var S0 = 0.083333333333333333333; /* 1/12 */
    var S1 = 0.00277777777777777777778; /* 1/360 */
    var S2 = 0.00079365079365079365079365; /* 1/1260 */
    var S3 = 0.000595238095238095238095238;/* 1/1680 */
    var S4 = 0.0008417508417508417508417508;/* 1/1188 */

    /*
     * error for 0, 0.5, 1.0, 1.5, ..., 14.5, 15.0.
     */
    sferr_halves = [0.0, //new double[] { 0.0, /* n=0 - wrong, place holder only */
    0.1534264097200273452913848, /* 0.5 */
    0.0810614667953272582196702, /* 1.0 */
    0.0548141210519176538961390, /* 1.5 */
    0.0413406959554092940938221, /* 2.0 */
    0.03316287351993628748511048, /* 2.5 */
    0.02767792568499833914878929, /* 3.0 */
    0.02374616365629749597132920, /* 3.5 */
    0.02079067210376509311152277, /* 4.0 */
    0.01848845053267318523077934, /* 4.5 */
    0.01664469118982119216319487, /* 5.0 */
    0.01513497322191737887351255, /* 5.5 */
    0.01387612882307074799874573, /* 6.0 */
    0.01281046524292022692424986, /* 6.5 */
    0.01189670994589177009505572, /* 7.0 */
    0.01110455975820691732662991, /* 7.5 */
    0.010411265261972096497478567, /* 8.0 */
    0.009799416126158803298389475, /* 8.5 */
    0.009255462182712732917728637, /* 9.0 */
    0.008768700134139385462952823, /* 9.5 */
    0.008330563433362871256469318, /* 10.0 */
    0.007934114564314020547248100, /* 10.5 */
    0.007573675487951840794972024, /* 11.0 */
    0.007244554301320383179543912, /* 11.5 */
    0.006942840107209529865664152, /* 12.0 */
    0.006665247032707682442354394, /* 12.5 */
    0.006408994188004207068439631, /* 13.0 */
    0.006171712263039457647532867, /* 13.5 */
    0.005951370112758847735624416, /* 14.0 */
    0.005746216513010115682023589, /* 14.5 */
    0.005554733551962801371038690
    /* 15.0 */
    ];

    var nn;

    if ( n <= 15.0 ) {
        nn = n + n;
        if ( nn == Math.round(nn) ) {return ( sferr_halves[Math.round(nn)] );}
        return ( GemmaStatUtils.logGamma( n + 1.0 ) - ( n + 0.5 ) * Math.log( n ) + n - GemmaStatUtils.Constants.M_LN_SQRT_2PI );
    }

    nn = n * n;
    if ( n > 500 ) {return ( ( S0 - S1 / nn ) / n );}
    if ( n > 80 ) {return ( ( S0 - ( S1 - S2 / nn ) / nn ) / n );}
    if ( n > 35 ) {return ( ( S0 - ( S1 - ( S2 - S3 / nn ) / nn ) / nn ) / n );}
    /* 15 < n <= 35 : */
    return ( ( S0 - ( S1 - ( S2 - ( S3 - S4 / nn ) / nn ) / nn ) / nn ) / n );
};

/**
 * Ported from bd0.c in R source.
 * <p>
 * Evaluates the "deviance part"
 * 
 * <pre>
 *    bd0(x,M) :=  M * D0(x/M) = M*[ x/M * log(x/M) + 1 - (x/M) ] =
 *         =  x * log(x/M) + M - x
 * </pre>
 * 
 * where M = E[X] = n*p (or = lambda), for x, M &gt; 0
 * <p>
 * in a manner that should be stable (with small relative error) for all x and np. In particular for x/np close to
 * 1, direct evaluation fails, and evaluation is based on the Taylor series of log((1+v)/(1-v)) with v =
 * (x-np)/(x+np).
 * 
 * @param x
 * @param np
 * @return
 */
GemmaStatUtils.bd0 = function (x, np) {
    var ej, s, s1, v;
    var j;

    if ( Math.abs( x - np ) < 0.1 * ( x + np ) ) {
        v = ( x - np ) / ( x + np );
        s = ( x - np ) * v;/* s using v -- change by MM */
        ej = 2 * x * v;
        v = v * v;
        for ( j = 1;; j++ ) { /* Taylor series */
            ej *= v;
            s1 = s + ej / ( ( j << 1 ) + 1 );
            if ( s1 == s ) /* last term was effectively 0 */
            	    {return ( s1 );}
            s = s1;
        }
    }
    /* else: | x - np | is not too small */
    return ( x * Math.log( x / np ) + np - x );
};

GemmaStatUtils.dbinom = function (x, n, p) {

    if ( p < 0 || p > 1 || n < 0 ) throw "IllegalArgumentException";

    return GemmaStatUtils.dbinom_raw( x, n, p, 1 - p );
}

