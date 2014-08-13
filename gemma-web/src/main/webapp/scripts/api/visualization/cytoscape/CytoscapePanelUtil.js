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
var Color = net.brehaut.Color;
Gemma.CytoscapePanelUtil = {};

/**
 * For tool tips, abbreviate.
 */
Gemma.CytoscapePanelUtil.ttSubstring = function( tString ) {

   if ( !tString ) {
      return null;
   }

   var maxLength = 60;
   var endOnWord = true;
   return Ext.util.Format.ellipsis( tString, maxLength, endOnWord );
};

/**
 * 
 */
Gemma.CytoscapePanelUtil.getMaxWithNull = function( n1, n2 ) {
   // missing data check
   if ( n1 === null || n2 === null ) {
      return 1.0;
   }
   return Math.max( n1, n2 );
};

Gemma.CytoscapePanelUtil.decimalPlaceRounder = function( number ) {
   if ( number == null ) {
      return null;
   }
   return Ext.util.Format.round( number, 4 );
};

/**
 * Given a node degree rank, decide what color it should be (hex value)
 * 
 * @param nodeDegree,
 *           a relative ranking such that low values represent low node degrees.
 * @param type
 *           either for nodes, either 'node' or 'overlay', or for edges one of ['positive', 'negative', 'both'].
 */
Gemma.CytoscapePanelUtil.nodeDegreeColorMapper = function( nodeDegree, type ) {

   // no data for some genes
   if ( nodeDegree == null ) {
      return Gemma.CytoscapeSettings.nodeDegreeColor.lightest;
   }

   var base = {};
   var blend = Color( "rgb(255,255,255)" );

   // figure out base colour
   if ( type === 'node' ) {
      base = Color( Gemma.CytoscapeSettings.nodeColor );
   } else if ( type === 'overlay' ) {
      // not used yet - might be okay to just always use the overlay as is
      base = Color( Gemma.CytoscapeSettings.nodeColorOverlay );
   } else if ( type === 'positive' ) {
      base = Color( Gemma.CytoscapeSettings.supportColorPositive );
   } else if ( type === 'negative' ) {
      base = Color( Gemma.CytoscapeSettings.supportColorNegative );
   } else if ( type === 'both' ) {
      base = Color( Gemma.CytoscapeSettings.supportColorBoth );
   } else {
      // assume the worst.
      return Gemma.CytoscapeSettings.nodeDegreeColor.lightest;
   }

   // return tint - so worst rank gets lightest color; low values are "good".
   // we're going to use.
   if ( nodeDegree < Gemma.CytoscapeSettings.nodeDegreeValue.lowest ) {
      return base.toCSS();
   } else if ( nodeDegree < Gemma.CytoscapeSettings.nodeDegreeValue.low ) {
      return base.blend( blend, Gemma.CytoscapeSettings.nodeDegreeColor.dark ).toCSS();
   } else if ( nodeDegree < Gemma.CytoscapeSettings.nodeDegreeValue.moderage ) {
      return base.blend( blend, Gemma.CytoscapeSettings.nodeDegreeColor.moderate ).toCSS();
   } else if ( nodeDegree < Gemma.CytoscapeSettings.nodeDegreeValue.high ) {
      return base.blend( blend, Gemma.CytoscapeSettings.nodeDegreeColor.light ).toCSS();
   } else { // highest
      return base.blend( blend, Gemma.CytoscapeSettings.nodeDegreeColor.lightest ).toCSS();
   }

};

/**
 * @param {int}
 *           displayStringency
 */
Gemma.CytoscapePanelUtil.restrictResultsStringency = function( displayStringency ) {
   // FIXME explain/adjust this heuristic. The idea is we reduce the stringency a little so the user can adjust the view
   // without triggering a requery.
   // if ( displayStringency > 5 ) {
   // return displayStringency - Math.round( displayStringency / 4 );
   // }
   return displayStringency;
};

/**
 * 
 */
Gemma.CytoscapePanelUtil.getCoexVizCommandFromCoexGridCommand = function( csc ) {
   var newCsc = {};

   Ext.apply( newCsc, {
      geneIds : csc.geneIds,
      eeIds : csc.eeIds,
      stringency : Gemma.CytoscapePanelUtil.restrictResultsStringency( csc.displayStringency ),
      displayStringency : csc.displayStringency,
      useMyDatasets : csc.useMyDatasets,
      queryGenesOnly : csc.queryGenesOnly,
      taxonId : csc.taxonId
   } );

   return newCsc;
};

/**
 * When we go from the table view to the visualization, we need to collect the genes to query - not just the query genes
 * but the found genes.
 * 
 * @param {}
 *           searchResults.
 */
Gemma.CytoscapePanelUtil.restrictQueryGenesForCytoscapeQuery = function( searchResults ) {

   // function meetsStringency( coexPair, stringency ) {
   // return coexPair.posSupp >= stringency || coexPair.negSupp >= stringency;
   // }

   function absent( element, array ) {
      return array.indexOf( element ) === -1;
   }

   var originalQueryGeneIds = searchResults.getQueryGeneIds();
   var originalCoexpressionPairs = searchResults.getResults();

   // Genes to get complete results for.
   var geneIds = [];

   var qlength = originalQueryGeneIds.length;

   // var resultsPerQueryGene = Gemma.CytoscapeSettings.maxGeneIdsPerCoexVisQuery / qlength;

   // var queryGeneCountHash = {};

   // always keep the query genes
   for (var i = 0; i < qlength; i++) {
      geneIds.push( originalQueryGeneIds[i] );
      // queryGeneCountHash[originalQueryGeneIds[i]] = 0;
   }

   // decide whether to add the 'found gene'.
   // This needs to take in account the stringency of the forthcoming cytoscape query
   // so that nodes that are connected at lower stringency to the query gene are not included
   // only add to cytoscapeCoexCommand.geneIds if current query gene has room in its 'resultsPerQueryGeneCount' entry
   for (var i = 0; i < originalCoexpressionPairs.length; i++) {
      var coexpPair = originalCoexpressionPairs[i];

      var nodeDegreeRank = coexpPair.foundGeneNodeDegreeRank;

      /*
       * Removing genes is confusing ... but it must be done. However, we should do this based on specificity. Found
       * genes that have low relative specificity (e.g., >0.9) should be culled. But we shouldn't use stringency or
       * number of edges to do this (though there will likely be a relationship). Perhaps such 'non-specific' links
       * should be removed from the results in the first place... that might be less confusing for users.
       */
      if ( /*
             * meetsStringency( coexpPair, searchResults.getResultsStringency() ) &&
             */absent( coexpPair.foundGene.id, geneIds ) && nodeDegreeRank < 0.8
      /* && queryGeneCountHash[coexpPair.queryGene.id] < resultsPerQueryGene */) {

         geneIds.push( coexpPair.foundGene.id );

         // queryGeneCountHash[coexpPair.queryGene.id] = queryGeneCountHash[coexpPair.queryGene.id] + 1;
      }
   }

   return geneIds;
};

/**
 * 
 */
Gemma.CytoscapePanelUtil.getGeneIdArrayFromCytoscapeJSONNodeObjects = function( selectedNodes ) {

   var selectedNodesGeneIdArray = [];
   var sNodesLength = selectedNodes.length;
   var i;
   for (i = 0; i < sNodesLength; i++) {
      selectedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;
   }

   return selectedNodesGeneIdArray;

};
