Ext.namespace( 'Gemma.Metaheatmap' );

/**
 * Panel that contains the heatmap display canvas.
 * 
 * @author AZ
 * @version $Id$
 */
Gemma.Metaheatmap.HeatmapBox = Ext.extend( Ext.Panel, {

   width : 'auto',
   initComponent : function() {
      Ext.apply( this, {
         border : false,
         // bodyBorder : false,
         autoScroll : true,
         isGeneOnTop : false,
         isShowPvalue : true,
         conditionTree : this.conditionTree,
         geneTree : this.geneTree,
         cells : this.cells,
         lastCellMouseIn : null,
         items : [ {
            xtype : 'box',
            autoEl : 'canvas',
            ref : 'boxCanvas'
         } ]
      } );

      Gemma.Metaheatmap.HeatmapBox.superclass.initComponent.apply( this, arguments );
   },

   
   /**
    * @memberOf Gemma.Metaheatmap.HeatmapBox 
    */
   initializeMe : function() {

      // when we say 'owner container' here, we mean the visualisation panel, so travel up the container stack until
      // we get to it
      // (this is alternative to this.ownerCt which is dangerous to use because it will break if there are any layout
      // changes made)
      var ownerCt = this.findParentByType( 'Metaheatmap.VisualizationPanel' );

      this.addEvents( 'cell_mouse_in', 'cell_mouse_out', 'cell_click' );

      this.ctx = Gemma.Metaheatmap.Utils.getCanvasContext( this.boxCanvas.el.dom );

      this.boxCanvas.el.on( 'mousemove', function( e, t ) {
         if ( this.lastCellMouseIn !== null ) {
            this.lastCellMouseIn.draw( this.ctx );
            this.fireEvent( 'cell_mouse_out', this.lastCellMouseIn, e, t );
         }
         var x = e.getPageX() - Ext.get( t ).getX();
         var y = e.getPageY() - Ext.get( t ).getY();
         var cell = this.getCellByXY( x, y );
         // don't show "click-me" finger over cells that have no data
         if ( cell && !cell.isProbeMissing ) {
            document.body.style.cursor = 'pointer'; // makes it really slow?
         } else {
            document.body.style.cursor = 'default'; // makes it really slow?
         }

         if ( cell !== null ) {
            cell.highlight( this.ctx );
            this.fireEvent( 'cell_mouse_in', cell, e, t );
         }
         this.lastCellMouseIn = cell;
      }, this );

      this.boxCanvas.el.on( 'mouseout', function( e, t ) {
         document.body.style.cursor = 'default'; // makes it really slow?
         if ( this.lastCellMouseIn !== null ) {
            this.lastCellMouseIn.draw( this.ctx );
            this.fireEvent( 'cell_mouse_out', this.lastCellMouseIn, e, t );
         }
         this.lastCellMouseIn = null;
      }, this );

      this.body.on( 'scroll', function( e, t ) {
         var scroll = this.body.getScroll();
         ownerCt.variableWidthCol.boxTopLabels.body.scrollTo( 'left', scroll.left );
         ownerCt.fixedWidthCol.boxSideLabels.body.scrollTo( 'top', scroll.top );
      }, this );

      this.boxCanvas.el.on( 'click', function( e, t ) {
         var x = e.getPageX() - Ext.get( t ).getX();
         var y = e.getPageY() - Ext.get( t ).getY();

         var cell = this.getCellByXY( x, y );

         // don't do anything if you click on a cell with no data
         if ( cell && !cell.isProbeMissing ) {
            this.fireEvent( 'cell_click', cell );
         }

      }, this );

      this.amIinitialized = true;
   },

   resizeAndPosition : function() {
      if ( !this.amIinitialized ) {
         this.initializeMe();
      }

      // when we say 'owner container' here, we mean the visualisation panel, so travel up the container stack until
      // we get to it
      // (this is alternative to this.ownerCt which is dangerous to use because it will break if there are any layout
      // nesting changes made)
      var ownerCt = this.findParentByType( 'Metaheatmap.VisualizationPanel' );

      var headerHeight = ownerCt.variableWidthCol.boxTopLabels.tree.display.size.height;
      var sideWidth = ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.height;

      this.setWidth( ownerCt.getWidth() - sideWidth - 20 );
      this.setHeight( ownerCt.getHeight() - headerHeight - 20 );

      this.boxCanvas.setWidth( ownerCt.variableWidthCol.boxTopLabels.tree.display.size.width );
      this.boxCanvas.setHeight( ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.width );

      this.ctx.canvas.width = ownerCt.variableWidthCol.boxTopLabels.tree.display.size.width;
      this.ctx.canvas.height = ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.width;
      this.doLayout();
   },

   draw : function() {
      if ( !this.amIinitialized ) {
         this.initializeMe();
      }

      this.lastCellMouseIn = null;

      this.ctx.clearRect( 0, 0, this.ctx.canvas.width, this.ctx.canvas.height );

      for ( var i = 0; i < this.geneTree.items.length; i++) {
         var gene = this.geneTree.items[i];
         for ( var j = 0; j < this.conditionTree.items.length; j++) {
            var condition = this.conditionTree.items[j];
            this.drawCell( gene, condition );
         }
      }
   },

   /*
    * Create cell.
    */
   getCellByXY : function( x, y ) {
      var gene, condition;

      if ( this.isGeneOnTop ) {
         gene = this.geneTree.findItemByCoordinate( x );
         condition = this.conditionTree.findItemByCoordinate( y );
      } else {
         gene = this.geneTree.findItemByCoordinate( y );
         condition = this.conditionTree.findItemByCoordinate( x );
      }
      if ( !gene || !condition || gene === null || condition === null )
         return null;

      var pValue = 1; // perhaps ">0.1"?
      var correctedPValue = 1; // perhaps "-"?
      var foldChange, isProbeMissing, numberOfProbes, numberOfProbesDiffExpressed;
      var isGeneOnTop = this.isGeneOnTop;
      var cell = this.cells.getCell( gene, condition );

      var colorScale = Gemma.Metaheatmap.Config.FoldChangeColorScale;

      var color;

      if ( cell.logFoldChange > 0 ) {
         if ( cell.logFoldChange > 1 ) {
            color = colorScale["3"];
         } else {
            color = colorScale["1"];
         }
      } else if ( cell.logFoldChange < 0 ) {
         if ( cell.logFoldChange < -1 ) {
            color = colorScale["-3"];
         } else {
            color = colorScale["-1"];
         }
      } else { // 0 is when we didn't store the contrast in DB
         color = 'white';
      }

      var transparency = 0;

      if ( cell.correctedPValue > Gemma.Constants.DifferentialExpressionQvalueThreshold ) {
         color = 'white';
      }

      pValue = cell.pValue;
      correctedPValue = cell.correctedPValue;
      foldChange = cell.logFoldChange;
      isProbeMissing = cell.isProbeMissing;
      numberOfProbes = cell.numberOfProbes;
      numberOfProbesDiffExpressed = cell.numberOfProbesDiffExpressed;

      if ( this.isShowPvalue ) {
         transparency = this.calculateVisualizationValueBasedOnPvalue( cell.correctedPValue ) / 10;
      }

      if ( cell.correctedPValue > Gemma.Constants.DifferentialExpressionQvalueThreshold ) {
         color = 'white';
         transparency = 0;
      }

      var x, y, width, height;

      if ( this.isGeneOnTop ) {
         x = gene.display.pxlStart;
         y = condition.display.pxlStart;
         width = gene.display.pxlSize;
         height = condition.display.pxlSize;
      } else {
         x = condition.display.pxlStart;
         y = gene.display.pxlStart;
         width = condition.display.pxlSize;
         height = gene.display.pxlSize;
      }

      var drawCellFunction = this.drawCell_;

      /**
       * Cell object
       */
      return {
         'gene' : gene,
         'condition' : condition,
         'pValue' : pValue,
         'correctedPValue' : correctedPValue,
         'foldChange' : foldChange,
         'visualizationValue' : color,
         'isProbeMissing' : isProbeMissing,
         'numberOfProbes' : numberOfProbes,
         'numberOfProbesDiffExpressed' : numberOfProbesDiffExpressed,

         drawCell_ : drawCellFunction,

         /**
          * Highlight a cell
          * 
          * @param ctx
          * @returns
          */
         highlight : function( ctx ) {
            this.drawCell_( ctx, gene, condition, color, isProbeMissing, isGeneOnTop, transparency );

            if ( width > 4 && height > 4 ) {
               ctx.strokeStyle = 'black';
               ctx.strokeRect( x + 0.5, y + 0.5, width - 1, height - 1 );
            } else {
               ctx.fillStyle = 'black';
               ctx.fillRect( x, y, width, height );
            }
         },

         draw : function( ctx ) {
            this.drawCell_( ctx, gene, condition, color, isProbeMissing, isGeneOnTop, transparency );
         }
      };
   },

   /**
    * Map pvalues to color
    * 
    * @param correctedPValue
    * @returns {Number}
    */
   calculateVisualizationValueBasedOnPvalue : function( correctedPValue ) {
      var visualizationValue = 0.5;
      if ( correctedPValue < 0.5 && correctedPValue >= 0.25 )
         visualizationValue = 0.5;
      else if ( correctedPValue < 0.25 && correctedPValue >= 0.1 )
         visualizationValue = 1;
      else if ( correctedPValue < 0.1 && correctedPValue >= 0.05 )
         visualizationValue = 1.1;
      else if ( correctedPValue < 0.05 && correctedPValue >= 0.01 )
         visualizationValue = 4;
      else if ( correctedPValue < 0.01 && correctedPValue >= 0.005 )
         visualizationValue = 7;
      else if ( correctedPValue < 0.005 && correctedPValue >= 0.0001 )
         visualizationValue = 10;
      else if ( correctedPValue < 0.0001 && correctedPValue >= 0.00001 )
         visualizationValue = 10;
      else if ( correctedPValue < 0.00001 )
         visualizationValue = 10;
      return visualizationValue;
   },

   /**
    * Draw a cell on the heatmap.
    * 
    * @param gene
    * @param condition
    */
   drawCell : function( gene, condition ) {
      var cellData = this.cells.getCell( gene, condition );

      var transparency = 0;

      var colorScale = Gemma.Metaheatmap.Config.FoldChangeColorScale;

      var color;

      if ( cellData.logFoldChange > 0 ) {
         if ( cellData.logFoldChange > 1 ) {
            color = colorScale["3"];
         } else {
            color = colorScale["1"];
         }
      } else if ( cellData.logFoldChange < 0 ) {
         if ( cellData.logFoldChange < -1 ) {
            color = colorScale["-3"];
         } else {
            color = colorScale["-1"];
         }
      } else { // 0 is when we didn't store the contrast in DB
         color = 'white';
      }

      if ( this.isShowPvalue ) {
         transparency = 0.1 * this.calculateVisualizationValueBasedOnPvalue( cellData.correctedPValue );
      }

      if ( cellData.correctedPValue > Gemma.Constants.DifferentialExpressionQvalueThreshold ) {
         color = 'white';
         transparency = 0;
      }

      this.drawCell_( this.ctx, gene, condition, color, cellData.isProbeMissing, this.isGeneOnTop, transparency );
   },

   /**
    * Low-level rendering method.
    * 
    * @param ctx
    * @param gene
    * @param condition
    * @param color
    * @param isProbeMissing
    * @param isGeneOnTop
    * @param transparency
    */
   drawCell_ : function( ctx, gene, condition, color, isProbeMissing, isGeneOnTop, transparency ) {
      if ( isProbeMissing ) {
         if ( isGeneOnTop ) {
            ctx.fillStyle = 'white';
            ctx.fillRect( gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize,
               condition.display.pxlSize );
            ctx.fillStyle = "gray";
            ctx.fillRect( gene.display.pxlStart + 0.5 * gene.display.pxlSize - 1, condition.display.pxlStart + 0.5
               * condition.display.pxlSize - 1, 2, 2 );
            ctx.restore();
         } else {
            ctx.fillStyle = 'white';
            ctx.fillRect( condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize,
               gene.display.pxlSize );
         }

      } else {
         ctx.fillStyle = color;
         if ( isGeneOnTop ) {
            ctx.fillRect( gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize,
               condition.display.pxlSize );
         } else {
            var innerBoxWidth = Math.floor( 0.5 * condition.display.pxlSize );
            var innerBoxHeight = Math.floor( 0.5 * gene.display.pxlSize );

            ctx.fillRect( condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize,
               gene.display.pxlSize );

            if ( transparency <= 0.1 ) {
               ctx.strokeStyle = "rgba(0,0,0,0.1)";
               ctx.strokeRect( condition.display.pxlStart + 0.5 * condition.display.pxlSize - 0.5 * innerBoxWidth,
                  gene.display.pxlStart + 0.5 * gene.display.pxlSize - 0.5 * innerBoxHeight, innerBoxWidth,
                  innerBoxHeight );
            } else {
               ctx.fillStyle = "rgba(0,0,0," + transparency + ")";
               ctx.fillRect( condition.display.pxlStart + 0.5 * condition.display.pxlSize - 0.5 * innerBoxWidth,
                  gene.display.pxlStart + 0.5 * gene.display.pxlSize - 0.5 * innerBoxHeight, innerBoxWidth,
                  innerBoxHeight );
            }
         }
      }
   },

   /**
    * Grey for missing values.
    * 
    * @param ctx
    * @param gene
    * @param condition
    * @param isGeneOnTop
    */
   drawMissingCell_ : function( ctx, gene, condition, isGeneOnTop ) {
      if ( isGeneOnTop ) {
         ctx.fillStyle = 'white';
         ctx.fillRect( gene.display.pxlStart, condition.display.pxlStart, gene.display.pxlSize,
            condition.display.pxlSize );

         ctx.fillStyle = "gray";

         ctx.fillRect( gene.display.pxlStart + 0.5 * gene.display.pxlSize - 1, condition.display.pxlStart + 0.5
            * condition.display.pxlSize - 1, 2, 2 );

         ctx.restore();
      } else {
         ctx.fillStyle = 'white';
         ctx.fillRect( condition.display.pxlStart, gene.display.pxlStart, condition.display.pxlSize,
            gene.display.pxlSize );

         var innerBoxWidth = Math.floor( 0.6 * condition.display.pxlSize );
         var innerBoxHeight = Math.floor( 0.6 * gene.display.pxlSize );

         ctx.save();
         ctx.strokeStyle = "gray";
         ctx.translate( condition.display.pxlStart, gene.display.pxlStart );
         ctx.beginPath();
         ctx.moveTo( condition.display.pxlSize * 0.5 - innerBoxWidth * 0.5, gene.display.pxlSize * 0.5 - innerBoxHeight
            * 0.5 );
         ctx.lineTo( condition.display.pxlSize * 0.5 + innerBoxWidth * 0.5, gene.display.pxlSize * 0.5 + innerBoxHeight
            * 0.5 );
         ctx.moveTo( condition.display.pxlSize * 0.5 + innerBoxWidth * 0.5, gene.display.pxlSize * 0.5 - innerBoxHeight
            * 0.5 );
         ctx.lineTo( condition.display.pxlSize * 0.5 - innerBoxWidth * 0.5, gene.display.pxlSize * 0.5 + innerBoxHeight
            * 0.5 );
         ctx.stroke();
         ctx.restore();
      }
   },

   onRender : function() {
      Gemma.Metaheatmap.HeatmapBox.superclass.onRender.apply( this, arguments );

      this.setSize( this.heatmapMaxWidth, this.heatmapMaxHeight );

      this.amIinitialized = false;
   }

} );

Ext.reg( 'Metaheatmap.HeatmapBox', Gemma.Metaheatmap.HeatmapBox );