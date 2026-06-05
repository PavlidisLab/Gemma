function reset( data ) {

}

function handleSuccess( data ) {
   Ext.DomHelper.overwrite( "messages", {
      tag : 'div',
      html : data
   } );
}

function handleFailure( data ) {
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
      const task = new Gemma.ObservableSubmittedTask( {
         'taskId' : taskId
      } );
      task.on( 'task-failed', handleFailure );
      task.on( 'task-cancelling', reset );
      task.showTaskProgressWindow( {
         showLogButton : true
      } );
   } catch ( e ) {
      handleFailure( e );
   }
}

function index( commandObj ) {

   const callParams = [];

   callParams.push( commandObj );

   const delegate = handleIndexSuccess.createDelegate( this, [], true );
   const errorHandler = handleFailure.createDelegate( this, [], true );

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

Ext.onReady( function() {
   Ext.form.Field.prototype.msgTarget = 'side';
   const simple = new Ext.FormPanel( {
      border : false
   } );

   const geneCheckBox = new Ext.form.Checkbox( {
      boxLabel : 'Index Genes',
      labelSeparator : '',
      name : 'gene'
   } );
   simple.add( geneCheckBox );

   const probeCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Design Elements',
      name : 'probe'
   } );
   simple.add( probeCheckBox );

   const adCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Platforms',
      name : 'ad'
   } );
   simple.add( adCheckBox );

   const bsCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Biological Sequences',
      name : 'bs'
   } );
   simple.add( bsCheckBox );

   const eeCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Datasets',
      name : 'ee'
   } );
   simple.add( eeCheckBox );

   const bibRefCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Publications',
      name : 'bibRef'
   } );
   simple.add( bibRefCheckBox );

   const eeSetCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Dataset Groups',
      name : 'eeSet'
   } );
   simple.add( eeSetCheckBox );

   const geneSetCheckBox = new Ext.form.Checkbox( {
      labelSeparator : '',
      boxLabel : 'Index Gene Groups',
      name : 'geneSet'
   } );
   simple.add( geneSetCheckBox );

   simple.add( new Ext.Button( {
      text : "Index",
      handler : function( event ) {
         Ext.Msg.show( {
            title : Gemma.HelpText.CommonWarnings.ReIndexing.title,
            msg : String.format( Gemma.HelpText.CommonWarnings.ReIndexing.text, 'database' ),
            buttons : Ext.Msg.YESNO,
            fn : function( btn ) {
               if ( btn === 'yes' ) {
                  index( {
                     indexPlatforms : adCheckBox.getValue(),
                     indexDatasets : eeCheckBox.getValue(),
                     indexDesignElements : probeCheckBox.getValue(),
                     indexPublications : bibRefCheckBox.getValue(),
                     indexGenes : geneCheckBox.getValue(),
                     indexBioSequences : bsCheckBox.getValue(),
                     indexDatasetGroups : eeSetCheckBox.getValue(),
                     indexGeneGroups : geneSetCheckBox.getValue()
                  } );
               }
            },
            scope : this,
            icon : Ext.MessageBox.WARNING
         } );
      },
      scope : this
   } ) );
   simple.render( 'index-form' );
} );