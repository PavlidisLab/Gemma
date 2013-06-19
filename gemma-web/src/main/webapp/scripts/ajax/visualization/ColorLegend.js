Ext.namespace('Gemma.Metaheatmap');

Gemma.Metaheatmap.ColorLegend = Ext.extend(Ext.Window, {

      height : 150,
      width : 270,
      constrain : true,
      closeAction : 'hide',
      closable : false,
      shadow : false,
      border : false,
      bodyBorder : false,
      margins : {
         top : 0,
         right : 0,
         bottom : 0,
         left : 0
      },

      initComponent : function() {

         Ext.apply(this, {
               cellSize : this.cellSize,

               foldChangeLabels : this.foldChangeLabels,
               foldChangeValues : this.foldChangeValues,

               pValueLabels : this.pValueLabels,
               pValueValues : this.pValueValues,

               discreteColorRange : this.discreteColorRangeObject,

               fontSize : this.fontSize,

               items : [{
                     xtype : 'box',
                     autoEl : 'canvas',
                     ref : 'boxCanvas',
                     listeners : {
                        afterrender : {
                           scope : this,
                           fn : function() {
                              this.ctx = Gemma.Metaheatmap.Utils.getCanvasContext(this.boxCanvas.el.dom);
                              this.ctx.canvas.height = this.height;
                              this.ctx.canvas.width = this.width;
                              this.drawVertical();
                           }
                        }
                     }
                  }]
            });

         Gemma.Metaheatmap.ColorLegend.superclass.initComponent.apply(this, arguments);
      },

      drawVertical : function() {
         var colorScale = {
            "5" : "rgb(142, 1, 82)",
            "4" : "rgb(197, 27, 125)",
            "3" : "rgb(222, 119, 174)",
            "2" : "rgb(241, 182, 218)",
            "1" : "rgb(253, 224, 239)",
            "0" : "rgb(247, 247, 247)",
            "-1" : "rgb(230, 245, 208)",
            "-2" : "rgb(184, 225, 134)",
            "-3" : "rgb(127, 188, 65)",
            "-4" : "rgb(77, 146, 33)",
            "-5" : "rgb(39, 100, 25)"
         };

         this.ctx.font = this.fontSize + "px sans-serif";
         var y = 1;
         var x = 1;

         for (var i = 0; i < this.foldChangeLabels.length; i++) {

            var color = colorScale[this.foldChangeValues[i]];
            this.drawCell_(1, y, this.cellSize, color, 0);

            this.ctx.fillStyle = "black";
            this.ctx.fillText(this.foldChangeLabels[i], this.cellSize + 3, y + this.cellSize);

            y += this.cellSize;
         }

         x = 115;
         y = 1;

         for (var i = 0; i < this.pValueLabels.length; i++) {
            var color = "white";

            if (this.pValueLabels[i] === "No Data") {
               this.drawMissingCell_(x, y, this.cellSize);
            } else if (this.pValueLabels[i] == "Not Significant") {
               this.drawTestedButNotSignificant_(x, y, this.cellSize)
            } else {
               var transparency = this.pValueValues[i] / 10;
               this.drawCell_(x, y, this.cellSize, color, transparency);
            }

            this.ctx.fillStyle = "black";
            this.ctx.fillText(this.pValueLabels[i], x + this.cellSize + 3, y + this.cellSize);

            y += this.cellSize;
         }

      },

      drawCell_ : function(x, y, size, color, transparency) {
         this.ctx.fillStyle = color;
         var innerBoxSize = Math.floor(0.5 * size);
         this.ctx.fillRect(x, y, size, size);
         this.ctx.fillStyle = "rgba(0,0,0," + transparency + ")";
         this.ctx.fillRect(x + size / 2 - innerBoxSize / 2, y + size / 2 - innerBoxSize / 2, innerBoxSize, innerBoxSize);
      },

      drawMissingCell_ : function(x, y, size) {
         this.ctx.fillStyle = 'white';
         this.ctx.fillRect(x, y, size, size);

         // var innerBoxSize = Math.floor(0.6*size);
         //
         // this.ctx.save();
         // this.ctx.strokeStyle = "gray";
         // this.ctx.translate (x, y);
         // this.ctx.beginPath();
         // this.ctx.moveTo (size/2 - innerBoxSize/2, size/2 - innerBoxSize/2);
         // this.ctx.lineTo (size/2 + innerBoxSize/2, size/2 + innerBoxSize/2);
         // this.ctx.moveTo (size/2 + innerBoxSize/2, size/2 - innerBoxSize/2);
         // this.ctx.lineTo (size/2 - innerBoxSize/2, size/2 + innerBoxSize/2);
         // this.ctx.stroke();
         // this.ctx.restore();
      },

      drawTestedButNotSignificant_ : function(x, y, size) {
         this.ctx.fillStyle = 'white';
         this.ctx.fillRect(x, y, size, size);

         var innerBoxWidth = Math.floor(0.5 * size);
         var innerBoxHeight = Math.floor(0.5 * size);

         this.ctx.strokeStyle = "rgba(0,0,0,0.1)";
         this.ctx.strokeRect(x + size / 2 - innerBoxWidth / 2, y + size / 2 - innerBoxHeight / 2, innerBoxWidth, innerBoxHeight);
      }

   });

Ext.reg('Metaheatmap.ColorLegend', Gemma.Metaheatmap.ColorLegend);

// var heightInit = this.cellHeight + this.textWidthMax + this.textOffset;
// Ext.apply(this, {
//
// height: heightInit*2 + 35,
//	
// width: this.cellWidth * this.colorValues.length + 25,
//	
// _offset: this.textOffset,
//	
// items: [{
//	
// xtype: 'box',
// autoEl: 'canvas',
// id: this.canvasId,
// listeners: {
// afterrender: {
// scope: this,
// fn: function(){
// this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId));
// this._ctx.canvas.height = heightInit;
// this._ctx.canvas.width = widthInit;
// this.drawHorizontal(this._discreteColorRange, this._colorValues, this._title);
// }
// }
//		
//		
// }
//	
//	
// },
// {
//	
// xtype: 'box',
// autoEl: 'canvas',
// id: this.canvasId2,
// listeners: {
// afterrender: {
// scope: this,
// fn: function(){
// this._ctx = Gemma.MetaVisualizationUtils.getCanvasContext(document.getElementById(this._canvasId2));
// this._ctx.canvas.height = heightInit;
// this._ctx.canvas.width = widthInit;
// this.drawHorizontal(this._discreteColorRange2, this._colorValues2, this._title2);
// }
// }
//		
//		
// }
//	
//	
// }]
// });
//
//
// }
// drawHorizontal : function(discreteCR, cValues, title) {
//	
// var colorValue;
// var xstart=0;
// var ystart=this._textWidthMax;
//	
// if (Math.max(this._fontSize,10) === 10){//limiting font size to minimum 10 px so special case to make text align
// properly
// this._fontSize=10;
// }
//	
// this._ctx.font = this._fontSize + "px sans-serif";
//	
// //display title
// this._ctx.fillStyle="black";
// this._ctx.fillText(title,this.textOffset , 10);
//	
// var xStartText=xstart-this._fontSize/2;
//	
// if (this._cellWidth<15){//tweak for smaller cell sizes
// xStartText=-2;
// }
// var i;
// for (i = 0 ; i < cValues.length; i++){
// this._ctx.save();
// this._ctx.translate(xStartText, this._textWidthMax);
// this._ctx.rotate(-Math.PI/2);
// colorValue = cValues[i];
//		
// this._ctx.fillText(colorValue[1], 2, this._cellHeight, this._textWidthMax);
// this._ctx.rotate(Math.PI/2);
// this._ctx.restore();
// xStartText=xStartText +this._cellWidth;
// }
//	
// xstart=0;
//	
// for (i = 0 ; i < cValues.length; i++){
// colorValue = cValues[i];
// this._ctx.fillStyle = discreteCR.getCellColorString(colorValue[0]);
// this._ctx.fillRect(xstart,this._textWidthMax+this._offset,this._cellWidth, this._cellHeight);
// xstart=xstart + this._cellWidth;
// }
//	
//	
//
// },
// if (Math.max(this._fontSize,10) === 10){//limiting font size to minimum 10 px so special case to make text align
// properly
// this._fontSize=10;
// }
