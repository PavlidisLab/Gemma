Ext.namespace('Gemma');

Gemma.CoexpressionSearchData =  Ext.extend(Ext.util.Observable, {
	
		coexGridResults:{},
		
		cytoscapeResults:{},
		
		coexGridCoexCommand:{},
		
		cytoscapeCoexCommand:{},
		
		coexSearchTimeout: 420000,
		
		initComponent: function(){
			Gemma.CoexpressionSearchData.superclass.initComponent.call(this);
			
			this.addEvents('searchForCoexGridDataComplete', 'searchForCytoscapeDataComplete','searchErrorFromCoexpressionSearchData');
		},
		
		constructor: function(configs){
			if(typeof configs !== 'undefined'){
				Ext.apply(this, configs);
			}
			Gemma.CoexpressionSearchData.superclass.constructor.call(this);
		},
		
		searchForCoexGridDataAndCytoscapeData: function(){
			
			ExtCoexpressionSearchController.doSearchQuick2(
		            this.coexGridCoexCommand, {
		                callback: function (result){
		                	this.coexGridResults = result;
		                	
		                	this.fireEvent('searchForCoexGridDataComplete');
		                	this.searchForCytoscapeData();
		                	
		                }.createDelegate(this),
		                timeout: this.coexSearchTimeout,						
		                errorHandler : function(result){
							this.fireEvent('searchErrorFromCoexpressionSearchData', result);
						}.createDelegate(this)
		            });
			
		},
		
		
		searchForCytoscapeData: function(){
			
	    	Gemma.CytoscapePanelUtil.restrictQueryGenesForCytoscapeQuery(this);
	    
	    	if (this.cytoscapeCoexCommand.geneIds.length<2){
	    		//There is a bug where if you can get a gene back in results but if you search for it by itself there are no results(PPP2R1A human)
        		this.cytoscapeResults.knownGeneResults = [];
        		this.fireEvent('searchForCytoscapeDataComplete');
        		return;
	    	}
	    	
			Ext.apply(this.cytoscapeCoexCommand, {
                queryGenesOnly: true
            });
			
			ExtCoexpressionSearchController.doSearchQuick2Complete(
		            this.cytoscapeCoexCommand,this.coexGridCoexCommand.geneIds, {
		                callback: function (results){
		                	this.cytoscapeResults = results;		                	
		                	this.fireEvent('searchForCytoscapeDataComplete');
		                	
		                }.createDelegate(this),
		                timeout: this.coexSearchTimeout,
		                						
						errorHandler : function(result){
							this.fireEvent('searchErrorFromCoexpressionSearchData', result);
						}.createDelegate(this)
		            });
		
	    	
		}

});