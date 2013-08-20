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

Gemma.CoexpressionJSONUtils = {};

Gemma.CoexpressionJSONUtils.constructJSONGraphData= function (currentQueryGeneIds, knowngenes) {
    var elements = {
            nodes: [],
            edges: []
        };
        // helper array to prevent duplicate nodes from being
        // entered
        var graphNodeIds = [];
        var edgeSet = [];
        var kglength = knowngenes.length;
        var i;
        // populate node data plus populate edge data
        for (i = 0; i < kglength; i++) {

            
                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) === -1) {
                    isQueryGene = false;

                    if (currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1) {
                        isQueryGene = true;
                    }
                    
                    var data={data:{
                            id: knowngenes[i].foundGene.officialSymbol,
                            name: knowngenes[i].foundGene.officialSymbol,
                            geneid: knowngenes[i].foundGene.id,
                            queryflag: isQueryGene?1:0,
                            officialName: Gemma.CytoscapePanelUtil.ttSubstring(knowngenes[i].foundGene.officialName),
                            ncbiId: knowngenes[i].foundGene.ncbiId,
                            nodeDegreeOpacity: Gemma.CytoscapePanelUtil.nodeDegreeOpacityMapper(knowngenes[i].foundGeneNodeDegree),
                            nodeDegree: Gemma.CytoscapePanelUtil.decimalPlaceRounder(knowngenes[i].foundGeneNodeDegree)}
                        };

                    elements.nodes.push(data);

                    graphNodeIds.push(knowngenes[i].foundGene.id);
                }

                if (graphNodeIds.indexOf(knowngenes[i].queryGene.id) === -1) {
                    isQueryGene = false;

                    if (currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1) {
                        isQueryGene = true;
                    }
                    
                    var data = { data:{
                            id: knowngenes[i].queryGene.officialSymbol,
                            name: knowngenes[i].queryGene.officialSymbol,
                            geneid: knowngenes[i].queryGene.id,
                            queryflag: isQueryGene?1:0,                        
                            officialName: Gemma.CytoscapePanelUtil.ttSubstring(knowngenes[i].queryGene.officialName),
                            ncbiId: knowngenes[i].queryGene.ncbiId,
                            nodeDegreeOpacity: Gemma.CytoscapePanelUtil.nodeDegreeOpacityMapper(knowngenes[i].queryGeneNodeDegree),
                            nodeDegree: Gemma.CytoscapePanelUtil.decimalPlaceRounder(knowngenes[i].queryGeneNodeDegree)}
                        };

                    elements.nodes.push(data);
                    graphNodeIds.push(knowngenes[i].queryGene.id);
                }

                var support;
                var supportsign;
                if (knowngenes[i].posSupp > 0 && knowngenes[i].negSupp > 0) {
                    support = Math.max(knowngenes[i].posSupp, knowngenes[i].negSupp);
                    supportsign = "both";

                } else if (knowngenes[i].posSupp > 0) {
                    support = knowngenes[i].posSupp;
                    supportsign = "positive";
                }else {// if (knowngenes[i].negSupp > 0) {
                    support = knowngenes[i].negSupp;
                    supportsign = "negative";
                }
                // double edge check
                if (edgeSet.indexOf(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol) == -1 && edgeSet.indexOf(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol) == -1) {

                	var nodeDegreeValue = Gemma.CytoscapePanelUtil.decimalPlaceRounder(Gemma.CytoscapePanelUtil.getMaxWithNull(
                            knowngenes[i].queryGeneNodeDegree, knowngenes[i].foundGeneNodeDegree));
                	
                	var data = {data:{
                            id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
                            target: knowngenes[i].foundGene.officialSymbol,
                            source: knowngenes[i].queryGene.officialSymbol,
                            positiveSupport: knowngenes[i].posSupp,
                            negativeSupport: knowngenes[i].negSupp,
                            support: support,
                            supportSign: supportsign,
                            nodeDegree: nodeDegreeValue,
                            nodeDegreeOpacity:Gemma.CytoscapePanelUtil.nodeDegreeOpacityMapper(knowngenes[i].foundGeneNodeDegree)}
                        }; 
                	
                    elements.edges.push(data);
                    edgeSet.push(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol);
                    edgeSet.push(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol);
                }
           
        } // end for (<kglength)

        return elements;
    };
