Ext.namespace('Gemma');

Gemma.CoexpressionGraphData = function(coexpressionSearchData) {		
			
		this.originalResults={};
		this.originalResults.geneResults = coexpressionSearchData.cytoscapeResults.knownGeneResults;
		this.originalResults.trimStringency = coexpressionSearchData.cytoscapeResults.nonQueryGeneTrimmedValue;
	
		this.defaultSize = "medium";
		
		this.mediumGraphMaxSize = coexpressionSearchData.cytoscapeResults.maxEdges * 3 / 4;
		this.smallGraphMaxSize = coexpressionSearchData.cytoscapeResults.maxEdges / 2;
		
		this.largeGraphDataEnabled=false;
		this.mediumGraphDataEnabled=false;
		this.smallGraphDataEnabled=false;
		
		this.getTrimmedGraphData = function(resultsSizeLimit){
			
			return Gemma.CoexValueObjectUtil.trimKnownGeneResultsForReducedGraph(this.originalResults.geneResults, coexpressionSearchData.coexGridCoexCommand.geneIds,
	    			coexpressionSearchData.cytoscapeCoexCommand.stringency, coexpressionSearchData.cytoscapeCoexCommand.eeIds.length,
	    			resultsSizeLimit);
			
		};
		
		if (this.mediumGraphMaxSize > this.originalResults.geneResults.length ){
			this.graphDataMedium = this.originalResults;
		}else {
			this.graphDataMedium = this.getTrimmedGraphData(this.mediumGraphMaxSize);
			if (this.graphDataMedium.geneResults.length < this.originalResults.geneResults.length){
				this.mediumGraphDataEnabled = true;
				this.largeGraphDataEnabled = true;
			}
		}
		
		if (this.smallGraphMaxSize > this.graphDataMedium.geneResults.length ){
			this.graphDataSmall = this.graphDataMedium;
		}else{
			this.graphDataSmall = this.getTrimmedGraphData(this.smallGraphMaxSize);
			if (this.graphDataSmall.geneResults.length < this.graphDataMedium.geneResults.length){
				this.smallGraphDataEnabled = true;
				this.mediumGraphDataEnabled = true;
			}
		}
		
		this.getGraphData = function(graphSize){
			
			if (!graphSize){
				graphSize = this.defaultSize;
			}
			
			if(graphSize=="medium"){
				
				return this.graphDataMedium;
				
			} else if (graphSize=="small"){
				
				return this.graphDataSmall;
			}
			
			return this.originalResults;
			
		};		
		
		

};