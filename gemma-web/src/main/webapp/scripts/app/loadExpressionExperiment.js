/**
 * Loading of data from GEO or ArrayExpress
 * 
 * @author paul
 * 
 */

var uploadButton;
var arrayDesignCombo;
Ext.onReady( function() {
   uploadButton = new Ext.Button( {
      renderTo : "upload-button",
      text : "Start loading",
      width : 100

   } );

   arrayDesignCombo = new Gemma.ArrayDesignCombo( {
      renderTo : 'arrayDesignCombo',
      id : 'arrayDesign',
      width : 200
   } );

   uploadButton.on( "click", submitForm );
} );

function submitForm() {

   var accession = Ext.get( "accession" ).dom.value;
   var suppressMatching = Ext.get( "suppressMatching" ).dom.checked;
   var loadPlatformOnly = Ext.get( "loadPlatformOnly" ).dom.checked;
   var splitByPlatform = Ext.get( "splitByPlatform" ).dom.checked;
   var allowSuperSeriesLoad = Ext.get( "allowSuperSeriesLoad" ).dom.checked;
   var arrayExpress = Ext.get( "arrayExpress" ).dom.checked;
   var allowArrayExpressDesign = Ext.get( "allowArrayExpressDesign" ).dom.checked;
   var arrayDesign = arrayDesignCombo.getArrayDesign();
   var arrayDesignName = null;
   if ( arrayDesign ) {
      arrayDesignName = arrayDesign.get( "shortName" );
   }

   var callParams = [];

   var commandObj = {
      accession : accession,
      suppressMatching : suppressMatching,
      loadPlatformOnly : loadPlatformOnly,
      splitByPlatform : splitByPlatform,
      arrayExpress : arrayExpress,
      allowSuperSeriesLoad : allowSuperSeriesLoad,
      arrayDesignName : arrayDesignName,
      allowArrayExpressDesign : allowArrayExpressDesign
   };

   callParams.push( commandObj );

   // this should return quickly, with the task id.
   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : Gemma.CONTEXT_PATH + '/images/loading.gif'
   } );
   Ext.DomHelper.append( "messages", "&nbsp;Submitting job..." );

   uploadButton.disable();

   // ExpressionExperimentLoadController.load.apply(this, callParams);

   ExpressionExperimentLoadController.load( commandObj, {
      callback : function( taskId ) {
         var task = new Gemma.ObservableSubmittedTask( {
            'taskId' : taskId
         } );

         task.on( 'task-completed', function( payload ) {
            Ext.DomHelper.overwrite( "messages", payload );

         } );

         task.showTaskProgressWindow( {
            showLogButton : true
         } );

      },
      errorHandler : handleFailure
   } );

}

function handleFailure( data ) {
   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : Gemma.CONTEXT_PATH + '/images/icons/warning.png'
   } );
   Ext.DomHelper.append( "messages", {
      tag : 'span',
      html : data
   } );
   uploadButton.enable();
}

function reset( data ) {
   uploadButton.enable();
}
