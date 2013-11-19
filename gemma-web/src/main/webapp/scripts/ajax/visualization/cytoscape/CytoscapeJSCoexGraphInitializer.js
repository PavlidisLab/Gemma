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

Gemma.CytoscapeJSCoexGraphInitializer = function(visualization, graphData, readyFunction, ownerRef) {

	visualization.cytoscape({

		showOverlay : false,
		fit: false,

		style : cytoscape.stylesheet().selector('node.emphasis').css({
			'content' : 'data(name)',
			'font-family' : Gemma.CytoscapeSettings.labelFontName,
			'font-size' : Gemma.CytoscapeSettings.labelFontSize,
			'text-opacity' : 'data(nodeDegreeOpacity)',
			// 'text-outline-width': 1,
			// 'text-outline-color': '#888',
			'text-valign' : 'center',
			'color' : Gemma.CytoscapeSettings.labelFontColor,
			'width' : Gemma.CytoscapeSettings.nodeSize,
			'height' : Gemma.CytoscapeSettings.nodeSize,
			'border-color' : Gemma.CytoscapeSettings.nodeQueryColorTrue,
			'border-width' : 'mapData(queryflag,0,1,0,13)',

			// node color
			'background-color' : '#000000',
			'background-opacity' : 'data(nodeDegreeOpacity)',
			'text-valign' : 'top',
			'visibility' : 'hidden' // filtering will show appropriate nodes
		}).selector('node.basic').css({
			'content' : 'data(name)',
			// the text color
			'color' : Gemma.CytoscapeSettings.labelFontColor,
			'font-family' : Gemma.CytoscapeSettings.labelFontName,
			'font-size' : Gemma.CytoscapeSettings.labelFontSize,

			'text-valign' : 'center',

			'width' : Gemma.CytoscapeSettings.nodeSize,
			'height' : Gemma.CytoscapeSettings.nodeSize,
			'border-color' : Gemma.CytoscapeSettings.nodeQueryColorTrue,
			'border-width' : 'mapData(queryflag,0,1,0,13)',

			// node color
			'background-color' : Gemma.CytoscapeSettings.nodeColor,
			// 'background-opacity':'data(nodeDegreeOpacity)',
			'text-valign' : 'top',
			'visibility' : 'hidden'
		}).selector('node.overlay').css({
			'content' : 'data(name)',
			// the text color
			'color' : Gemma.CytoscapeSettings.nodeColorOverlay,
			'font-family' : Gemma.CytoscapeSettings.labelFontName,
			'font-size' : Gemma.CytoscapeSettings.labelFontSize,
			'text-valign' : 'center',
			'width' : Gemma.CytoscapeSettings.nodeSize,
			'height' : Gemma.CytoscapeSettings.nodeSize,
			'border-color' : Gemma.CytoscapeSettings.nodeQueryColorTrue,
			'border-width' : 'mapData(queryflag,0,1,0,13)',

			// node color
			'background-color' : Gemma.CytoscapeSettings.nodeColorOverlay,
			// 'background-opacity':'data(nodeDegreeOpacity)',
			'text-valign' : 'top',
			'visibility' : 'hidden'
		}).selector(':selected').css({
			'background-color' : Gemma.CytoscapeSettings.selectionGlowColor,
			'line-color' : '#000',
			'target-arrow-color' : '#000',
			'text-outline-color' : '#000'
		}).selector('edge.emphasis').css({
			'width' : 'mapData(support,2,250,5,100)',
			'opacity' : 'data(nodeDegreeOpacity)'
		}).selector('edge.basic').css({
			'width' : 'mapData(support,2,250,5,100)'
		}).selector("edge[supportSign='positive']").css({
			'line-color' : Gemma.CytoscapeSettings.supportColorPositive
		}).selector("edge[supportSign='negative']").css({
			'line-color' : Gemma.CytoscapeSettings.supportColorNegative
		}).selector("edge[supportSign='both']").css({
			'line-color' : Gemma.CytoscapeSettings.supportColorBoth
		}),

		//use grid for initialization because firefox was bugging out in version 2.0.3 with arbor on init.  Set to arbor after grid finishes layout as workaround
		layout: { name: 'grid' },

		renderer : {
			selectionToPanDelay : 350,
			dragToSelect : true,
			dragToPan : true
		},

		elements : graphData,

		ready : function() {

			ownerRef.cy = this;

			readyFunction(ownerRef);

		}
	});

};


