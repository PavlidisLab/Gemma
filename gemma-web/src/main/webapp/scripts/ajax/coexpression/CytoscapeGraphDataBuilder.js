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

/**
 * 
 * 
 * 
 */
Gemma.CytoscapeGraphDataBuilder = {

   dataSchemaJSON : {
      nodes : [{
            name : 'label',
            type : 'string'
         }, {
            name : 'geneid',
            type : 'number'
         }, {
            name : 'queryflag',
            type : 'boolean'
         }, {
            name : 'nodeDegree',
            type : 'number'
         }, {
            name : 'nodeDegreeBin',
            type : 'string'
         }, {
            name : 'officialName',
            type : 'string'
         }, {
            name : 'ncbiId',
            type : 'number'
         }],
      edges : [{
            name : 'positivesupport',
            type : 'number'
         }, {
            name : 'negativesupport',
            type : 'number'
         }, {
            name : 'support',
            type : 'number'
         }, {
            name : 'supportsign',
            type : 'string'
         }, {
            name : 'nodeDegree',
            type : 'number'
         }]
   },

   /**
    * Constructs graph (in a format that cytoscape understands).
    * 
    * @param queryGeneIds
    * @param coexPairs
    */
   constructGraphData : function(queryGeneIds, coexPairs) {
      var graph = {
         nodes : [],
         edges : []
      };

      // Helper object to build the graph.
      var graphBuilder = {
         /* helper arrays to prevent duplicate nodes, edges */
         _geneIdsOfAlreadyCreatedNodes : [],
         _edgeSet : [],

         isGenePresentInGraph : function(gene) {
            return (this._geneIdsOfAlreadyCreatedNodes.indexOf(gene.id) !== -1);
         },

         addNode : function(node) {
            graph.nodes.push(node);
            this._geneIdsOfAlreadyCreatedNodes.push(node.geneid);
         },

         isEdgePresentInGraph : function(genePair) {
            return (this._edgeSet.indexOf(genePair.queryGene.officialSymbol + "to" + genePair.foundGene.officialSymbol) !== -1);
         },

         addEdge : function(edge) {
            graph.edges.push(edge);

            // Edges going in the opposite direction are equivalent, so we keep track of both.
            this._edgeSet.push(coexPairs[i].foundGene.officialSymbol + "to" + coexPairs[i].queryGene.officialSymbol);
            this._edgeSet.push(coexPairs[i].queryGene.officialSymbol + "to" + coexPairs[i].foundGene.officialSymbol);
         }
      };

      function makeNode(gene) {
         return {
            id : gene.id.toString(),
            label : gene.officialSymbol,
            geneid : gene.id,
            queryflag : isInQueryGeneSet(gene),
            officialName : Gemma.CytoscapePanelUtil.ttSubstring(gene.officialName),
            ncbiId : gene.ncbiId,
            nodeDegreeBin : Gemma.CytoscapePanelUtil.nodeDegreeBinMapper(gene.nodeDegree),
            nodeDegree : Gemma.CytoscapePanelUtil.decimalPlaceRounder(gene.nodeDegree)
         };
      }

      function makeEdge(genePair) {
         var support = extractCoexpressionSupport(genePair);
         return {
            id : genePair.foundGene.officialSymbol + "to" + genePair.queryGene.officialSymbol,
            target : genePair.foundGene.id.toString(),
            source : genePair.queryGene.id.toString(),
            positivesupport : genePair.posSupp,
            negativesupport : genePair.negSupp,
            support : support.value,
            supportsign : support.sign,
            nodeDegree : Gemma.CytoscapePanelUtil.decimalPlaceRounder(Gemma.CytoscapePanelUtil.getMaxWithNull(genePair.queryGene.nodeDegree, genePair.foundGene.nodeDegree))
         };
      }

      function isInQueryGeneSet(gene) {
         return (queryGeneIds.indexOf(gene.id) !== -1);
      }

      function extractCoexpressionSupport(genePair) {
         var value;
         var sign;

         if (genePair.posSupp > 0 && genePair.negSupp > 0) {
            value = Math.max(genePair.posSupp, genePair.negSupp);
            sign = "both";

         } else if (genePair.posSupp > 0) {
            value = genePair.posSupp;
            sign = "positive";
         } else if (genePair.negSupp > 0) {
            value = genePair.negSupp;
            sign = "negative";
         }
         return {
            'value' : value,
            'sign' : sign
         };
      }

      for (var i = 0; i < coexPairs.length; i++) {
         // Unpack coexpression gene pair.
         var firstGene = coexPairs[i].queryGene;
         var secondGene = coexPairs[i].foundGene;

         // TODO: map that to gene right after search results are returned...
         firstGene.nodeDegree = coexPairs[i].queryGeneNodeDegree;
         secondGene.nodeDegree = coexPairs[i].foundGeneNodeDegree;

         // Add nodes to the graph if not already present.
         if (!graphBuilder.isGenePresentInGraph(firstGene)) {
            graphBuilder.addNode(makeNode(firstGene));
         }
         if (!graphBuilder.isGenePresentInGraph(secondGene)) {
            graphBuilder.addNode(makeNode(secondGene));
         }

         // Add edge if not already present.
         if (!graphBuilder.isEdgePresentInGraph(coexPairs[i])) {
            graphBuilder.addEdge(makeEdge(coexPairs[i]));
         }
      }

      return {
         dataSchema : this.dataSchemaJSON,
         data : graph
      };
   }
};
