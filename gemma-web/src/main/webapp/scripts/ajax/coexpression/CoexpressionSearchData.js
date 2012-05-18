Ext.namespace('Gemma');

Gemma.CoexpressionSearchData =  Ext.extend(Ext.util.Observable, {
	
		coexGridResults:{},
		
		cytoscapeResults:{},
		
		coexGridCoexCommand:{},
		
		cytoscapeCoexCommand:{},
		
		coexSearchTimeout: 420000,
		
		initComponent: function(){
			Gemma.CoexpressionSearchData.superclass.initComponent.call(this);
			
			this.addEvents('searchForCoexGridDataComplete', 'searchForCytoscapeDataComplete');
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