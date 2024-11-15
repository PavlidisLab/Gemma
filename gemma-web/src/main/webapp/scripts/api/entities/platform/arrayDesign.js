Ext.namespace( 'Gemma' );

function handleFailure( data, e ) {
   reportFeedback( "error", data, e );
}

function reset( data ) {
   uploadButton.enable();
}

// array designs grid page doesn't have a 'messages' div
function reportFeedback( type, text, e ) {
   var messagesDiv = (Ext.get( "messages" ) !== null);
   if ( type === "error" ) {
      if ( messagesDiv ) {
         Ext.DomHelper.overwrite( "messages", {
            tag : 'img',
            src : ctxBasePath + '/images/icons/warning.png'
         } );
         Ext.DomHelper.append( "messages", {
            tag : 'span',
            html : "&nbsp;There was an error:<br/>" + text + e
         } );
      } else {
         Ext.Msg.show( {
            title : 'Error',
            msg : "There was an error:<br/>" + text + e,
            buttons : Ext.Msg.OK,
            icon : Ext.MessageBox.WARNING
         } );
      }

   } else if ( type === "loading" ) {
      if ( messagesDiv ) {
         Ext.DomHelper.overwrite( "messages", {
            tag : 'img',
            src : ctxBasePath + '/images/default/tree/loading.gif'
         } );
      }
   } else if ( type === "success" ) {
      if ( messagesDiv ) {
         Ext.DomHelper.overwrite( "messages", "" );
      }
   }
}

function handleReportLoadSuccess( data, callerScope ) {
   try {
      reportFeedback( "success" );
      var arrayDesignSummaryDiv = "arraySummary_" + data.id;
      if ( Ext.get( arrayDesignSummaryDiv ) !== null ) {
         Ext.DomHelper.overwrite( arrayDesignSummaryDiv, data.html );
      }
      if ( callerScope ) {
         callerScope.fireEvent( 'reportUpdated', data.id );
      }
   } catch (e) {
      handleFailure( data, e );

   }
}

function handleDoneUpdateReport( id, callerScope, callback ) {

   var callParams = [];
   var commandObj = {
      id : id
   };
   callParams.push( commandObj );
   var callback = handleReportLoadSuccess.createDelegate( this, [ callerScope ], true );
   var errorHandler = handleFailure.createDelegate( this, [], true );
   callParams.push( callback );
   callParams.push( errorHandler );
   // confusion: what is 'this'
   ArrayDesignController.getReportHtml.apply( Ext.getDom( 'arraySummary_' + id ), callParams );

}

function updateArrayDesignReport( id, callerScope ) {

   var callParams = [];
   callParams.push( {
      id : id
   } );
   callParams.push( {
      callback : function( taskId ) {
         var task = new Gemma.ObservableSubmittedTask( {
            'taskId' : taskId
         } );
         // e.g. refresh icon from showAllArrayDesigns
         if ( callerScope ) {
            var throbberEl = callerScope.getEl();
            task.showTaskProgressThrobber( throbberEl );
         } else {// e.g. refresh button on showArrayDesign.html
            task.showTaskProgressWindow( {
               showLogButton : true
            } );
         }
         task.on( 'task-completed', function( payload ) {
            handleDoneUpdateReport( id, callerScope );
         } );
      }.createDelegate( this )
   } );

   ArrayDesignController.updateReport.apply( this, callParams );
}

Gemma.updateArrayDesignReport = updateArrayDesignReport;

function remove( id ) {
   alert( "Are you sure?" );
}
