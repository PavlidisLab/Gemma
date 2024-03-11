/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

/**
 * 
 * @author Kelsey
 */

var Heatmap = function() {

   // row labels
   var MAX_LABEL_LENGTH_PIXELS = 400;
   var MAX_LABEL_LENGTH_CHAR = 175;
   var EXPANDED_BOX_WIDTH = 10;
   var CLIP = 3; // contrast. TODO: make it adjustable in range 2-4?
   var NAN_COLOR = "grey";
   var SHOW_LABEL_MIN_SIZE = 9; // 6 is unreadable; 8 is almost okay.
   var MIN_BOX_HEIGHT_FOR_LABELS = 12;
   var MAX_BOX_HEIGHT = 16;
   var MIN_BOX_HEIGHT = 2;
   // var MAX_ROWS_BEFORE_SCROLL = 30;
   var MIN_IMAGE_SIZE = 50;

   // column labels
   var MAX_SAMPLE_LABEL_HEIGHT_PIXELS = 220;
   var SAMPLE_LABEL_MAX_CHAR = 125;
   var PER_CONDITION_LABEL_HEIGHT = 10;

   // condition key
   // var MAX_PER_FACTOR_VALUE_LABEL_WIDTH = 200;
   var FACTOR_VALUE_LABEL_MAX_CHAR = 125;
   var FACTOR_VALUE_LABEL_BOX_WIDTH = 10;

   // extra space
   var TRIM = 5;
   var SMALL_TRIM = 3;

   var DEFAULT_ROW_LABEL = "&nbsp;";

   var DEFAULT_CONFIG = {
      label : false,
      maxBoxHeight : MAX_BOX_HEIGHT,
      allowTargetSizeAdjust : false
   };

   // black-red-orange-yellow-white
   var COLOR_16 = [ "rgb(0, 0, 0)", "rgb(32, 0, 0)", "rgb(64, 0, 0)", "rgb(96, 0, 0)", "rgb(128, 0, 0)",
                   "rgb(159, 32, 0)", "rgb(191, 64, 0)", "rgb(223, 96, 0)", "rgb(255, 128, 0)", "rgb(255, 159, 32)",
                   "rgb(255, 191, 64)", "rgb(255, 223, 96)", "rgb(255, 255, 128)", "rgb(255, 255, 159)",
                   "rgb(255, 255, 191)", "rgb(255, 255, 223)", "rgb(255, 255, 255)" ];

   /**
    * @param container
    * @param data -
    *           in form of VisualizationValueObject array with 'data' series.
    * @param config
    *           optional
    * @param sampleLabels
    *           optional.
    */
   function HeatMap( container, data, config, sampleLabels, conditionLabels, conditionLabelKey ) {

      if ( !config ) {
         config = DEFAULT_CONFIG;
      } else {
         config.label = config.label || false;
         config.maxBoxHeight = config.maxBoxHeight || MAX_BOX_HEIGHT;
         config.allowTargetSizeAdjust = config.allowTargetSizeAdjust || false;
      }

      drawMap( data, container, COLOR_16, config, sampleLabels, conditionLabels, conditionLabelKey );

      // Creates 1 canvas per row of the heat map
      function drawMap( vectorObjs, target, colors, config, sampleLabels, conditionLabels, conditionLabelKey ) {

         if ( target.getWidth() <= 0 || target.getHeight() <= 0 || vectorObjs.length === 0 ) {
            return;
         }

         var binSize = (2 * CLIP) / colors.length;

         // Get dimensions of target to determine box size in heat map
         var panelWidth = target.getWidth() - TRIM;
         var panelHeight = target.getHeight() - TRIM;
         var numRows = vectorObjs.length;

         var rowLabelSizePixels = 0;

         if ( config.label ) {

            // Try to figure out the room needed for the row labels. note: this is confusing because the label
            // contains html. The actual displayed text is less, so we
            // check rawLabel. FIXME the rawLabel has the html too.
            var maxRowLabelLength = 0;
            for (var i = 0; i < vectorObjs.length; i++) {
               if ( vectorObjs[i].rawLabel ) {
                  labelLength = Math.min( vectorObjs[i].rawLabel.length, MAX_LABEL_LENGTH_CHAR );
                  if ( labelLength > maxRowLabelLength ) {
                     maxRowLabelLength = labelLength;
                  }
               }
            }

            // multiplier is a guesstimate. Can get from ctx.measureText but we don't have that yet...
            rowLabelSizePixels = Math.min( MAX_LABEL_LENGTH_PIXELS, maxRowLabelLength * 8 );
         }

         var heatmapHeight = panelHeight; // initial guess.
         var labelHeight = 0;

         if ( config.showSampleNames && sampleLabels ) {

            // compute the room needed for the labels.
            var maxLabelLength = 0;
            for (var j = 0; j < sampleLabels.length; j++) {
               if ( sampleLabels[j].length > maxLabelLength ) {
                  maxLabelLength = sampleLabels[j].length;
               }
            }

            // maximum space for the label depends on mode.
            var maxLabelSize;
            if ( config.forceFit ) {
               // if we know the heatmap height here...
            } else {
               // FIXME allow as much as it wants.
            }

            // compute approximate pixel size of that label...not so easy.
            labelHeight = Math.min( MAX_SAMPLE_LABEL_HEIGHT_PIXELS,
               Math.min( maxLabelLength, SAMPLE_LABEL_MAX_CHAR ) * 8 );

            heatmapHeight = heatmapHeight - labelHeight;
         }

         var calculatedBoxHeight = Math.floor( heatmapHeight / numRows );

         if ( calculatedBoxHeight > config.maxBoxHeight ) {
            boxHeight = config.maxBoxHeight;
         } else if ( calculatedBoxHeight < MIN_BOX_HEIGHT_FOR_LABELS && config.label ) {
            boxHeight = MIN_BOX_HEIGHT_FOR_LABELS;
         } else {
            boxHeight = Math.max( MIN_BOX_HEIGHT, calculatedBoxHeight );
         }

         if ( boxHeight <= 0 ) {
            return;
         }

         // Final.
         heatmapHeight = boxHeight * numRows + TRIM;

         var numberOfColumns = vectorObjs[0].data.length; // assumed to be the same always.
         // avoid insanity
         if ( numberOfColumns === 0 ) {
            return;
         }

         var increment = 1;
         var heatmapWidth = 1;
         var boxWidth = 1;
         if ( config.forceFit ) {
            /*
             * shrink it down. Should always be true for thumbnails, or settable by user for big ones.
             */
            heatmapWidth = config.label ? Math.max( panelWidth - rowLabelSizePixels, MIN_IMAGE_SIZE ) : panelWidth;

            if ( heatmapWidth <= 0 ) {
               return;
            }

            /* do not use Math.floor, canvas will handle fractional values okay and fill the space. */
            // var boxWidth = Math.floor(heatmapWidth / numberOfColumns);
            boxWidth = heatmapWidth / numberOfColumns;

            /*
             * If columns are really tiny, we just skip some columns -- blur things out. This can also speed things up,
             * we don't try drawing details we can't see -- fewer squares to draw means less time. Note we allow
             * fractional values here, so the heatmap is the same exact width as before.
             */
            var mergedBoxWidth = boxWidth;
            while (mergedBoxWidth < 1) {
               increment++;
               mergedBoxWidth = boxWidth * increment;
            }
            boxWidth = mergedBoxWidth;

         } else {
            /*
             * Let it expand.
             */
            boxWidth = EXPANDED_BOX_WIDTH;
            heatmapWidth = boxWidth * numberOfColumns;
         }

         if ( config.allowTargetSizeAdjust && heatmapHeight < panelHeight ) {
            try {
               Ext.DomHelper.applyStyles( target, "height:" + heatmapHeight + "px" );
            } catch (e) {
               // just in case.
            }
         }

         // put the heatmap in a scroll panel if it is too big to display.
         if ( heatmapHeight + labelHeight > panelHeight
            || (!config.forceFit && heatmapWidth + rowLabelSizePixels > panelWidth) ) {

            try {
               Ext.DomHelper.applyStyles( target, "overflow:auto" );
            } catch (e) {
               // IE sometimes throws.
            }
         } else {
            try {
               Ext.DomHelper.applyStyles( target, "overflow:inherit" );
            } catch (e) {
               // IE sometimes throws.
            }
         }

         if ( config.legend && config.legend.show && config.legend.container ) {
            insertLegend( config.legend.container );
         }

         /*
          * Add labels to the columns. FIXME: if the heatmap itself isn't taking much space, make more room for the
          * labels.
          */
         if ( config.showSampleNames && sampleLabels ) {
            var id = 'sampleLabels-' + Ext.id();

            if ( boxWidth >= SHOW_LABEL_MIN_SIZE ) {

               Ext.DomHelper.append( target, {
                  id : id,
                  tag : 'div',
                  width : heatmapWidth,
                  height : MAX_SAMPLE_LABEL_HEIGHT_PIXELS
               } );
               var labelDiv = Ext.get( id );

               var ctx = constructCanvas( Ext.get( labelDiv ), heatmapWidth, labelHeight );

               ctx.fillStyle = "#000000";
               ctx.font = Math.min( 10, boxWidth - 1 ) + "px sans-serif";
               ctx.textAlign = "left";
               ctx.translate( 0, labelHeight - 2 );

               for (j = 0; j < sampleLabels.length; j++) {
                  // the shorter the better for performance.
                  var lab = Ext.util.Format.ellipsis( sampleLabels[j], SAMPLE_LABEL_MAX_CHAR );

                  ctx.translate( boxWidth, 0 );
                  ctx.rotate( -Math.PI / 2 );
                  ctx.fillText( lab, 0, 0 );
                  ctx.rotate( Math.PI / 2 );
               }
            } else if ( config.forceFit ) {

               var message;
               // If too many, no matter how wide they make it, there won't be room
               // -- expand is better.
               if ( numberOfColumns.length > 100 ) { // FIXME -- see if they have access to 'expand'.
                  message = "Click 'zoom in' to see the sample labels";
               } else {
                  message = "Click 'zoom in' or try widening the window to see the sample labels";
               }

               Ext.DomHelper.append( target, {
                  id : id,
                  tag : 'div',
                  width : target.getWidth() - TRIM,
                  height : 20
               } );

               labelDiv = Ext.get( id );
               ctx = constructCanvas( Ext.get( labelDiv ), target.getWidth() - TRIM, 20 );
               ctx.translate( 0, 10 );
               ctx.fillText( message, 0, 0 );
            }

         }
         if ( conditionLabels ) { // draw the colour labels above the columns

            var factorCount = 0;
            maxLabelLength = 0;
            for ( var factorCategory in conditionLabelKey) {
               factorCount++;
               // compute the room needed for the labels.
               if ( factorCategory.length > maxLabelLength ) {
                  maxLabelLength = factorCategory.length;
               }
            }
            // compute approximate pixel size of labels
            var labelWidth = Math.min( MAX_SAMPLE_LABEL_HEIGHT_PIXELS,
               Math.min( maxLabelLength, SAMPLE_LABEL_MAX_CHAR ) * 8 );

            id = 'conditionLabels-' + Ext.id();
            // id = 'conditionLabels-' + container.id;

            Ext.DomHelper.append( target, {
               id : id,
               tag : 'div',
               width : heatmapWidth + 10 + labelWidth,
               height : factorCount * PER_CONDITION_LABEL_HEIGHT + SMALL_TRIM
            } );
            labelDiv = Ext.get( id );

            ctx = constructCanvas( Ext.get( labelDiv ), heatmapWidth + 10 + labelWidth, factorCount
               * PER_CONDITION_LABEL_HEIGHT + SMALL_TRIM );
            var x = 0;
            var y = 0;
            // store row/column info for hover text
            var factorValueByLocation = []; // [row][column]
            // over-column boxes

            var adjacentColumnsDrawnAsOne = 0;
            for (j = 0; j < conditionLabels.length; j++) {
               for (factorCategory in conditionLabels[j]) {

                  var factorValueArr = conditionLabels[j][factorCategory];
                  var value = factorValueArr[0];
                  var colour = factorValueArr[1];

                  adjacentColumnsDrawnAsOne = 0;

                  if ( increment > 1 ) {
                     var colours = [ colour ];
                     // take the mode of adjacent columns.
                     for (var k = 1; k < increment && j + k < conditionLabels.length - 1; k++) {

                        factorValueArr = conditionLabels[j + k][factorCategory];
                        value = factorValueArr[0];
                        colour = factorValueArr[1];
                        colours.push( colour );
                        adjacentColumnsDrawnAsOne++;

                     }
                     colour = mode( colours );
                  }

                  ctx.fillStyle = colour;
                  ctx.fillRect( x, y, boxWidth, PER_CONDITION_LABEL_HEIGHT );
                  factorValueByLocation[y / PER_CONDITION_LABEL_HEIGHT] = [];
                  factorValueByLocation[y / PER_CONDITION_LABEL_HEIGHT][j] = value;
                  y += PER_CONDITION_LABEL_HEIGHT;
               }
               x += boxWidth;
               y = 0;
               j += adjacentColumnsDrawnAsOne;
            }
            // end of line labels
            ctx.fillStyle = "#000000";
            ctx.font = Math.min( 10, PER_CONDITION_LABEL_HEIGHT - 1 ) + "px sans-serif";
            ctx.textAlign = "left";
            ctx.translate( boxWidth * conditionLabels.length / increment + 10, Math.min( 10,
               PER_CONDITION_LABEL_HEIGHT - 1 ) );
            x = 0;
            y = 0;
            for (factorCategory in conditionLabelKey) {
               var facCat = Ext.util.Format.ellipsis( factorCategory, FACTOR_VALUE_LABEL_MAX_CHAR );
               ctx.fillText( facCat, x, y );
               y += PER_CONDITION_LABEL_HEIGHT;
            }

         }
         var vid = "heatmapCanvas-" + Ext.id();
         Ext.DomHelper.append( target, {
            id : vid,
            tag : 'div',
            width : panelWidth,
            height : boxHeight,
            style : "width:" + panelWidth + ";height:" + heatmapHeight
         } );

         var canvasDiv = Ext.get( vid );
         ctx = constructCanvas( Ext.get( vid ), heatmapWidth, heatmapHeight );

         /*
          * Draw the heatmap.
          */
         var offsety = 0;
         for (i = 0; i < vectorObjs.length; i++) {
            var d = vectorObjs[i].data; // points.
            var offsetx = 0;
            for (j = 0; j < d.length; j++) {

               var a = d[j][1];

               if ( a > CLIP ) {
                  a = CLIP;
               } else if ( a < -CLIP ) {
                  a = -CLIP;
               }

               if ( increment > 1 ) {
                  // take an average of adjacent columns. If we hit an NaN, too bad...
                  for (k = 1; k < increment && j < d.length - 1; k++) {
                     j++;
                     a += d[j][1];

                  }
                  a /= increment;

               }

               if ( isNaN( a ) ) {
                  ctx.fillStyle = NAN_COLOR;
               } else {
                  ctx.fillStyle = getColor( a, binSize, colors );
               }

               try {
                  ctx.fillRect( offsetx, offsety, boxWidth, boxHeight );
               } catch (e) {
                  /*
                   * Probably a config error somewhere...
                   */
               }
               offsetx = offsetx + boxWidth;

               last = a;

            }

            if ( config.label ) {
               // Add row label FIXME let these have more room if the heatmap fits okay, instead of
               // chopping it off.
               var rowLabel = DEFAULT_ROW_LABEL;
               if ( vectorObjs[i].label ) {
                  rowLabel = vectorObjs[i].label;
               }
               var text = Ext.DomHelper.append( canvasDiv, {
                  id : "heatmaplabel-" + Ext.id(),
                  tag : 'div',
                  html : "&nbsp;" + rowLabel,
                  style : "white-space: nowrap;"
               }, true );
               Ext.DomHelper.applyStyles( text, "position:absolute;top:" + offsety + "px;left:" + (offsetx + 5) + "px" );
            }
            offsety += boxHeight;
         }

         // add legend for experimental design / condition bar chart
         if ( config.conditionLegend ) {

            var factorValueCount = 0;
            factorCount = 0;
            var allColumnsWidth = 0;
            var maxColumnWidth = 0;
            for (factorCategory in conditionLabelKey) {
               factorCount++;
               // compute the room needed for the labels.
               if ( ctx.measureText( factorCategory ).width > maxColumnWidth ) {
                  maxColumnWidth = ctx.measureText( factorCategory ).width;
               }
               for ( var factorValue in conditionLabelKey[factorCategory]) {
                  factorValueCount++;
                  // compute the room needed for the labels.
                  var dim = ctx.measureText( factorValue );
                  var width = Math.round( dim.width );
                  if ( width > maxColumnWidth ) {
                     maxColumnWidth = width;
                  }
               }
               allColumnsWidth += maxColumnWidth;
               maxColumnWidth = 0;
            }
            var fontSize = Math.min( 10, PER_CONDITION_LABEL_HEIGHT - 1 );
            var charWidth = Math.floor( fontSize * 0.8 );
            // compute approximate pixel size of labels
            labelWidth = allColumnsWidth + (FACTOR_VALUE_LABEL_BOX_WIDTH + SMALL_TRIM + SMALL_TRIM) * factorCount;

            id = 'conditionKey-' + Ext.id();

            Ext.DomHelper.append( target, {
               id : id,
               tag : 'div',
               width : heatmapWidth,
               height : factorValueCount * PER_CONDITION_LABEL_HEIGHT + SMALL_TRIM
            } );
            labelDiv = Ext.get( id );

            ctx = constructCanvas( Ext.get( labelDiv ), 10 + labelWidth, factorValueCount * PER_CONDITION_LABEL_HEIGHT
               + SMALL_TRIM + 20 );
            ctx.fillStyle = "#000000";
            ctx.font = fontSize + "px sans-serif";
            ctx.textAlign = "left";
            ctx.translate( 10, 20 );
            x = 0;
            y = 0;
            for (factorCategory in conditionLabelKey) {
               facCat = Ext.util.Format.ellipsis( factorCategory, FACTOR_VALUE_LABEL_MAX_CHAR );
               var maxLabelWidthInCategory = 0;
               dim = ctx.measureText( facCat );
               width = Math.round( dim.width );
               if ( width > maxLabelWidthInCategory ) {
                  maxLabelWidthInCategory = width;
               }
               ctx.fillText( facCat, x, y );
               y += PER_CONDITION_LABEL_HEIGHT + 2;
               for (factorValue in conditionLabelKey[factorCategory]) {
                  var facVal = Ext.util.Format.ellipsis( factorValue, FACTOR_VALUE_LABEL_MAX_CHAR );
                  colour = conditionLabelKey[factorCategory][factorValue];
                  ctx.fillStyle = colour;
                  ctx.fillRect( x, y - fontSize, FACTOR_VALUE_LABEL_BOX_WIDTH, PER_CONDITION_LABEL_HEIGHT );
                  x += FACTOR_VALUE_LABEL_BOX_WIDTH + SMALL_TRIM;
                  ctx.fillStyle = "#000000";
                  ctx.fillText( facVal, x, y );
                  x = 0;
                  y += PER_CONDITION_LABEL_HEIGHT;
                  dim = ctx.measureText( facVal );
                  width = Math.round( dim.width );
                  if ( width > maxLabelWidthInCategory ) {
                     maxLabelWidthInCategory = width;
                  }
               }
               ctx.translate( FACTOR_VALUE_LABEL_BOX_WIDTH + SMALL_TRIM + maxLabelWidthInCategory + SMALL_TRIM, 0 );
               x = 0;
               y = 0;
            }
         }
      }

      // figure out which colour to use
      function getColor( a, binSize, colors ) {

         var v = Math.floor( (a + CLIP) / binSize );
         if ( v > colors.length - 1 ) {
            v = colors.length - 1;
         }
         return colors[v];
      }

      var MAX_LEGEND_HEIGHT = 10;
      var LEGEND_WIDTH = 64;

      /**
       * Add a legend to the heatmap
       */
      function insertLegend( container, vertical ) {

         if ( !container ) {
            return;
         }

         var legendDiv = Ext.get( container );

         if ( !legendDiv ) {
            return;
         }

         legendDiv.innerHTML = ''; // careful...

         var legendWidth = legendDiv.getWidth() - 10;
         var legendHeight = legendDiv.getHeight();
         var boxHeight = 10;
         var boxWidth = 10;

         var ctx;

         var numBoxes = COLOR_16.length / 2;
         var scalebarLength = 0;
         if ( vertical ) {
            scalebarLength = Math.ceil( boxHeight * numBoxes );
            ctx = constructCanvas( Ext.get( container ), 50, 60 + scalebarLength );
         } else {
            scalebarLength = Math.ceil( boxWidth * numBoxes );
            ctx = constructCanvas( Ext.get( container ), 60 + scalebarLength, 40 );
         }

         ctx.fillStyle = "#000000"; // black...
         ctx.font = boxHeight + "px sans-serif";
         ctx.textAlign = "left";
         ctx.lineWidth = 1;
         ctx.textBaseline = 'top';

         var x;
         var y = 3;

         if ( vertical ) {
            x = boxWidth + 3;
         } else {
            x = 3;
         }

         ctx.save();
         ctx.translate( x, y );
         ctx.fillText( sprintf( "%.1f", -CLIP ), 0, 0 );
         ctx.restore();

         if ( vertical ) {
            x = 3;
         } else {
            x += ctx.measureText( sprintf( "%.1f", -CLIP ) ).width + 4;
         }

         for (var i = 0; i < COLOR_16.length; i += 2) {

            if ( !COLOR_16[i] ) {
               break;
            }

            ctx.save();

            // draw outlines.
            ctx.beginPath();

            if ( vertical ) {

            } else {
               ctx.moveTo( x, y );
               ctx.lineTo( x + boxWidth, y );
               ctx.moveTo( x, y + boxHeight );
               ctx.lineTo( x + boxWidth, y + boxHeight );
            }
            ctx.stroke();
            ctx.restore();

            ctx.fillStyle = COLOR_16[i];
            ctx.fillRect( x, y, boxWidth, boxHeight );

            if ( vertical ) {
               y += boxHeight;
            } else {
               x += boxWidth;
            }
         }

         // close box at right
         ctx.save();
         ctx.beginPath();
         ctx.moveTo( x, y );
         ctx.lineTo( x, y + boxHeight );
         ctx.stroke();
         ctx.restore();

         x += 4; // next to the last box
         ctx.fillStyle = "#000000"; // black...
         ctx.fillText( sprintf( "%.1f", CLIP ), x, y );

      }

      /**
       * Function: (private) constructCanvas
       * 
       * Initializes a canvas. When the browser is IE, we make use of excanvas.
       * 
       * Parameters: none
       * 
       * Returns: ctx
       */
      function constructCanvas( div, canvasWidth, canvasHeight ) {

         /**
          * For positioning labels and overlay.
          */
         div.setStyle( {
            'position' : 'relative'
         } );

         if ( canvasWidth <= 0 || canvasHeight <= 0 ) {
            throw 'Invalid dimensions for plot, width = ' + canvasWidth + ', height = ' + canvasHeight;
         }

         var canvas = Ext.DomHelper.append( div, {
            tag : 'canvas',
            width : canvasWidth,
            height : canvasHeight
         } );

         // check if canvas is supported (not supported in IE < 9; need to use excanvas in IE8)
         if ( !document.createElement( "canvas" ).getContext && Prototype.Browser.IE ) {
            canvas = Ext.get( window.G_vmlCanvasManager.initElement( canvas ) );
         }

         return canvas.getContext( '2d' );
      }

   }

   return {
      clean : function( element ) {
         element.innerHTML = '';
      },

      draw : function( target, data, options, sampleLabels, conditionLabels, conditionLabelKey ) {
         var map = new HeatMap( target, data, options, sampleLabels, conditionLabels, conditionLabelKey );
         return map;
      }
   };
}();

function mode( array ) {
   if ( array.length == 0 )
      return null;
   var modeMap = {};
   var maxEl = array[0], maxCount = 1;
   for (var i = 0; i < array.length; i++) {
      var el = array[i];
      if ( modeMap[el] == null )
         modeMap[el] = 1;
      else
         modeMap[el]++;
      if ( modeMap[el] > maxCount ) {
         maxEl = el;
         maxCount = modeMap[el];
      }
   }
   return maxEl;
}
