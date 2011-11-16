Ext.namespace('Gemma.Metaheatmap');

Gemma.Metaheatmap.HeatmapBox = Ext.extend ( Ext.Panel, {
	initComponent : function () {		
		Ext.apply (this, {
				border 		: false,
				//bodyBorder 	: false,
	       		autoScroll	: true,
	       		isGeneOnTop : false,
				isShowPvalue : true,
	       		conditionTree 	: this.conditionTree,
				geneTree	: this.geneTree,
				cells	: this.cells,
				lastCellMouseIn : null,
				items: [{
					xtype  : 'box',
					autoEl : 'canvas',
					ref    : 'boxCanvas'
				}]								
		});

		Gemma.Metaheatmap.HeatmapBox.superclass.initComponent.apply (this, arguments);
	},
		
	initializeMe : function () {
		
		// when we say 'owner container' here, we mean the visualisation panel, so travel up the container stack until we get to it
		// (this is alternative to this.ownerCt which is dangerous to use because it will break if there are any layout changes made)
		var ownerCt = this.findParentByType('Metaheatmap.VisualizationPanel');
		
		this.addEvents('cell_mouse_in','cell_mouse_out','cell_click');
		
		this.ctx = Gemma.Metaheatmap.Utils.getCanvasContext (this.boxCanvas.el.dom);
		
		this.boxCanvas.el.on('mousemove', function(e, t) {	
			document.body.style.cursor = 'pointer'; // makes it really slow
			if (this.lastCellMouseIn !== null) {
				this.lastCellMouseIn.draw (this.ctx);
				this.fireEvent ('cell_mouse_out',this.lastCellMouseIn,e,t);
			}
			var x = e.getPageX() - Ext.get(t).getX();
			var y = e.getPageY() - Ext.get(t).getY();
			var cell = this.getCellByXY(x,y);			
			if (cell !== null) {
				cell.highlight (this.ctx);
				this.fireEvent ('cell_mouse_in',cell,e,t);
			}
			this.lastCellMouseIn = cell;			
		}, this);				

		this.boxCanvas.el.on('mouseout', function(e, t) {	
			document.body.style.cursor = 'default'; // makes it really slow
			if (this.lastCellMouseIn !== null) {
				this.lastCellMouseIn.draw (this.ctx);
				this.fireEvent ('cell_mouse_out',this.lastCellMouseIn,e,t);
			}
			this.lastCellMouseIn = null;			
		}, this);
		
		this.body.on('scroll',function (e, t) {						
			var scroll = this.body.getScroll();
			ownerCt.variableWidthCol.boxTopLabels.body.scrollTo ('left',scroll.left);
			ownerCt.fixedWidthCol.boxSideLabels.body.scrollTo ('top',scroll.top);			
		}, this);
		
		this.boxCanvas.el.on('click', function(e, t) {
			var x = e.getPageX() - Ext.get(t).getX();
			var y = e.getPageY() - Ext.get(t).getY();
			
			var cell = this.getCellByXY (x,y);
			
			this.fireEvent('cell_click',cell);
			
		}, this);				
		
		this.amIinitialized = true;
	},
	
	resizeAndPosition : function () {
		if (!this.amIinitialized) {
			this.initializeMe();		
		}
		
		// when we say 'owner container' here, we mean the visualisation panel, so travel up the container stack until we get to it
		// (this is alternative to this.ownerCt which is dangerous to use because it will break if there are any layout nesting changes made)
		var ownerCt = this.findParentByType('Metaheatmap.VisualizationPanel');

		//this.setPosition (ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.height, ownerCt.variableWidthCol.boxTopLabels.tree.display.size.height);		
		
		var headerHeight = ownerCt.variableWidthCol.boxTopLabels.tree.display.size.height;
		var sideWidth    = ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.height;
		
		this.setWidth (ownerCt.getWidth() - sideWidth - 20);
		this.setHeight (ownerCt.getHeight() - headerHeight - 20);							
		
		this.boxCanvas.setWidth (ownerCt.variableWidthCol.boxTopLabels.tree.display.size.width);
		this.boxCanvas.setHeight (ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.width);							
		
		this.ctx.canvas.width  = ownerCt.variableWidthCol.boxTopLabels.tree.display.size.width;
		this.ctx.canvas.height = ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.width;
		this.doLayout();
	},
	
	draw : function () {
		if (!this.amIinitialized) {
			this.initializeMe();		
		}
				
		this.lastCellMouseIn = null;
		
		this.ctx.clearRect (0, 0, this.ctx.canvas.width, this.ctx.canvas.height);		
		
		for (var i = 0; i < this.geneTree.items.length; i++) {
			var gene = this.geneTree.items[i];
			for (var j = 0; j < this.conditionTree.items.length; j++) {
				var condition = this.conditionTree.items[j];
				this.drawCell (gene, condition);											
			}
		}			
	},
	
	getCellByXY : function (x,y) {
		var gene, condition;
		
		if (this.isGeneOnTop) {
			gene 	  = this.geneTree.findItemByCoordinate (x);
			condition = this.conditionTree.findItemByCoordinate (y);			
		} else {
			gene 	  = this.geneTree.findItemByCoordinate (y);
			condition = this.conditionTree.findItemByCoordinate (x);			
		}
		if ( !gene || !condition || gene === null || condition === null) return null;
		
		var pValue = 1;
		var foldChange, isProbeMissing;
		var isGeneOnTop = this.isGeneOnTop;
		var cell  = this.cells.getCell (gene, condition); 		
		var color = this.getVisualizationValue (gene, condition);
		
		
		if (cell !== null ) {			
			pValue = cell.pValue;
			foldChange = cell.logFoldChange;
			isProbeMissing = cell.isProbeMissing;
		} 
		
		var x, y, width, height;
		
		if (this.isGeneOnTop) {
			x = gene.display.pxlStart;
			y = condition.display.pxlStart;
			width  = gene.display.pxlSize;
			height = condition.display.pxlSize;
		} else {
			x = condition.display.pxlStart;
			y = gene.display.pxlStart;
			width  = condition.display.pxlSize;
			height = gene.display.pxlSize;
		}
		var drawCellFunction = this.drawCell_;
		return {
			'gene' 		 : gene,
			'condition'  : condition,
			'pValue' 	 : pValue,
			'foldChange' : foldChange,
			'visualizationValue' : color,
			
			drawCell_ : drawCellFunction,
			
			highlight : function (ctx) {
				this.drawCell_ (ctx, gene, condition, color, isProbeMissing, isGeneOnTop);				

				if (width > 4 && height > 4) {
					ctx.strokeStyle = 'black';
					ctx.strokeRect (x + 0.5, y + 0.5, width - 1, height - 1);
				} else {
					ctx.fillStyle = 'black';
					ctx.fillRect (x, y, width, height);					
				}
			},
			
			draw : function (ctx) {
				this.drawCell_ (ctx, gene, condition, color, isProbeMissing, isGeneOnTop);				
//				ctx.clearRect (x, y, width, height);
//				ctx.fillStyle = color;
//				ctx.fillRect (x, y, width, height);
			}			
		};
	},

	//TODO: remove
//	getCellData : function (gene, condition) {
//		var geneToCellMap = this.cellData[condition.id];
//		
//		if (typeof geneToCellMap != 'undefined') {
//			var cellValueObj = geneToCellMap[gene.id];
//			if (typeof cellValueObj != 'undefined') {
//				return cellValueObj;			
//			}
//		}
//		
//		return null;
//	},
	
	getVisualizationValue : function (gene, condition) {
		var color;
		var cellData = this.cells.getCell (gene,condition);
		if (cellData === null) {
			// Contrast not stored. Assuming high pValue.
			color = 'black';			
		} else {
			if (cellData.isProbeMissing) {
				 color = Gemma.Metaheatmap.Config.basicColourRange.getCellColorString (null); // Gray cell.				
			} else {
				if (this.isShowPvalue) {
					 color = Gemma.Metaheatmap.Config.basicColourRange.getCellColorString (						
							 this.calculateVisualizationValueBasedOnPvalue (cellData.pValue) );
				} else {					
					color = Gemma.Metaheatmap.Config.contrastsColourRange.getCellColorString (cellData.logFoldChange);
				}
			}
		}		

		return color;
	},
		
    calculateVisualizationValueBasedOnFoldChange : function ( foldChange ) {
        var visualizationValue = 0.0;
        var absFoldChange = Math.abs(foldChange - 1);
        if (absFoldChange >= 0 && absFoldChange < 0.05) visualizationValue = 0.1;                
        else if (absFoldChange >= 0.05 && absFoldChange < 0.1 ) visualizationValue = 0.2;                                 
        else if (absFoldChange >= 0.1 && absFoldChange < 0.2 ) visualizationValue = 0.3; 
        else if (absFoldChange >= 0.2 && absFoldChange < 0.3 ) visualizationValue = 0.4; 
        else if (absFoldChange >= 0.3 && absFoldChange < 0.4 ) visualizationValue = 0.5;
        else if (absFoldChange >= 0.4 && absFoldChange < 0.5 ) visualizationValue = 0.6;
        else if (absFoldChange >= 0.5 && absFoldChange < 0.6 ) visualizationValue = 0.7;
        else if (absFoldChange >= 0.6 && absFoldChange < 0.7 ) visualizationValue = 0.8;
        else if (absFoldChange >= 0.7 && absFoldChange < 0.8 ) visualizationValue = 0.9;
        else if (absFoldChange >= 0.8) visualizationValue = 1;

        if (foldChange < 0) visualizationValue = (-1)*visualizationValue;
        return visualizationValue;
    },

    calculateVisualizationValueBasedOnPvalue : function ( pValue ) {
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
            visualizationValue = 6;
        else if ( pValue < 0.001 && pValue >= 0.0001 )
            visualizationValue = 8;
        else if ( pValue < 0.0001 && pValue >= 0.00001 )
            visualizationValue = 9;
        else if ( pValue < 0.00001 ) visualizationValue = 10;
        return visualizationValue;
    },
	
	drawCell : function (gene, condition) {
		var color = this.getVisualizationValue (gene, condition);		
		this.ctx.fillStyle = color;		
		
		var cellData = this.cells.getCell (gene,condition);		
		if (cellData !== null && cellData.isProbeMissing) {
			if (this.isGeneOnTop) {
				this.ctx.fillRect (gene.display.pxlStart + gene.display.pxlSize/2, condition.display.pxlStart + condition.display.pxlSize/2, 1, 1);			
			} else {
				this.ctx.fillRect (condition.display.pxlStart + condition.display.pxlSize/2, gene.display.pxlStart + gene.display.pxlSize/2, 1, 1);						
			}			
		} else {
			if (this.isGeneOnTop) {
				this.ctx.fillRect (gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize, condition.display.pxlSize);			
			} else {
				this.ctx.fillRect (condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize, gene.display.pxlSize);						
			}
		}
	},

	drawCell_ : function (ctx, gene, condition, color, isProbeMissing, isGeneOnTop) {
		ctx.fillStyle = color;		
		
		if (isProbeMissing) {
			if (isGeneOnTop) {
				ctx.clearRect (gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize, condition.display.pxlSize);
				ctx.fillRect (gene.display.pxlStart + gene.display.pxlSize/2, condition.display.pxlStart + condition.display.pxlSize/2, 1, 1);			
			} else {
				ctx.clearRect (condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize, gene.display.pxlSize);
				ctx.fillRect (condition.display.pxlStart + condition.display.pxlSize/2, gene.display.pxlStart + gene.display.pxlSize/2, 1, 1);						
			}			
		} else {
			if (isGeneOnTop) {
				ctx.fillRect (gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize, condition.display.pxlSize);			
			} else {
				ctx.fillRect (condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize, gene.display.pxlSize);						
			}
		}
	},		
		
	onRender : function() {
		Gemma.Metaheatmap.HeatmapBox.superclass.onRender.apply (this, arguments);		
		
		this.setSize (this.heatmapMaxWidth, this.heatmapMaxHeight);

		this.amIinitialized = false;
	}

});
Ext.reg ('Metaheatmap.HeatmapBox', Gemma.Metaheatmap.HeatmapBox);


/*columnFilters : {},

addFilterFunction : function (filterId, filterFunction) {
	columnFilters[filterId] = filterFunction;
},

removeFilterFunction : function (filterId) {
	delete columnFilters[filterId];
},

applyFilters : function () {
	for (var filter in columnFilters) {
		this.items.each(function() {this.filterColumns(filteringFn);});							
	}												
},

filterColumns : function(filteringFn) {
	var count = 0;
	this.items.each(function() {
				count = count + this.filterColumns(filteringFn);
			});
	return count;
},*/
//_filterRows : function(filteringFn) {
//
//					},
//
//					_sortColumns : function(asc_desc, sortingFn) {
//						this.items.each(function() {
//									this.items.sort(asc_desc, sortingFn);
//								});
//					}

//var i;
//var initialWidth = 0;
//for (i = 0; i < this.dataDatasetGroups.length; i++) {
//	if (this.dataDatasetGroups[i].length > 0) {
//		var dsGroupPanel = new Gemma.MetaHeatmapDatasetGroupPanel({
//			applicationRoot : this.applicationRoot,
//			height : this.height,
//			dataFactorColumns : this.dataDatasetGroups[i],
//			datasetGroupIndex : i,
//			geneNames : this.geneNames[i],
//			geneIds : this.geneIds[i]
//		});				
//		this.add(dsGroupPanel);
//		initialWidth += dsGroupPanel.width + Gemma.MetaVisualizationConfig.groupSeparatorWidth;
//	}			
//}
//this.setWidth(initialWidth);

