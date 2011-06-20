Ext.namespace('Gemma');

Gemma.ColorLegend = Ext.extend(Ext.Window,
{
	
	title:'Color Legend',
	
	closeAction : 'hide',
	
	
    initComponent: function(){        
		
	
		
		if (this.vertical === true) {
		
			var widthInit = this.cellWidth + this.textWidthMax + this.textOffset;
			
			var heightInit = this.cellHeight * this.colorValues.length + this.fontSize + this.textOffset * 2;
			
			Ext.apply(this, {
			
				height: heightInit + 35,
				
				width: widthInit * 2 + 25,
				
				_offset: this.textOffset,
				
				items: [{
				
					xtype: 'box',
					autoEl: 'canvas',
					id: this.canvasId,
					listeners: {
						afterrender: {
							scope: this,
							fn: function(){
								this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId));
								this._ctx.canvas.height = heightInit;
								this._ctx.canvas.width = widthInit;
								this.drawVertical(this._discreteColorRange, this._colorValues, this._title);
							}
						}
					
					
					}
				
				
				}, {
					xtype: 'box',
					autoEl: 'canvas',
					id: this.canvasId2,
					listeners: {
						afterrender: {
							scope: this,
							fn: function(){
								this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId2));
								this._ctx.canvas.height = heightInit;
								this._ctx.canvas.width = widthInit;
								this.drawVertical(this._discreteColorRange2, this._colorValues2, this._title2);
							}
						}
					
					
					}
				
				}]
			});
			
		}
		else {
		
			var heightInit = this.cellHeight + this.textWidthMax + this.textOffset;
			Ext.apply(this, {
			
				height: heightInit*2 + 35,
				
				width: this.cellWidth * this.colorValues.length + 25,
				
				_offset: this.textOffset,
				
				items: [{
				
					xtype: 'box',
					autoEl: 'canvas',
					id: this.canvasId,
					listeners: {
						afterrender: {
							scope: this,
							fn: function(){
								this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId));
								this._ctx.canvas.height = heightInit;
								this._ctx.canvas.width = widthInit;
								this.drawHorizontal(this._discreteColorRange, this._colorValues, this._title);
							}
						}
					
					
					}
				
				
				},
				{
				
					xtype: 'box',
					autoEl: 'canvas',
					id: this.canvasId2,
					listeners: {
						afterrender: {
							scope: this,
							fn: function(){
								this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId2));
								this._ctx.canvas.height = heightInit;
								this._ctx.canvas.width = widthInit;
								this.drawHorizontal(this._discreteColorRange2, this._colorValues2, this._title2);
							}
						}
					
					
					}
				
				
				}]
			});
			
			
		}
		
	
		
		
        Ext.apply(this, {
        	
        	_textWidthMax:this.textWidthMax,//how many cellWidths you want the text to occupy
        	_cellHeight : this.cellHeight, 
			_cellWidth : this.cellWidth,
			_vertical : this.vertical,
			
			_discreteColorRange : this.discreteColorRangeObject,
			
			_discreteColorRange2 : this.discreteColorRangeObject2,
			
			_colorValues : this.colorValues,
			_colorValues2 : this.colorValues2,
			_canvasId : this.canvasId,
			_canvasId2 : this.canvasId2,
			_fontSize : this.fontSize,
			_ctx : null,
			_title : this.legendTitle,
			_title2 : this.legendTitle2,
        	
			margins : {
				top : 0,
				right : 0,
				bottom : 0,
				left : 0
			}
        	
         }); 
        
        Gemma.ColorLegend.superclass.initComponent.apply(this, arguments);
    
        
    },
    
    drawHorizontal : function(discreteCR, cValues, title) {
		
		var colorValue;
		var xstart=0;
		var ystart=this._textWidthMax;		
		
		if (Math.max(this._fontSize,10) === 10){//limiting font size to minimum 10 px so special case to make text align properly 			
			this._fontSize=10;
		}
		
		this._ctx.font = this._fontSize + "px sans-serif";	
		
		//display title
		this._ctx.fillStyle="black";
		this._ctx.fillText(title,this.textOffset , 10);
		
		var xStartText=xstart-this._fontSize/2;
		
		if (this._cellWidth<15){//tweak for smaller cell sizes
			xStartText=-2;
		}	
		var i;
		for (i = 0 ; i < cValues.length; i++){
			this._ctx.save();
			this._ctx.translate(xStartText, this._textWidthMax);
			this._ctx.rotate(-Math.PI/2);
			colorValue = cValues[i];
			
			this._ctx.fillText(colorValue[1], 2, this._cellHeight, this._textWidthMax);
			this._ctx.rotate(Math.PI/2);
			this._ctx.restore();
			xStartText=xStartText +this._cellWidth;			
		}		
		
		xstart=0;
		
		for (i = 0 ; i < cValues.length; i++){			
			colorValue = cValues[i];			
			this._ctx.fillStyle = discreteCR.getCellColorString(colorValue[0]);
			this._ctx.fillRect(xstart,this._textWidthMax+this._offset,this._cellWidth, this._cellHeight);
			xstart=xstart + this._cellWidth;
		}		
		
		

	},
	
	drawVertical : function(discreteCR,cValues, title) {
		
		var colorValue;
		//var xstart= this._textWidthMax+this._offset;
		var xstart=0;		
		var ystart=this.fontSize+this.textOffset*2;
		
		if (Math.max(this._fontSize,10) === 10){//limiting font size to minimum 10 px so special case to make text align properly 			
			this._fontSize=10;
		}
		
		this._ctx.font = this._fontSize + "px sans-serif";
		
		//display title
		this._ctx.fillStyle="black";
		this._ctx.fillText(title,this.textOffset , 10, this._textWidthMax);
		
		var yStartText=this._cellHeight/2+this._fontSize/2;
		
		if (this._cellHeight<15){//tweak for smaller cell sizes
			yStartText=yStartText-1;
		}
		
		
		//var textMeasurement;
				
		for (var i = 0 ; i < cValues.length; i++){
			colorValue = cValues[i];
			
			this._ctx.fillStyle = discreteCR.getCellColorString(colorValue[0]);
			this._ctx.fillRect(xstart,ystart,this._cellWidth, this._cellHeight);
			
			//textMeasurement = this._ctx.measureText(colorValue[1]);
			this._ctx.fillStyle="black";
			this._ctx.fillText(colorValue[1], this._cellWidth +this._offset, ystart+yStartText, this._textWidthMax);			
			
			ystart=ystart + this._cellHeight;
		}	
		
		
	}

});


