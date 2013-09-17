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
Ext.namespace('Gemma');

//TODO remove unnecessary settings from old cytoscape web implementation

Gemma.CytoscapeSettings = {

   backgroundColor : "#FFF7FB",

   // node stuff
   labelFontName : 'Arial',
   labelFontColor : "#252525",
   labelFontColorFade : "#BDBDBD",
   labelGlowStrength : 100,
   labelFontWeight : "bold",
   labelFontSize : 150,

   labelFontSizeBigger : 18,
   labelFontSizeBiggest : 25,

   labelYOffset : -20,

   labelHorizontalAnchor : "center",

   nodeColor : "#969696",
   nodeColorFade : "#FFF7FB",
   nodeColorOverlay : "#000099",
   borderWidth : 'mapData(queryflag,0,1,0,25)',
   edgeWidth: 'mapData(support,2,100,10,30)',
     
   nodeSize : 200,
   queryNodeSize : 200,

   nodeQueryColorTrue : "#E41A1C",
   nodeQueryColorFalse : "#6BAED6",

   // edge stuff
   supportColorBoth : "#CCCCCC",
   supportColorPositive : "#E66101",
   supportColorNegative : "#5E3C99",

   selectionGlowColor : "#00CC00",

   selectionGlowOpacity : 1,

   zoomLevelBiggerFont : 0.7,
   zoomLevelBiggestFont : 0.4,

   maxGeneIdsPerCoexVisQuery : 200,

   nodeTooltipText : "${id} (${officialName})<br/>Specificity:${nodeDegreeBin}<br/>NCBI Id:${ncbiId}<br/>",

   edgeTooltipText : "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",

   // e.g. dark colour is >dark value and <moderate value, darkest is <=0.2
   // darkest : most specificity, lightest: least specificity
   nodeDegreeValue : {
      lightest : 0.7,
      light : 0.6,
      moderate : 0.35,
      dark : 0.2
   },
   
   nodeDegreeOpacity : {
	      lightest : 0.1,
	      light : 0.3,
	      moderate : 0.5,
	      dark : 0.65,
	      darkest : 1
	   },

   // note that high node degree means low specificity(see nodeDegreeValue above)
   nodeDegreeColor : {
      lightest : {
         name : "Lowest",
         value : "#DEDEDE"
      },
      light : {
         name : "Low",
         value : "#C9C9C9"
      },
      moderate : {
         name : "Moderate",
         value : "#737373"
      },
      dark : {
         name : "High",
         value : "#404040"
      },
      darkest : {
         name : "Highest",
         value : "#000000"
      }

   },

   nodeDegreeColorSecondGeneList : {
      lightest : {
         value : "#B2B2FF"
      },
      light : {
         value : "#8080FF"
      },
      moderate : {
         value : "#4D4DFF"
      },
      dark : {
         value : "#0000FF"
      },
      darkest : {
         value : "#000099"
      }

   }

};


Gemma.CytoscapejsSettings = {
		
		nodeSettings:{
			
		},
		
		arborLayout : {
			    name: 'arbor',
			    liveUpdate: false,
			    
			    //edgeLength:100,
			    nodeMass: 20,
			    
			    maxSimulationTime: 6000,
			    repulsion: 100,
			    //stiffness:100,
			    //gravity: false,
			    friction:0.2,
			    fit: false,
			    simulationBounds: [0, 0, 10000, 10000],
			    
			    padding: [ 0, 0, 0, 0 ]
		
			    /*nodeMass: function(data){
			    	
			    	if (data.nodeDegree < 0.4) return 2;
			        return 50; // use the weight attribute in the node's data as mass
			    },
			    
			    repulsion: 1500
				/*,
			    repulsion: 1, // whether to show the layout as it's running
			    ready: undefined, // callback on layoutready 
			    stop: undefined, // callback on layoutstop
			    maxSimulationTime: 5000, // max length in ms to run the layout
			    fit: true, // fit to viewport
			    padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
			    ungrabifyWhileSimulating: true, // so you can't drag nodes during layout

			    // forces used by arbor (use arbor default on undefined)
			    //defaults:
			    //repulsion: 600,
			    //stiffness: 1000,
			    //friction: 0.3,
			    //gravity: false,
			    
			    repulsion: 1,
			    stiffness: 1,
			    friction: 0.1,
			    gravity: true,
			    
			    fps: undefined,
			    
			    precision: undefined,

			    // static numbers or functions that dynamically return what these
			    // values should be for each element
			    //nodeMass: 2, 
			    edgeLength: undefined,

			    stepSize: 1, // size of timestep in simulation

			    // function that returns true if the system is stable to indicate
			    // that the layout can be stopped
			    //stableEnergy: function( energy ){
			    //    var e = energy; 
			    //    return (e.max <= 0.1) || (e.mean <= 0.05);
			    //}*/
			}
		
};






Gemma.CytoscapeSettings.defaultForceDirectedLayout = {
   name : "ForceDirected"
};

Gemma.CytoscapeSettings.secondGeneListBypassOverlay = {
   color : "#4D4DFF",
   labelGlowStrength : 240,
   labelFontColor : "#0000FF",
   labelFontStyle : "italic",
   labelFontWeight : "bold"
};



