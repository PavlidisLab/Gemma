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

Gemma.CoexpressionJSONUtils = {};

/**
 * Convert our 'native' objects into Cytoscape data objects. The value generated becomes the 'elements' of the
 * cytoscape() initialization.
 * <p>
 * See http://cytoscape.github.io/cytoscape.js/#notation/elements-json.
 * 
 * @param {Array}
 *           currentQueryGeneIds
 * @param {Array}
 *           results from the server (CoexpressionValueObjectExts)
 */
Gemma.CoexpressionJSONUtils.constructJSONGraphData = function( currentQueryGeneIds, results ) {
   var elements = [];

   // helper array to prevent duplicate nodes from being entered
   var graphNodeIds = {};
   var edgeSet = {};

   // populate node data plus populate edge data
   for ( var i = 0; i < results.length; i++) {

      var r = results[i];

      if ( !graphNodeIds[r.foundGene.id] ) {
         var isQueryGene = currentQueryGeneIds.indexOf( r.foundGene.id ) !== -1;

         var data = {
            group : 'nodes',
            data : {
               id : r.foundGene.officialSymbol,
               name : r.foundGene.officialSymbol,
               geneid : r.foundGene.id,
               queryflag : isQueryGene ? 1 : 0,
               officialName : Gemma.CytoscapePanelUtil.ttSubstring( r.foundGene.officialName ),
               ncbiId : r.foundGene.ncbiId,
               nodeDegreeColor : Gemma.CytoscapePanelUtil.nodeDegreeColorMapper( r.foundGeneNodeDegreeRank, 'node' ),
               nodeDegree : Gemma.CytoscapePanelUtil.decimalPlaceRounder( r.foundGeneNodeDegreeRank )
            }
         };

         elements.push( data );

         graphNodeIds[r.foundGene.id] = 1;
      } else {
         // many duplicates.
         // console.log( "duplicate: " + r.foundGene.officialSymbol );
      }

      if ( !graphNodeIds[r.queryGene.id] ) {
         var isQueryGene = currentQueryGeneIds.indexOf( r.queryGene.id ) !== -1;

         var data = {
            group : 'nodes',
            data : {
               id : r.queryGene.officialSymbol,
               name : r.queryGene.officialSymbol,
               geneid : r.queryGene.id,
               queryflag : isQueryGene ? 1 : 0,
               officialName : Gemma.CytoscapePanelUtil.ttSubstring( r.queryGene.officialName ),
               ncbiId : r.queryGene.ncbiId,
               nodeDegreeColor : Gemma.CytoscapePanelUtil.nodeDegreeColorMapper( r.queryGeneNodeDegreeRank, 'node' ),
               nodeDegree : Gemma.CytoscapePanelUtil.decimalPlaceRounder( r.queryGeneNodeDegreeRank )
            }
         };

         elements.push( data );
         graphNodeIds[r.queryGene.id] = 1;
      }

      /*
       * Edges.
       */

      // double edge check (fixme, this is rather inefficient-looking; and we shouldn't have duplicates, right?)
      var e1 = r.foundGene.officialSymbol + "to" + r.queryGene.officialSymbol;
      var e2 = r.queryGene.officialSymbol + "to" + r.foundGene.officialSymbol;

      // for edges we have not yet seen,
      if ( edgeSet[e1] || edgeSet[e2] ) {
         continue;
      }

      var support = r.support;
      var supportsign;
      if ( r.posSupp > 0 && r.negSupp > 0 ) {
         supportsign = "both";
      } else if ( r.posSupp > 0 ) {
         supportsign = "positive";
      } else {
         supportsign = "negative";
      }

      // compute the nodeDegree of the highest (worst) of the two. Low values are "good".
      var nodeDegreeValue = Gemma.CytoscapePanelUtil.decimalPlaceRounder( Gemma.CytoscapePanelUtil.getMaxWithNull(
         r.queryGeneNodeDegreeRank, r.foundGeneNodeDegreeRank ) );

      var data = {
         group : 'edges',
         data : {
            id : r.foundGene.officialSymbol + "to" + r.queryGene.officialSymbol,
            target : r.foundGene.officialSymbol,
            source : r.queryGene.officialSymbol,
            positiveSupport : r.posSupp,
            negativeSupport : r.negSupp,
            support : support,
            supportSign : supportsign,
            nodeDegree : nodeDegreeValue,
            nodeDegreeColor : Gemma.CytoscapePanelUtil.nodeDegreeColorMapper( nodeDegreeValue, supportsign ),
         }
      };

      elements.push( data );
      edgeSet[e1] = 1;
      edgeSet[e2] = 1;

   } // end for (<results.length)

   return elements;
};
