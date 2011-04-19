Ext.namespace('Gemma');

Gemma.MetaVisualizationPopups = {};

Gemma.MetaVisualizationPopups.makeGeneInfoWindow = function ( geneName, geneId ) {
	
	GenePickerController.getGenes([geneId], function(genes){
		var i;
		for(i = 0; i<genes.length;i++){ // should only be one
		var gene = genes[i];
			var popup = new Ext.Window(
			{  width: 600,
			   height: 350,
				autoScroll: true,
				bodyStyle: 'padding: 7px; font-size: 12px; line-height: 18px; ',
				title: "Details for gene: "+geneName,
				html:'<br><h4><a target="_blank" href="/Gemma/gene/showGene.html?id='+gene.id+'">'+gene.officialSymbol+'</a> '+gene.officialName+'</h4>'+
					   	'<b>Taxon:</b> ' + gene.taxonCommonName+'<br>'+
						'<b>Aliases:</b> '+gene.aliases+' <a target="_blank" title="NCBI Gene link"'+
							'href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids='+gene.ncbiId+'">'+
							'<img alt="NCBI Gene Link" src="/Gemma/images/logo/ncbi.gif"/> </a>'+'<br>'+
				       '<b>Description:</b> '+gene.description+'<br>'+
				       '<a target="_blank" href="/Gemma/gene/showCompositeSequences.html?id=$'+gene.id+'">Probes</a>'+'<br>'+
					   '<br><a target="_blank" href="/Gemma/gene/showGene.html?id='+gene.id+'">More about this gene</a>'									      	  
			});
		}
			popup.show();
	});
			
};


Gemma.MetaVisualizationPopups.makeDatasetInfoWindow = function ( datasetName, datasetShortName, datasetId ) {
	var textPanel = new Ext.Panel({
		bodyStyle: 'padding: 7px; font-size: 12px; line-height: 18px; ',
		//bodyBorder: true,
		border:true,
		frame:true, // gives blue background
		tpl:new Ext.XTemplate('<br><h4>',
					'<a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id={datasetId}"',
						' ext:qtip="{datasetName}">{datasetShortName}</a>: {datasetName}</h4><br><b> Description: </b>{description}<br><br>',
						'<a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id={datasetId}',
						' ext:qtip="{datasetName}">More about this experiment</a>'),
		tplWriteMode: 'overwrite'
	});

	var popup = new Ext.Window(
			{  	width: 600,
			   	height: 500,
				layout: 'fit',
				autoScroll: true,
				title: "Details for expression experiment",
				items: textPanel
			});	

	
	popup.show();
	
	popup.loadMask = new Ext.LoadMask(popup.getEl(), {
			msg: "Loading ..."
		});
	
	popup.loadMask.show();	
	
	ExpressionExperimentController.getDescription(datasetId, function(description){
			textPanel.update({
				description: description,
				datasetName: datasetName,
				datasetShortName: datasetShortName,
				datasetId: datasetId
			});
			popup.loadMask.hide();
			
	}.createDelegate(this));
			
};
