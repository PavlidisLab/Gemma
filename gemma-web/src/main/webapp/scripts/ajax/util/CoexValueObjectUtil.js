Ext.namespace("Gemma");


Gemma.CoexValueObjectUtil = {
		
		//takes an array of CoexpressionValueObjectExts (knowngenes) and trims away results based on stringency
		trimKnownGeneResults: function (knowngenes, currentQueryGeneIds, filterStringency) {
	       
	        //helper array to prevent duplicate nodes from being entered
	        var graphNodeIds = [];

	        var trimmedGeneResults = [];	        
	       
	        var kglength = knowngenes.length;
	        for (i = 0; i < kglength; i++) {

	            // go in only if the query or known gene is contained in the original query geneids AND the stringency is >= the filter stringency
	            if (((currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1 || (currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1)) && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency))

	            ) {

	                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) === -1) {
	                    graphNodeIds.push(knowngenes[i].foundGene.id);
	                }


	                if (graphNodeIds.indexOf(knowngenes[i].queryGene.id) === -1) {
	                    graphNodeIds.push(knowngenes[i].queryGene.id);
	                }
	                
	                trimmedGeneResults.push(knowngenes[i]);

	            } //end if
	        } // end for (<kglength)
	        //we need to loop through again to add edges that we missed the first time (because we were unsure whether both nodes would be in the graph)
	        for (i = 0; i < kglength; i++) {

	            //if both nodes of the edge are in the graph, and it meets the stringency threshold, and neither of the nodes are query genes(because their edges have already been added) 
	            if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) !== -1 && graphNodeIds.indexOf(knowngenes[i].queryGene.id) !== -1 && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency) && currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) === -1 && currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) === -1) {

	                trimmedGeneResults.push(knowngenes[i]);

	            }


	        } // end for (<kglength)
	        
	        var trimmed={};

	        trimmed.trimmedKnownGeneResults = trimmedGeneResults;
	        
	        trimmed.trimmedNodeIds = graphNodeIds;
	        
	        return trimmed;

	    },
	    
	    getCurrentQueryGeneIds: function (queryGenes){
	    	var currentQueryGeneIds = [];
	        var qlength = queryGenes.length;

	        //populate geneid array for complete graph
	        var i;

	        for (i = 0; i < qlength; i++) {	           

	            if (currentQueryGeneIds.indexOf(queryGenes[i].id) === -1) {
	                currentQueryGeneIds.push(queryGenes[i].id);
	            }

	        }
	        
	        return currentQueryGeneIds;
	    }
		
		
}