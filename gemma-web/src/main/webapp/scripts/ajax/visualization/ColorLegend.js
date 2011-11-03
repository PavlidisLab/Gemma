Ext.namespace('Gemma.Metaheatmap');

Gemma.Metaheatmap.ColorLegend = Ext.extend ( Ext.Window, {
	
	height : 160,
	width  : 100,

	closeAction : 'hide',
	closable    : false,
	shadow 	    : false,
	border 	    : false,
	bodyBorder  : false,
	margins     : {
		top    : 0,
		right  : 0,
		bottom : 0,
		left   : 0
	},    
	
    initComponent: function () {        
		
    	Ext.apply (this, {
    		cellSize	: this.cellSize, 
    		orientation : this.orientation,
    		scaleLabels	: this.scaleLabels,
    		scaleValues : this.scaleValues,
    		
    		discreteColorRange : this.discreteColorRangeObject,

    		fontSize : this.fontSize,
    		
			items: [{				
				xtype  : 'box',
				autoEl : 'canvas',
				ref    : 'boxCanvas',
				listeners : {
					afterrender: {
						scope : this,
						fn : function() {
							this.ctx = Gemma.Metaheatmap.Utils.getCanvasContext (this.boxCanvas.el.dom);								
							this.ctx.canvas.height = this.height;
							this.ctx.canvas.width  = this.width;
							this.drawVertical ();
						}
					}								
				}				
			}]
    	}); 
				     
        Gemma.Metaheatmap.ColorLegend.superclass.initComponent.apply (this, arguments);
    },
    
	
	drawVertical : function () {
		this.ctx.font = this.fontSize + "px sans-serif";
		var y = 1;				
		for (var i = 0; i < this.scaleLabels.length; i++) {
			
			if (this.scaleLabels[i] === "No Data") {
				this.drawMissingData_ (1, y, this.cellSize);
			} else {
				var color = this.discreteColorRange.getCellColorString (this.scaleValues[i]);
				this.drawCell_ (1, y, this.cellSize, color);
			}
			
			this.ctx.fillStyle = "black";
			this.ctx.fillText (this.scaleLabels[i], this.cellSize + 3, y + this.cellSize);
			
			y += this.cellSize;
		}				
	},
	
	drawMissingData_ : function (x, y, size) {
		this.ctx.fillStyle = 'gray';				
		this.ctx.fillRect (x + size / 2, y + size / 2, 1, 1);				
	},
	
	drawCell_ : function (x, y, size, color) {
		this.ctx.fillStyle = color;				
		this.ctx.fillRect (x, y, size, size);			
	}		

});

Ext.reg('Metaheatmap.ColorLegend', Gemma.Metaheatmap.ColorLegend);

//var heightInit = this.cellHeight + this.textWidthMax + this.textOffset;
//Ext.apply(this, {
//
//	height: heightInit*2 + 35,
//	
//	width: this.cellWidth * this.colorValues.length + 25,
//	
//	_offset: this.textOffset,
//	
//	items: [{
//	
//		xtype: 'box',
//		autoEl: 'canvas',
//		id: this.canvasId,
//		listeners: {
//			afterrender: {
//				scope: this,
//				fn: function(){
//					this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId));
//					this._ctx.canvas.height = heightInit;
//					this._ctx.canvas.width = widthInit;
//					this.drawHorizontal(this._discreteColorRange, this._colorValues, this._title);
//				}
//			}
//		
//		
//		}
//	
//	
//	},
//	{
//	
//		xtype: 'box',
//		autoEl: 'canvas',
//		id: this.canvasId2,
//		listeners: {
//			afterrender: {
//				scope: this,
//				fn: function(){
//					this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId2));
//					this._ctx.canvas.height = heightInit;
//					this._ctx.canvas.width = widthInit;
//					this.drawHorizontal(this._discreteColorRange2, this._colorValues2, this._title2);
//				}
//			}
//		
//		
//		}
//	
//	
//	}]
//});
//
//
//}
//drawHorizontal : function(discreteCR, cValues, title) {
//	
//	var colorValue;
//	var xstart=0;
//	var ystart=this._textWidthMax;		
//	
//	if (Math.max(this._fontSize,10) === 10){//limiting font size to minimum 10 px so special case to make text align properly 			
//		this._fontSize=10;
//	}
//	
//	this._ctx.font = this._fontSize + "px sans-serif";	
//	
//	//display title
//	this._ctx.fillStyle="black";
//	this._ctx.fillText(title,this.textOffset , 10);
//	
//	var xStartText=xstart-this._fontSize/2;
//	
//	if (this._cellWidth<15){//tweak for smaller cell sizes
//		xStartText=-2;
//	}	
//	var i;
//	for (i = 0 ; i < cValues.length; i++){
//		this._ctx.save();
//		this._ctx.translate(xStartText, this._textWidthMax);
//		this._ctx.rotate(-Math.PI/2);
//		colorValue = cValues[i];
//		
//		this._ctx.fillText(colorValue[1], 2, this._cellHeight, this._textWidthMax);
//		this._ctx.rotate(Math.PI/2);
//		this._ctx.restore();
//		xStartText=xStartText +this._cellWidth;			
//	}		
//	
//	xstart=0;
//	
//	for (i = 0 ; i < cValues.length; i++){			
//		colorValue = cValues[i];			
//		this._ctx.fillStyle = discreteCR.getCellColorString(colorValue[0]);
//		this._ctx.fillRect(xstart,this._textWidthMax+this._offset,this._cellWidth, this._cellHeight);
//		xstart=xstart + this._cellWidth;
//	}		
//	
//	
//
//},
//if (Math.max(this._fontSize,10) === 10){//limiting font size to minimum 10 px so special case to make text align properly 			
//	this._fontSize=10;
//}
