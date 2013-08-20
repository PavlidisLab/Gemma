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

Gemma.CytoscapePanelUtil = {};

Gemma.CytoscapePanelUtil.ttSubstring = function (tString) {

    if (!tString) {
        return null;
    }

    var maxLength = 60;

    if (tString.length > maxLength) {
        return tString.substr(0, maxLength) + "...";
    }

    return tString;
};


// inputs should be two node degrees between 0 and 1, if
// null(missing data) return 1 as nodes/edges with 1 fade
// into the background
Gemma.CytoscapePanelUtil.getMaxWithNull = function (n1, n2) {

    // missing data check
    if (n1 == null || n2 == null) {
        return 1;
    }

    return Math.max(n1, n2);

};

Gemma.CytoscapePanelUtil.decimalPlaceRounder = function (number) {

    if (number == null) {
        return null;
    }
    return Ext.util.Format.round(number, 4);

};

Gemma.CytoscapePanelUtil.nodeDegreeOpacityMapper = function (nodeDegree) {
	

    // no data for some genes
    if (nodeDegree == null) {
        return 0;
    }
    
    if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.lightest) {
        return Gemma.CytoscapeSettings.nodeDegreeOpacity.lightest;
    } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.light) {
        return Gemma.CytoscapeSettings.nodeDegreeOpacity.light;
    } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.moderate) {
        return Gemma.CytoscapeSettings.nodeDegreeOpacity.moderate;
    } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.dark) {
        return Gemma.CytoscapeSettings.nodeDegreeOpacity.dark;
    } else {
    	//darkest
        return Gemma.CytoscapeSettings.nodeDegreeOpacity.darkest;
    }

};

Gemma.CytoscapePanelUtil.nodeDegreeBinMapper = function (nodeDegree) {
	

    // no data for some genes
    if (nodeDegree == null) {
        return 0;
    }


    if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.lightest) {
        return Gemma.CytoscapeSettings.nodeDegreeColor.lightest.name;
    } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.light) {
        return Gemma.CytoscapeSettings.nodeDegreeColor.light.name;
    } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.moderate) {
        return Gemma.CytoscapeSettings.nodeDegreeColor.moderate.name;
    } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.dark) {
        return Gemma.CytoscapeSettings.nodeDegreeColor.dark.name;
    } else {
        return Gemma.CytoscapeSettings.nodeDegreeColor.darkest.name;
    }
    

};

Gemma.CytoscapePanelUtil.restrictResultsStringency = function (displayStringency) {
    if (displayStringency > 5) {
        return displayStringency - Math.round(displayStringency / 4);
    }

    return 2;
};

Gemma.CytoscapePanelUtil.getCoexVizCommandFromCoexGridCommand = function (csc) {
    var newCsc = {};

    Ext.apply( newCsc, {
        geneIds: csc.geneIds,
        eeIds: csc.eeIds,
        stringency: Gemma.CytoscapePanelUtil.restrictResultsStringency(csc.displayStringency),
        displayStringency: csc.displayStringency,
        forceProbeLevelSearch: csc.forceProbeLevelSearch,
        useMyDatasets: csc.useMyDatasets,
        queryGenesOnly: csc.queryGenesOnly,
        taxonId: csc.taxonId
    });

    return newCsc;
};

Gemma.CytoscapePanelUtil.restrictQueryGenesForCytoscapeQuery = function (searchResults) {
    function meetsStringency (coexPair, stringency) {
        return coexPair.posSupp >= stringency || coexPair.negSupp >= stringency;
    }

    function absent(element, array) {
        return array.indexOf(element) === -1;
    }

    var originalQueryGeneIds = searchResults.getQueryGeneIds();
    var originalCoexpressionPairs = searchResults.getDisplayedResults();

    // Genes to get complete results for.
    var geneIds = [];
//    coexpressionSearchData.cytoscapeCoexCommand.geneIds = [];

    var qlength = originalQueryGeneIds.length;
    var resultsPerQueryGene = Gemma.CytoscapeSettings.maxGeneIdsPerCoexVisQuery / qlength;

    var queryGeneCountHash = {};

    var i;
    for (i = 0; i < qlength; i++) {
        geneIds.push( originalQueryGeneIds[i] );
        queryGeneCountHash[ originalQueryGeneIds[i] ] = 0;
    }

    var kglength = originalCoexpressionPairs.length;

    // This needs to take in account the stringency of the forthcoming cytoscape query
    // so that nodes that are connected at lower stringency to the query gene are not included
    // only add to cytoscapeCoexCommand.geneIds if current query gene has room in its 'resultsPerQueryGeneCount' entry
    for (i = 0; i < kglength; i++) {

        if (meetsStringency(originalCoexpressionPairs[i], searchResults.getResultsStringency()) &&
            absent(originalCoexpressionPairs[i].foundGene.id, geneIds) &&
            queryGeneCountHash[originalCoexpressionPairs[i].queryGene.id] < resultsPerQueryGene )
        {
            geneIds.push(originalCoexpressionPairs[i].foundGene.id);
            queryGeneCountHash[originalCoexpressionPairs[i].queryGene.id] = queryGeneCountHash[originalCoexpressionPairs[i].queryGene.id] + 1;
        }
    }

    return geneIds;
};


Gemma.CytoscapePanelUtil.getGeneIdArrayFromCytoscapeJSONNodeObjects = function (selectedNodes) {

    var selectedNodesGeneIdArray = [];
    var sNodesLength = selectedNodes.length;
    var i;
    for (i = 0; i < sNodesLength; i++) {
        selectedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;
    }

    return selectedNodesGeneIdArray;

};

