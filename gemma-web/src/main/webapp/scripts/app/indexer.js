/**
 * 
 * 
 * @param {}
 *           data
 * 
 */

function reset( data ) {

}

function handleSuccess( data ) {
   Ext.DomHelper.overwrite( "messages", {
      tag : 'div',
      html : data
   } );
}

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

function handleIndexSuccess( taskId ) {
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

function index( event ) {

   var callParams = [];

   var commandObj = {
      indexAD : adCheckBox.getValue(),
      indexEE : eeCheckBox.getValue(),
      indexProbe : probeCheckBox.getValue(),
      indexBibRef : bibRefCheckBox.getValue(),
      indexGene : geneCheckBox.getValue(),
      indexBioSequence : bsCheckBox.getValue(),
      indexExperimentSet : eeSetCheckBox.getValue(),
      indexGeneSet : geneSetCheckBox.getValue(),
      indexOntologies : ontologyCheckBox.getValue()
   };

   callParams.push( commandObj );

   var delegate = handleIndexSuccess.createDelegate( this, [], true );
   var errorHandler = handleFailure.createDelegate( this, [], true );

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
   IndexService.index.apply( this, callParams );

}

var indexForm = function() {

   Ext.form.Field.prototype.msgTarget = 'side';
   var simple = new Ext.FormPanel( {
      border : false
   } );

   geneCheckBox = new Ext.form.Checkbox( {
      boxLabel : 'Index genes',
      labelSeparator : '',
      name : 'gene'
   } );
   simple.add( geneCheckBox );

   probeCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index probes',
      name : 'probe'
   } );
   simple.add( probeCheckBox );

   adCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Platforms',
      name : 'ad'
   } );
   simple.add( adCheckBox );

   bsCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Biosequences',
      name : 'bs'
   } );
   simple.add( bsCheckBox );

   eeCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Expression Experiments',
      name : 'ee'
   } );
   simple.add( eeCheckBox );

   bibRefCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Bibliographic References',
      name : 'bibRef'
   } );
   simple.add( bibRefCheckBox );

   eeSetCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Experiment Groups',
      name : 'eeSet'
   } );
   simple.add( eeSetCheckBox );

   geneSetCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Gene Groups',
      name : 'geneSet'
   } );
   simple.add( geneSetCheckBox );

   ontologyCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index ontologies',
      name : 'ontologies'
   } );
   simple.add( ontologyCheckBox );

   simple.add( new Ext.Button( {
      text : "index",
      handler : function( event ) {
         Ext.Msg.show( {
            title : Gemma.HelpText.CommonWarnings.ReIndexing.title,
            msg : String.format( Gemma.HelpText.CommonWarnings.ReIndexing.text, 'database' ),
            buttons : Ext.Msg.YESNO,
            fn : function( btn, text ) {
               if ( btn == 'yes' ) {
                  index( event );
               }
            },
            scope : this,
            icon : Ext.MessageBox.WARNING
         } );
      },
      scope : this
   } ) );
   simple.render( 'index-form' );
};

Ext.onReady( function() {
   indexForm();
} );