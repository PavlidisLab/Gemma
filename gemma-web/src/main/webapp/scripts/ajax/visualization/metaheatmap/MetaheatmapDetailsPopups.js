Ext.namespace('Gemma');

Gemma.MetaVisualizationPopups = {};

Gemma.MetaVisualizationPopups.makeGeneInfoWindow = function ( geneName, geneId ) {
	return new Ext.Window(
			{  width: 200,
			   height: 100,

				title: "Details for gene: "+geneName,
				layout:"vbox",
				items:[
				       {xtype:'label', ref:'aliases', text:"Aliases: "},
				       {xtype:'label', ref:'probes', text:"Probes: "},
				       {xtype:'label', ref:'geneGroups', text:"Gene groups: "},			      	         
				      ]			      			                                                                     												      	  
			});		
};

Gemma.MetaVisualizationPopups.makeDatasetInfoWindow = function ( datasetName, datasetId ) {
	return new Ext.Window(
			{  width: 200,
			   height: 100,

				title: "Details for experession experiment: "+datasetName,
				layout:"vbox",
				items:[
				       {xtype:'label', ref:'description', text:"Description: "},
				       {xtype:'label', ref:'samples', text:"Samples: "},
				       {xtype:'label', ref:'tags', text:"Tags: "},
				       {xtype:'label', ref:'factors', text:"Factors: "},
				       // analyses?
				       // experimental design?
				      ]			      			                                                                     												      	  
			});		
};
