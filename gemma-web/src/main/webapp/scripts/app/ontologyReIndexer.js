function handleFailure( data, e ) {
   Ext.DomHelper.overwrite( "taskId", "" );
   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : Gemma.CONTEXT_PATH + '/images/icons/warning.png'
   } );
   Ext.DomHelper.append( "messages", {
      tag : 'span',
      html : "&nbsp;There was an error: " + data
   } );
}

function handleSuccess( taskId ) {
   try {
      Ext.DomHelper.overwrite( "messages", "" );
      var task = new Gemma.ObservableSubmittedTask( {
         'taskId' : taskId
      } );
      task.on( 'task-failed', handleFailure );
      task.on( 'task-cancelling', reset );
      task.showTaskProgressWindow( {
         showLogButton : true
      } );
   } catch (e) {
      handleFailure( taskId, e );

   }
}

function reinitializeOntologyIndices( event ) {

   var delegate = handleSuccess.createDelegate( this, [], true );
   var errorHandler = handleFailure.createDelegate( this, [], true );

   var callParams = [];
   callParams.push( {
      callback : delegate,
      errorHandler : errorHandler
   } );

   // this should return quickly, with the task id.
   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : Gemma.CONTEXT_PATH + '/images/default/tree/loading.gif'
   } );
   Ext.DomHelper.append( "messages", "&nbsp;Submitting job..." );
   AnnotationController.reinitializeOntologyIndices.apply( this, callParams );

}

var reinitializeOntologyIndicesForm = function() {

   Ext.form.Field.prototype.msgTarget = 'side';
   var simple = new Ext.FormPanel( {
      border : false
   } );

   simple.add( new Ext.Button( {
      text : "Reinitialize Ontology Indices",
      handler : function( event ) {
         Ext.Msg.show( {
            title : Gemma.HelpText.CommonWarnings.ReIndexing.title,
            msg : String.format( Gemma.HelpText.CommonWarnings.ReIndexing.text, 'ontology' ),
            buttons : Ext.Msg.YESNO,
            fn : function( btn, text ) {
               if ( btn == 'yes' ) {
                  reinitializeOntologyIndices( event );
               }
            },
            scope : this,
            icon : Ext.MessageBox.WARNING
         } );
      },
      scope : this
   } ) );
   simple.render( 'reinitializeOntologyIndices-form' );
};

Ext.onReady( function() {
   reinitializeOntologyIndicesForm();
} );