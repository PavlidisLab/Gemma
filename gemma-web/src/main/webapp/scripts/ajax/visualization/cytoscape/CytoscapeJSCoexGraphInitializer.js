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
Ext.namespace('Gemma');


Gemma.CytoscapeJSCoexGraphInitializer = function (visualization, graphData, readyFunction, ownerRef) {
	
	visualization.cytoscape({
		
		showOverlay: false,
		  	  
    	style: Gemma.getCytoscapeJSNodeDegreeEmphasisStyle(cytoscape.stylesheet()),
    	
    	layout: Gemma.CytoscapejsSettings.arborLayout,
    	
    	renderer: { selectionToPanDelay: 500,
    		  dragToSelect: true,
    		  dragToPan: true },
      
    	elements: graphData,
    	  
    	  ready: function(){ 
    		  
    		ownerRef.cy = this;
    	    
    	    readyFunction(ownerRef);
    	    
    	  }
    	});
    
};

Gemma.getCytoscapeJSNodeDegreeEmphasisStyle = function (stylesheet){
	
	return stylesheet.selector('node')
	  .css({
	    'content': 'data(name)',
	    'font-family': Gemma.CytoscapeSettings.labelFontName,
	    'font-size': Gemma.CytoscapeSettings.labelFontSize,
	    'text-opacity':'data(nodeDegreeOpacity)',
	    //'text-outline-width': 1,
	    //'text-outline-color': '#888',
	    'text-valign': 'center',
	    'color':  Gemma.CytoscapeSettings.labelFontColor,
	    'width': Gemma.CytoscapeSettings.nodeSize,
	    'height': Gemma.CytoscapeSettings.nodeSize,
	    'border-color': Gemma.CytoscapeSettings.nodeQueryColorTrue,
	    'border-width': 'mapData(queryflag,0,1,0,3)',
	    
	    //node color	    
	    'background-color': '#000000',
	    'background-opacity':'data(nodeDegreeOpacity)',
	    'text-valign': 'top',
	    'visibility': 'hidden' //filtering will show appropriate nodes
	  })
	.selector(':selected')
	  .css({
	    'background-color': Gemma.CytoscapeSettings.selectionGlowColor,
	    'line-color': '#000',
	    'target-arrow-color': '#000',
	    'text-outline-color': '#000'
	  })
	.selector('edge')
	  .css({
	    'width': 'mapData(support,2,100,1,3)',	    		    
	    'opacity' : 'data(nodeDegreeOpacity)'
	  }).selector("edge[supportSign='positive']").css({'line-color': Gemma.CytoscapeSettings.supportColorPositive})
	  .selector("edge[supportSign='negative']").css({'line-color': Gemma.CytoscapeSettings.supportColorNegative})
	  .selector("edge[supportSign='both']").css({'line-color': Gemma.CytoscapeSettings.supportColorBoth});

};

Gemma.getCytoscapeJSDefaultStyle = function (stylesheet){
	
	return stylesheet.selector('node')
	  .css({
		    'content': 'data(name)',
		    //the text color
		    'color':  Gemma.CytoscapeSettings.labelFontColor,
		    'font-family': Gemma.CytoscapeSettings.labelFontName,
		    'font-size': Gemma.CytoscapeSettings.labelFontSize,
		    
		    'text-valign': 'center',
		    
		    'width': Gemma.CytoscapeSettings.nodeSize,
		    'height': Gemma.CytoscapeSettings.nodeSize,
		    'border-color': Gemma.CytoscapeSettings.nodeQueryColorTrue,
		    'border-width': 'mapData(queryflag,0,1,0,3)',
		    
		    //node color	    
		    'background-color': Gemma.CytoscapeSettings.nodeColor,
		    //'background-opacity':'data(nodeDegreeOpacity)',
		    'text-valign': 'top' ,
		    'visibility': 'hidden'
		  })
		.selector(':selected')
		  .css({
		    'background-color': Gemma.CytoscapeSettings.selectionGlowColor,
		    'line-color': '#000',	    
		    'text-outline-color': '#000'
		  })
		.selector('edge')
		  .css({
		    'width': 'mapData(support,2,100,1,3)',	    
		  }).selector("edge[supportSign='positive']").css({'line-color': Gemma.CytoscapeSettings.supportColorPositive})
		  .selector("edge[supportSign='negative']").css({'line-color': Gemma.CytoscapeSettings.supportColorNegative})
		  .selector("edge[supportSign='both']").css({'line-color': Gemma.CytoscapeSettings.supportColorBoth});
	
};

Gemma.applyCytoscapeJSNodeDegreeEmphasisStyle = function (viz){
	
	Gemma.getCytoscapeJSNodeDegreeEmphasisStyle(viz.style().resetToDefault()).update();	
	
};

Gemma.applyCytoscapeJSDefaultStyle = function (viz){
	
	Gemma.getCytoscapeJSDefaultStyle(viz.style().resetToDefault()).update();			
	
};
