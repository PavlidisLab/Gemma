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
			if (this.lastCellMouseIn !== null) {
				this.lastCellMouseIn.draw (this.ctx);
				this.fireEvent ('cell_mouse_out',this.lastCellMouseIn,e,t);
			}
			var x = e.getPageX() - Ext.get(t).getX();
			var y = e.getPageY() - Ext.get(t).getY();
			var cell = this.getCellByXY(x,y);	
			// don't show "click-me" finger over cells that have no data
			if (cell && !cell.isProbeMissing) {
				document.body.style.cursor = 'pointer'; // makes it really slow?
			} else {
				document.body.style.cursor = 'default'; // makes it really slow?
			}
					
			if (cell !== null) {
				cell.highlight (this.ctx);
				this.fireEvent ('cell_mouse_in',cell,e,t);
			}
			this.lastCellMouseIn = cell;			
		}, this);				

		this.boxCanvas.el.on('mouseout', function(e, t) {	
			document.body.style.cursor = 'default'; // makes it really slow?
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
			
			// don't do anything if you click on a cell with no data
			if (cell && !cell.isProbeMissing) {
				this.fireEvent('cell_click',cell);
			}
			
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
			'isProbeMissing' : isProbeMissing,
			
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
			}			
		};
	},
	
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
		
    calculateVisualizationValueBasedOnPvalue : function ( pValue ) {
        var visualizationValue = 0;
        if ( pValue < 0.5 && pValue >= 0.25 )
            visualizationValue = 0.5;
        else if ( pValue < 0.25 && pValue >= 0.1 )
            visualizationValue = 1;
        else if ( pValue < 0.1 && pValue >= 0.05 )
            visualizationValue = 2;
        else if ( pValue < 0.05 && pValue >= 0.01 )
            visualizationValue = 3;
        else if ( pValue < 0.01 && pValue >= 0.001 )
            visualizationValue = 4;
        else if ( pValue < 0.001 && pValue >= 0.0001 )
            visualizationValue = 7;
        else if ( pValue < 0.0001 && pValue >= 0.00001 )
            visualizationValue = 9;
        else if ( pValue < 0.00001 ) visualizationValue = 10;
        return visualizationValue;
    },
	
	drawCell : function (gene, condition) {
		var color = this.getVisualizationValue (gene, condition);		
		var cellData = this.cells.getCell (gene,condition);		
		
		this.drawCell_( this.ctx, gene, condition, color, cellData.isProbeMissing, this.isGeneOnTop )		
	},

	drawCell_ : function (ctx, gene, condition, color, isProbeMissing, isGeneOnTop) {
		if (isProbeMissing) {
			if (isGeneOnTop) {
				ctx.clearRect (gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize, condition.display.pxlSize);
				ctx.fillStyle = 'white';
				ctx.fillRect (gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize, condition.display.pxlSize);
				
				ctx.fillStyle = color;		
				ctx.fillRect (gene.display.pxlStart + gene.display.pxlSize/2 - 1, condition.display.pxlStart + condition.display.pxlSize/2 - 1, 2, 2);			
			} else {
				ctx.clearRect (condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize, gene.display.pxlSize);
				ctx.fillStyle = 'white';
				ctx.fillRect (condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize, gene.display.pxlSize);

				ctx.fillStyle = color;		
				ctx.fillRect (condition.display.pxlStart + condition.display.pxlSize/2 - 1, gene.display.pxlStart + gene.display.pxlSize/2 - 1, 2, 2);						
			}			
		} else {
			ctx.fillStyle = color;		
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