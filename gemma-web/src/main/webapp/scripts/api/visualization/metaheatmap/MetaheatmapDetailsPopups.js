Ext.namespace( 'Gemma' );

Gemma.MetaVisualizationPopups = {};
Gemma.MetaVisualizationPopups.openWindows = [];
Gemma.MetaVisualizationPopups.cascadeLayoutCounter = 0;

Gemma.MetaVisualizationPopups.makeGeneInfoWindow = function( geneName, geneId ) {

   GenePickerController.getGenes( [ geneId ], function( genes ) {
      for (var i = 0; i < genes.length; i++) { // should only be one!
         var gene = genes[i];
         var popup = new Ext.Window( {
            width : 600,
            height : 350,
            autoScroll : true,
            closeAction : 'hide',
            bodyStyle : 'padding: 7px; font-size: 12px; line-height: 18px; ',
            title : "Details for gene: " + geneName,
            html : '<br><h4><a target="_blank" href="' + Gemma.CONTEXT_PATH + '/gene/showGene.html?id=' + gene.id + '">'
               + gene.officialSymbol + '</a> ' + gene.officialName + '</h4>' + '<b>Taxon:</b> ' + gene.taxonCommonName
               + '<br>' + '<b>Aliases:</b> ' + gene.aliases + ' <a target="_blank" title="NCBI Gene link"'
               + 'href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids='
               + gene.ncbiId + '">' + '<img alt="NCBI Gene Link" src="' + Gemma.CONTEXT_PATH + '/images/logo/ncbi-symbol.svg" height="16"/> </a>' + '<br>'
               + '<b>Description:</b> ' + gene.description + '<br>'
               + '<br><a target="_blank" href="' + Gemma.CONTEXT_PATH + '/gene/showGene.html?id=' + gene.id + '">More about this gene</a>'
         } );
      }
      popup.show();

      var xy = popup.getPosition();
      popup.setPosition( xy[0] + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20, xy[1]
         + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20 );
      Gemma.MetaVisualizationPopups.cascadeLayoutCounter++;
      if ( Gemma.MetaVisualizationPopups.cascadeLayoutCounter > 4 ) {
         Gemma.MetaVisualizationPopups.cascadeLayoutCounter = 0;
      }
   } );

};

Gemma.MetaVisualizationPopups.makeDatasetInfoWindow = function( datasetName, datasetShortName, datasetId ) {
   var textPanel = new Ext.Panel(
      {
         bodyStyle : 'padding: 7px; font-size: 12px; line-height: 18px; ',
         // bodyBorder: true,
         border : true,
         closeAction : 'hide',
         frame : true, // gives blue background
         tpl : new Ext.XTemplate(
            '<br><h4>',
            '<a target="_blank" href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id={datasetId}"',
            ' ext:qtip="{datasetName}">{datasetShortName}</a>: {datasetName}</h4><br><b> Description: </b>{description}<br><br>',
            '<a target="_blank" href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id={datasetId}"',
            ' ext:qtip="{datasetName}">More about this experiment</a>' ),
         tplWriteMode : 'overwrite'
      } );

   var popup = new Ext.Window( {
      width : 600,
      height : 500,
      layout : 'fit',
      autoScroll : true,
      title : "Details for expression experiment",
      items : textPanel
   } );

   popup.show();

   var xy = popup.getPosition();
   popup.setPosition( xy[0] + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20, xy[1]
      + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20 );
   Gemma.MetaVisualizationPopups.cascadeLayoutCounter++;
   if ( Gemma.MetaVisualizationPopups.cascadeLayoutCounter > 4 ) {
      Gemma.MetaVisualizationPopups.cascadeLayoutCounter = 0;
   }

   popup.loadMask = new Ext.LoadMask( popup.getEl(), {
      msg : "Loading ..."
   } );

   popup.loadMask.show();

   ExpressionExperimentController.getDescription( datasetId, function( description ) {
      textPanel.update( {
         description : description,
         datasetName : datasetName,
         datasetShortName : datasetShortName,
         datasetId : datasetId
      } );
      popup.loadMask.hide();

   }.createDelegate( this ) );

};

Gemma.MetaVisualizationPopups.makeMinipieInfoWindow = function( numberOfProbesTotal, numberOfProbesDiffExpressed,
   percentProbesDiffExpressed, numberOfProbesUpRegulated, percentProbesUpRegulated, numberOfProbesDownRegulated,
   percentProbesDownRegulated ) {

   var popup = new Ext.Window( {
      width : 400,
      height : 350,
      autoScroll : true,
      bodyStyle : 'padding: 7px; font-size: 12px; line-height: 18px; ',
      title : "Differential Expression Details",
      html : '<b>Number of Probes:<br><br></b>' + '<b>&nbsp;&nbsp;&nbsp;Total</b>: ' + numberOfProbesTotal + '<br><br>'
         + '<b>&nbsp;&nbsp;&nbsp;Differentially Expressed</b>: ' + numberOfProbesDiffExpressed + ' ('
         + percentProbesDiffExpressed + ' of total)<br><br>' + '<b>&nbsp;&nbsp;&nbsp;Up Regulated</b>: '
         + numberOfProbesUpRegulated + '  (' + percentProbesUpRegulated + ' of total)<br><br> '
         + '<b>&nbsp;&nbsp;&nbsp;Down Regulated</b>: ' + numberOfProbesDownRegulated + '  ('
         + percentProbesDownRegulated + ' of total)<br>'
   } );

   popup.show();

   var xy = popup.getPosition();
   popup.setPosition( xy[0] + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20, xy[1]
      + Gemma.MetaVisualizationPopups.cascadeLayoutCounter * 20 );
   Gemma.MetaVisualizationPopups.cascadeLayoutCounter++;
   if ( Gemma.MetaVisualizationPopups.cascadeLayoutCounter > 4 ) {
      Gemma.MetaVisualizationPopups.cascadeLayoutCounter = 0;
   }

};
