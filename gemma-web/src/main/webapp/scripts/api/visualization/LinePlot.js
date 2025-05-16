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
 * Draw line plots with (optional) sample labels above the graph.
 * 
 * 
 * @author Paul
 */
Gemma.LinePlot = (function() {

   var MAX_SAMPLE_LABEL_HEIGHT_PIXELS = 120;
   var TRIM = 5;
   var SMALL_TRIM = 3;
   var EXPANDED_BOX_WIDTH = 10; // maximum value to use when expanded.
   var SAMPLE_LABEL_MAX_CHAR = 125;
   var PER_CONDITION_LABEL_HEIGHT = 10;

   // condition key
   var MAX_PER_FACTOR_VALUE_LABEL_WIDTH = 200;
   var FACTOR_VALUE_LABEL_MAX_CHAR = 125;
   var FACTOR_VALUE_LABEL_BOX_WIDTH = 10;

   function LinePlot( container, data, config, sampleLabels, conditionLabels, conditionLabelKey ) {

      draw( container, data, config, sampleLabels, conditionLabels, conditionLabelKey );

      /**
       * 
       */
      function draw( container, series, config, sampleLabels, conditionLabels, conditionLabelKey ) {
         // maybe not ready yet.
         if ( series.length === 0 ) {
            return;
         }
         var numberOfColumns = series[0].data.length; // assumed to be the same always.

         // avoid insanity
         if ( numberOfColumns === 0 ) {
            return;
         }

         /*
          * Smooth or unsmooth the data, but only if the previous state has changed.
          */

         if ( config.smoothLineGraphs ) {
            if ( !series.smoothed ) {
               smooth( series );
               series.smoothed = true;
            }
         } else {
            if ( series.smoothed ) {
               // put back the original data.
               for (var i = 0; i < data.length; i++) {
                  var d = series[i].data;
                  if ( series[i].backup ) { // better be!
                     for (var j = 0; j < d.length; j++) {
                        series[i].data[j][1] = series[i].backup[j][1];
                     }
                  }
               }
               series.smoothed = false;
            }
         }

         /*
          * Deal with plotting.
          */

         // TOTAL area available.
         var width = container.getWidth();
         var height = container.getHeight();

         /* happens with hidden thumbnails sometimes, set to non-zero to avoid errors in some browsers */
         if ( width === 0 ) {
            width = 10;
         }
         if ( height === 0 ) {
            height = 10;
         }

         // space used for the line plot itself.
         var plotHeight = height; // to be modified to make room for the sample labels.

         // initial guess..
         var plotWidth = width - (2 * TRIM);

         var spacePerPoint;

         if ( config.forceFit ) {
            /*
             * doing a math.floor here doesn't work because the rounding adds up and makes the factor value bar chart
             * out of step with the graph
             */
            spacePerPoint = plotWidth / numberOfColumns;

         } else {
            spacePerPoint = Math.max( EXPANDED_BOX_WIDTH, plotWidth / numberOfColumns );
            plotWidth = spacePerPoint * numberOfColumns;
         }

         // put the heatmap in a scroll panel if it is too big to display.
         if ( !config.forceFit && plotWidth > width ) {
            Ext.DomHelper.applyStyles( container, "overflow:auto" );
         }

         if ( config.showSampleNames && sampleLabels ) {

            if ( spacePerPoint >= EXPANDED_BOX_WIDTH ) {
               var fontSize = Math.min( 12, spacePerPoint - 1 );
               // longest label...
               var maxLabelLength = 0;
               for (var j = 0; j < sampleLabels.length; j++) {
                  if ( sampleLabels[j].length > maxLabelLength ) {
                     maxLabelLength = sampleLabels[j].length;
                  }
               }
               // compute approximate pixel size of that label...not so easy without context.
               var labelHeight = Math.round( Math.min( MAX_SAMPLE_LABEL_HEIGHT_PIXELS, Math.min( maxLabelLength,
                  SAMPLE_LABEL_MAX_CHAR )
                  * fontSize * 0.8 ) ); // 0.8
               // is
               // about
               // right
               // (pixel
               // width)/(pixel
               // of
               // height).

               var id = 'sampleLabels-' + Ext.id();

               var sampleLabelsWidth = plotWidth + (2 * TRIM);

               Ext.DomHelper.append( container, {
                  id : id,
                  tag : 'div',
                  width : sampleLabelsWidth,
                  height : labelHeight
               } );

               var ctx = constructCanvas( Ext.get( id ), sampleLabelsWidth, labelHeight );

               ctx.fillStyle = "#000000";
               ctx.font = fontSize + "px sans-serif";
               ctx.textAlign = "left";
               ctx.translate( TRIM + spacePerPoint, labelHeight - 2 ); // +extra to make sure we don't chop off the
               // top.

               // vertical text.
               for (var j = 0; j < sampleLabels.length; j++) {
                  // the shorter the better for performance.
                  var lab = Ext.util.Format.ellipsis( sampleLabels[j], SAMPLE_LABEL_MAX_CHAR );

                  ctx.rotate( -Math.PI / 2 );
                  ctx.fillText( lab, 0, 0 );
                  ctx.rotate( Math.PI / 2 );
                  ctx.translate( spacePerPoint, 0 );
               }

               plotHeight = plotHeight - labelHeight;

            } else if ( config.forceFit ) {
               var message;
               if ( numberOfColumns.length > 80 ) { // basically, no matter how wide they make it, there won't be
                  // room.
                  message = "Click 'zoom in' to see the sample labels";
               } else {
                  message = "Click 'zoom in' or try widening the window to see the sample labels";
               }

               var mid = "message-" + Ext.id();

               Ext.DomHelper.append( container, {
                  id : mid,
                  tag : 'div',
                  width : plotWidth,
                  height : 20
               } );

               var ctx = constructCanvas( Ext.get( mid ), plotWidth, 20 );
               ctx.translate( 10, 10 );
               ctx.fillText( message, 0, 0 );
               plotHeight = plotHeight - 20;
            }

         }
         var factorCount = 0;
         maxLabelLength = 0;
         for ( var factorCategory in conditionLabelKey) {
            factorCount++;
            // compute the room needed for the labels.
            if ( factorCategory.length > maxLabelLength ) {
               maxLabelLength = factorCategory.length;
            }
         }
         var conditionLabelsHeight = factorCount * PER_CONDITION_LABEL_HEIGHT + SMALL_TRIM;
         if ( conditionLabels ) { // draw the colour labels above the columns

            // compute approximate pixel size of labels
            // var labelWidth = Math.min(MAX_SAMPLE_LABEL_HEIGHT_PIXELS, Math.min(maxLabelLength, SAMPLE_LABEL_MAX_CHAR)
            // *
            // 8);

            id = 'conditionLabels-' + Ext.id();
            // id = 'conditionLabels-' + container.id;

            Ext.DomHelper.append( container, {
               id : id,
               tag : 'div',
               width : plotWidth,
               height : conditionLabelsHeight
            } );
            labelDiv = Ext.get( id );

            // ctx = constructCanvas(Ext.get(labelDiv), plotWidth + 10 + labelWidth, factorCount *
            // PER_CONDITION_LABEL_HEIGHT
            // + SMALL_TRIM);
            ctx = constructCanvas( Ext.get( labelDiv ), plotWidth + 10, conditionLabelsHeight );
            var x = 0;
            var y = 0;
            ctx.translate( TRIM, 0 );
            // over-column boxes
            for (j = 0; j < conditionLabels.length; j++) {
               for (factorCategory in conditionLabels[j]) {
                  var factorValueArr = conditionLabels[j][factorCategory];
                  var value = factorValueArr[0];
                  var colour = factorValueArr[1];
                  ctx.fillStyle = colour;
                  ctx.fillRect( x, y, spacePerPoint, PER_CONDITION_LABEL_HEIGHT );
                  y += PER_CONDITION_LABEL_HEIGHT;
               }
               x += spacePerPoint;
               y = 0;
            }
            // end of line labels
            /*
             * ctx.fillStyle = "#000000"; ctx.font = Math.min(10, PER_CONDITION_LABEL_HEIGHT - 1) + "px sans-serif";
             * ctx.textAlign = "left"; ctx.translate(plotWidth + 10, Math.min(10, PER_CONDITION_LABEL_HEIGHT - 1)); x =
             * 0; y = 0; for ( factorCategory in conditionLabelKey) { var facCat =
             * Ext.util.Format.ellipsis(factorCategory, FACTOR_VALUE_LABEL_MAX_CHAR); ctx.fillText(facCat, x, y); y +=
             * PER_CONDITION_LABEL_HEIGHT; }
             */

         }

         plotHeight = plotHeight - 4 * TRIM - conditionLabelsHeight;

         if ( plotHeight < 10 || plotWidth < 10 ) {
            return; // don't try ...
         }

         var vid = "lineplotCanvas-" + Ext.id();
         Ext.DomHelper.append( container, {
            id : vid,
            tag : 'div',
            style : "margin:" + TRIM + "px;width:" + plotWidth + "px;height:" + plotHeight + "px"
         } );
         var target = Ext.get( vid ).dom;

         Flotr.draw( target, series, config );
      }

   }

   function smooth( series ) {
      for (var i = 0; i < series.length; i++) {

         var d = series[i].data;

         if ( !series[i].backup ) {
            series[i].backup = [];
            for (var m = 0; m < d.length; m++) {
               series[i].backup.push( [ d[m][0], d[m][1] ] );
            }
         }

         // window size from 2-10.
         var w = Math.min( 10, Math.max( Math.floor( d.length / 25.0 ), 2 ) );

         var lv = 0;
         var ave = [];
         for (var j = 0; j < d.length; j++) {

            var u = 0, v = 0;
            if ( j < d.length - w ) {
               for (var k = j; k < j + w; k++) {
                  v += d[k][1];
                  u++;
               }
            } else {
               // at the end, we just average what we can. This causes some problems, but not too bad.
               for (var k = j; k < d.length; k++) {
                  v += d[k][1];
                  u++;
               }
            }

            ave.push( v / u );

         }

         for (j = 0; j < d.length; j++) {
            series[i].data[j][1] = ave[j];
         }

      }

   }

   /**
    * Function: (private) constructCanvas
    * 
    * Initializes a canvas. When the browser is IE, we make use of excanvas.
    * 
    * Parameters: none
    * 
    * Returns: ctx
    * 
    * @private
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

   return {
      clean : function( element ) {
         element.innerHTML = '';
      },

      draw : function( target, data, options, sampleLabels, conditionLabels, conditionLabelKey ) {
         return new LinePlot( target, data, options, sampleLabels, conditionLabels, conditionLabelKey );
      }
   };
}());