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
Ext.namespace( 'Gemma' );

/**
 * Style and layout parameters for cytoscape.js
 */
Gemma.CytoscapeSettings = {

   // white, for the display bkg
   backgroundColor : "#FFFFFF",

   // node color stuff
   labelFontName : 'Arial',

   // grey
   labelFontColor : "#4F4F4F",

   // grey; modified by 'overlays' and 'emphasis'
   nodeColor : "#222222",

   nodeColorOverlay : "#66ACDE", // baby blue

   // size stuff (edge size is in the cytoscapeJSCoexGraphInitializer)
   labelFontSize : 12,

   nodeSize : 18,

   nodeQueryColorTrue : "#AB1AE4", // purple

   // edge color stuff
   supportColorBoth : "#CCCCCC", // grey

   supportColorPositive : "#059900", // greenish
   supportColorNegative : "#E81C7B", // reddish

   selectionGlowColor : "#CC9C00", // gold

   // inoperative in cytoscape.js as of 2.2
   // nodeTooltipText : "${id} (${officialName})<br/>Specificity:${nodeDegreeBin}<br/>NCBI Id:${ncbiId}<br/>",

   // inoperative in cytoscape.js as of 2.2
   // edgeTooltipText : "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative
   // Support:${negativesupport}",

   // thresholds e.g. dark colour is >dark value and <moderate value
   // high values mean high node degree.
   nodeDegreeValue : {
      highest : 0.9,
      high : 0.8,
      moderate : 0.7,
      low : 0.5,
      lowest : 0.3
   },

   // Shades. We're blending in white, so low numbers translate to
   // dark. Note: alpha is much slower.
   nodeDegreeColor : {
      lightest : 0.85,
      light : 0.65,
      moderate : 0.5,
      dark : 0.35,
      darkest : 0.0
   }

};

/**
 * Todo: make negative correlations cause repulsion?
 */
Gemma.CytoscapejsSettings = {

   nodeSettings : {},

   coseLayout : {
      name : 'cose',
      refresh : 0,
      liveUpdate : false,
      fit : true,
   },

   // See also http://arborjs.org/reference
   arborLayout : {
      name : 'arbor',
      liveUpdate : false,
      repulsion : 1000,
      stiffness : 600,
      maxSimulationTime : 4000,
      friction : 0.5,
      gravity : true,
      fps : 12,
      precision : 0.1,
      fit : true
   }

};
