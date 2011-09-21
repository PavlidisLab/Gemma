Ext.namespace('Gemma');

// Do this ONLY in the Gemma side
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {

    Ext.QuickTips.init();
    Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
    
    var phenotypesPanel = new Gemma.PhenotypesPanel();
	phenotypesPanel.setDataStores(
		new Ext.data.DWRProxy(PhenotypeSearchController.findAllPhenotypes),
		new Ext.data.DWRProxy({
	        apiActionToHandlerMap: {
    	        read: {
        	        dwrFunction: PhenotypeSearchController.findCandidateGenes,
            	    getDwrArgsFunction: function(request){
            	    	return [request.params["phenotypeValue[]"]];
	                }
    	        }
	        }
    }));
    
	new Gemma.GemmaViewPort({
	 	centerPanelConfig: phenotypesPanel 
	});
});
