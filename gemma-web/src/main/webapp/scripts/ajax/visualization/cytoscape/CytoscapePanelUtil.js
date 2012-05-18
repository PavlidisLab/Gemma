Ext.namespace('Gemma');

Gemma.CytoscapePanelUtil = {};

Gemma.CytoscapePanelUtil.ttSubstring= function (tString) {

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

Gemma.CytoscapePanelUtil.nodeDegreeBinMapper = function (nodeDegree) {

	 // no data for some genes
	 if (nodeDegree == null) {
	     return null;
	 }	

	 
	 if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.lightest) {
	     return Gemma.CytoscapeSettings.nodeDegreeColor.lightest.name;
	 } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.light) {
		 return Gemma.CytoscapeSettings.nodeDegreeColor.light.name;
	 } else if (nodeDegree > Gemma.CytoscapeSettings.nodeDegreeValue.dark) {
		 return Gemma.CytoscapeSettings.nodeDegreeColor.dark.name;
	 } else  {
		 return Gemma.CytoscapeSettings.nodeDegreeColor.darkest.name;
	 } 
     
};

Gemma.CytoscapePanelUtil.restrictResultsStringency = function (displayStringency) {
	
	 if (displayStringency > 5) {
         return displayStringency - Math.round(displayStringency / 4);
     }
	 
	 return 2;
	
	
};

Gemma.CytoscapePanelUtil.getCoexVizCommandFromCoexGridCommand = function (csc){
	
	var newCsc = {};
	
	Ext.apply(newCsc, {
				geneIds : csc.geneIds,
				eeIds: csc.eeIds,				
				stringency : Gemma.CytoscapePanelUtil.restrictResultsStringency(csc.displayStringency),
				displayStringency : csc.displayStringency,
				forceProbeLevelSearch : csc.forceProbeLevelSearch,
				useMyDatasets : csc.useMyDatasets,
				queryGenesOnly : csc.queryGenesOnly,
				taxonId : csc.taxonId
			});
	
	return newCsc;
	
	
};


Gemma.CytoscapePanelUtil.restrictQueryGenesForCytoscapeQuery = function (coexpressionSearchData){
	
	coexpressionSearchData.cytoscapeCoexCommand.geneIds = [];
	
    var qlength = coexpressionSearchData.coexGridResults.queryGenes.length;
    
    var queryGeneCountHash = {};
    
    var resultsPerQueryGene = Gemma.CytoscapeSettings.maxGeneIdsPerCoexVisQuery / qlength;
    
    var i;
    for (i = 0; i < qlength; i++) {
        coexpressionSearchData.cytoscapeCoexCommand.geneIds.push(coexpressionSearchData.coexGridResults.queryGenes[i].id);
        
        queryGeneCountHash[coexpressionSearchData.coexGridResults.queryGenes[i].id] = 0;
        
    }

    var kglength = coexpressionSearchData.coexGridResults.knownGeneResults.length;
//only add to cytoscapeCoexCommand.geneIds if current query gene has room in its 'resultsPerQueryGeneCount' entry
    for (i = 0; i < kglength; i++) {
        if (coexpressionSearchData.cytoscapeCoexCommand.geneIds.indexOf(coexpressionSearchData.coexGridResults.knownGeneResults[i].foundGene.id) === -1 
        		&& queryGeneCountHash[coexpressionSearchData.coexGridResults.knownGeneResults[i].queryGene.id]< resultsPerQueryGene)  {
            coexpressionSearchData.cytoscapeCoexCommand.geneIds.push(coexpressionSearchData.coexGridResults.knownGeneResults[i].foundGene.id);
            queryGeneCountHash[coexpressionSearchData.coexGridResults.knownGeneResults[i].queryGene.id] = queryGeneCountHash[coexpressionSearchData.coexGridResults.knownGeneResults[i].queryGene.id] + 1;
        }
    }
	
};


Gemma.CytoscapePanelUtil.getGeneIdArrayFromCytoscapeJSONNodeObjects = function (selectedNodes){
	
	var selectedNodesGeneIdArray = [];
    var sNodesLength = selectedNodes.length;
    var i;
    for (i = 0; i < sNodesLength; i++) {
        selectedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;
    }
    
    return selectedNodesGeneIdArray;
	
};

