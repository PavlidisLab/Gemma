/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
Ext.namespace( 'Gemma' );

/**
 * Instantiates the core cytoscape
 * 
 * @param {Object}
 *           visualization the cy object (dom reference)
 * @param {Array}
 *           graphData
 * @param {Function}
 *           readyFunction
 * @param {Object}
 *           ownerRef - CytoscapeJSDisplay
 * 
 * From the cytoscape.js docs:
 * <p>
 * data() specifies a direct mapping to an element's data field. For example, data(descr) would map a property to the
 * value in an element's descr field in its data (i.e. ele.data("descr")). This is useful for mapping to properties like
 * label text content (the content property).
 * <p>
 * mapData() specifies a linear mapping to an element's data field. For example, mapData(weight, 0, 100, blue, red) maps
 * an element's weight to gradients between blue and red for weights between 0 and 100.
 * 
 */
Gemma.CytoscapeJSCoexGraphInitializer = function( visualization, graphData, readyFunction, ownerRef ) {

   visualization.cytoscape( {

      container : document.getElementById( 'cy' ),

      showOverlay : false,
      fit : false,

      // this is the "functional" mode of setting these. See http://cytoscape.github.io/cytoscape.js/#style/format
      style : cytoscape.stylesheet().selector( 'node.emphasis' ).css( {
         'content' : 'data(name)',
         'font-family' : Gemma.CytoscapeSettings.labelFontName,
         'font-size' : Gemma.CytoscapeSettings.labelFontSize,
         // 'text-opacity' : 'data(nodeDegreeOpacity + 0.2)',
         // 'text-outline-width': 1,
         // 'text-outline-color': '#888',
         'text-valign' : 'center',
         'color' : Gemma.CytoscapeSettings.labelFontColor,
         'width' : Gemma.CytoscapeSettings.nodeSize,
         'height' : Gemma.CytoscapeSettings.nodeSize,
         'border-color' : Gemma.CytoscapeSettings.nodeQueryColorTrue,
         'border-width' : 'mapData(queryflag,0,1,0,3)',
         // node color
         'background-color' : 'data(nodeDegreeColor)',
         // 'z-index' : 'data(support)', // FIXME make less specific nodes behind

         'text-valign' : 'top',
         // Do not show the labels when zoomed way out.
         'min-zoomed-font-size' : 9,
         'visibility' : 'visible' // filtering will show appropriate nodes
      } ).selector( 'node.basic' ).css( {
         'content' : 'data(name)',
         // 'z-index' : 'data(support)',

         // the text color
         'color' : Gemma.CytoscapeSettings.labelFontColor,
         'font-family' : Gemma.CytoscapeSettings.labelFontName,
         'font-size' : Gemma.CytoscapeSettings.labelFontSize,
         'text-valign' : 'center',
         'width' : Gemma.CytoscapeSettings.nodeSize,
         'height' : Gemma.CytoscapeSettings.nodeSize,
         'border-color' : Gemma.CytoscapeSettings.nodeQueryColorTrue,
         'border-width' : 'mapData(queryflag,0,1,0,3)',
         // node color
         'background-color' : Gemma.CytoscapeSettings.nodeColor,
         'text-valign' : 'top',
         // Do not show the labels when zoomed way out.
         'min-zoomed-font-size' : 9,
         'visibility' : 'visible'
      } ).selector( 'node.overlay' ).css( {
         'content' : 'data(name)',
         // the text color
         'color' : Gemma.CytoscapeSettings.nodeColorOverlay,
         'font-family' : Gemma.CytoscapeSettings.labelFontName,
         'font-size' : Gemma.CytoscapeSettings.labelFontSize,
         'text-valign' : 'center',
         'width' : Gemma.CytoscapeSettings.nodeSize,
         'height' : Gemma.CytoscapeSettings.nodeSize,
         'border-color' : Gemma.CytoscapeSettings.nodeQueryColorTrue,
         'border-width' : 'mapData(queryflag,0,1,0,3)',

         // Do not show the labels when zoomed way out.
         'min-zoomed-font-size' : 9,
         // node color
         'background-color' : Gemma.CytoscapeSettings.nodeColorOverlay,
         'text-valign' : 'top',
         'visibility' : 'visible'
      } ).selector( ':selected' ).css( {
         'background-color' : Gemma.CytoscapeSettings.selectionGlowColor,
         'line-color' : '#000',
         'target-arrow-color' : '#000',
         'text-outline-color' : '#000'
      } ).selector( 'edge.emphasis' ).css( {
         'width' : 'mapData(support,2,20,1,10)',
         'line-color' : 'data(nodeDegreeColor)'
      } ).selector( 'edge.basic' ).css( {
         // 'z-index' : 'data(support)', // FIXME make less important edges behind
         // "For example, data(weight, 0, 100, blue, red) maps an element's weight to gradients between blue and red for
         // weights between 0 and 100"
         'width' : 'mapData(support,2,20,1,10)'
      } ).selector( "edge[supportSign='positive']" ).css( {
         'line-color' : Gemma.CytoscapeSettings.supportColorPositive
      } ).selector( "edge[supportSign='negative']" ).css( {
         'line-color' : Gemma.CytoscapeSettings.supportColorNegative
      } ).selector( "edge[supportSign='both']" ).css( {
         'line-color' : Gemma.CytoscapeSettings.supportColorBoth
      } ),

      layout : {
         name : 'arbor', // cose
         liveUpdate : false
      },

      renderer : {
         selectionToPanDelay : 350,
         dragToSelect : true,
         dragToPan : true
      },

      elements : graphData,

      ready : function() {
         ownerRef.cy = this;
         readyFunction( ownerRef );
      }
   } );

   // http://plugins.jquery.com/cytoscape.js-panzoom/
   // the default values of each option are outlined below:
   visualization.cytoscapePanzoom( {
      zoomFactor : 0.1, // zoom factor per zoom tick
      zoomDelay : 45, // how many ms between zoom ticks
      minZoom : 0.1, // min zoom level
      maxZoom : 10, // max zoom level
      fitPadding : 50, // padding when fitting
      panSpeed : 10, // how many ms in between pan ticks
      panDistance : 10, // max pan distance per tick
      panDragAreaSize : 75, // the length of the pan drag box in which the vector for panning is calculated (bigger =
      // finer control of pan speed and direction)
      panMinPercentSpeed : 0.25, // the slowest speed we can pan by (as a percent of panSpeed)
      panInactiveArea : 8, // radius of inactive area in pan drag box
      panIndicatorMinOpacity : 0.5, // min opacity of pan indicator (the draggable nib); scales from this to 1.0
      autodisableForMobile : true, // disable the panzoom completely for mobile (since we don't really need it with
      // gestures like pinch to zoom)

      // icon class names
      sliderHandleIcon : 'fa fa-minus',
      zoomInIcon : 'fa fa-plus',
      zoomOutIcon : 'fa fa-minus',
      resetIcon : 'fa fa-expand'
   } );

   // disabled until we have a functionality - e.g: extend nodes; show details of gene. Also Really need tooltips.
   // /*
   // *
   // */
   // visualization.cytoscapeCxtmenu( {
   // menuRadius : 40, // the radius of the circular menu in pixels
   // selector : 'node', // nodes matching this Cytoscape.js selector will trigger cxtmenus
   // commands : [ // an array of commands to list in the menu
   //
   // /*
   // * { // example command content: 'a command name' // html/text content to be displayed in the menu select:
   // * function(){ // a function to execute when the command is selected console.log( this.id() ) // `this` holds the
   // * reference to the active node } }
   // */
   //
   // {
   // content : 'TESTING1',
   // select : function() {
   // console.log( this.data( 'officialName' ) );
   // }
   // }, {
   // content : 'TESTING2',
   // select : function() {
   // console.log( this.data( 'nodeDegree' ) );
   // }
   // } ],
   // fillColor : 'rgba(59, 59, 59, 0.75)', // the background colour of the menu
   // activeFillColor : 'rgba(92, 194, 237, 0.75)', // the colour used to indicate the selected command
   // activePadding : 2, // additional size in pixels for the active command
   // indicatorSize : null, // the size in pixels of the pointer to the active command
   // separatorWidth : 2, // the empty spacing in pixels between successive commands
   // spotlightPadding : 3, // extra spacing in pixels between the node and the spotlight
   // minSpotlightRadius : 24, // the minimum radius in pixels of the spotlight
   // maxSpotlightRadius : 26, // the maximum radius in pixels of the spotlight
   // itemColor : 'black', // the colour of text in the command's content
   // itemTextShadowColor : null, // the text shadow colour of the command's content
   // zIndex : 9999
   // // the z-index of the ui div
   // } );

};
