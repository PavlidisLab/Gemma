Ext.namespace( "Gemma" );

/**
 * ObservableSubmittedTask -- front-end representation of submitted task.
 * 
 * @param taskId
 * 
 * Events: task-completed -- (result) task-failed -- (exception) task-cancelling -- task-status-change --
 * synchronization-error -- communication or marshalling error, bug, etc log-message-received -- log message used by
 * progress window to redraw log panel.
 * 
 * @class Gemma.ObservableSubmittedTask
 * @extends Ext.util.Observable
 */
Gemma.ObservableSubmittedTask = Ext.extend( Ext.util.Observable, {

   /**
    * @memberOf Gemma. ObservableSubmittedTask
    * @constructor
    */
   constructor : function( config ) {
      this.taskId = config.taskId; // TODO: check and fail early

      this.addEvents( {
         "task-completed" : true,
         "task-failed" : true,
         "task-cancelling" : true,
         "task-status-change" : true,
         "synchronization-error" : true,
         "log-message-received" : true
      } );

      // Copy configured listeners into *this* object so that the base class's
      // constructor will add them.
      if ( config ) {
         if ( config.listeners ) {
            this.listeners = config.listeners;
         }
      }

      // Call our superclass constructor to complete construction process.
      Gemma.ObservableSubmittedTask.superclass.constructor.call( config );
   },

   /**
    * Send a task cancel request to the server.
    * 
    * @public
    */
   cancel : function() {
      ProgressStatusService.cancelJob( this.taskId, function() {
         this.fireEvent( 'task-cancelling' );
      }.createDelegate( this ) );
   },

   /**
    * Send addEmailAlert request to the server. This will notify user of task completion via email.
    * 
    * @public
    */
   addEmailAlert : function() {
      ProgressStatusService.addEmailAlert( this.taskId );
   },

   /**
    * Display a spinner(throbber) while task is running.
    * 
    * @public
    * @param {ExtElement}
    *           element -- an element to replace with throbber.
    */
   showTaskProgressThrobber : function( element ) {
      /*
       * Doesn't work quite right ... implemented for 'report' update.
       */
      var id = Ext.id();
      Ext.DomHelper
         .append( element, '<span id="' + id + '"><img src="' + Gemma.CONTEXT_PATH + '/images/default/tree/loading.gif"/></span>' );

      this.on( 'task-completed', function() {
         Ext.DomHelper.overwrite( id, "" );
      } );

      this.on( 'task-failed', function() {
         Ext.DomHelper.overwrite( id, "" );
      } );

      this._startSync();
   },

   /**
    * Display a popup with task progress information.
    * 
    * @public
    * @param [displayOptions] --
    *           see Gemma.ProgressWidget for details.
    */
   showTaskProgressWindow : function( displayOptions ) {
      try {
         var progressWindow = new Gemma.ProgressWindow( {
            task : this,
            displayOptions : displayOptions
         } );
         this._startSync();
         progressWindow.show();
      } catch (error) {
         Ext.Msg.alert( "Error", error );
      }
   },

   /**
    * @private
    * 
    * Starts syncing with the submitted task on the server. This includes current status, log messages, etc.
    * 
    * It stops syncing after task reaches final state (completed, failed or cancelled).
    */
   _startSync : function() {
      var task = this;
      task.status = "NEW";
      task.lastLogMessage = "";
      task.logs = "";

      checkTaskStatus();



      /**
       * Grab task status from the sever. Schedule to run the same function after delay if task is not in final state.
       */
      function checkTaskStatus() {
         if ( task.status === 'QUEUED' || task.status === 'RUNNING' || task.status === 'NEW' ) {
            ProgressStatusService.getSubmittedTask( task.taskId, {
               callback : function onSuccess( submittedTask ) {
                  if ( submittedTask === null ) {
                     task.status = 'UNKNOWN';
                     task.fireEvent( 'synchronization-error', "Task not found on the server." );
                     Ext.Msg.alert( "synchronization-error" );
                     return;
                  }

                  sync( submittedTask ); // copy state from the server to ObservableSubmittedTask

                  Ext.defer( checkTaskStatus, 3000 ); // check again after delay

               },
               errorHandler : function onFailure( error ) {
                  // can't get updates from the server.
                  // stop checking for updates.
                  task.status = 'UNKNOWN';
                  task.fireEvent( 'synchronization-error', error );
                  Ext.Msg.alert( "synchronization-error" );
               }
            } );
         }
      }

      function sync( submittedTask ) {
         task.logs = submittedTask.logMessages;

         if ( task.lastLogMessage !== submittedTask.lastLogMessage ) {
            task.lastLogMessage = submittedTask.lastLogMessage;
            task.fireEvent( 'log-message-received', task.lastLogMessage );
         }

         if ( task.status !== submittedTask.taskStatus ) {
            task.status = submittedTask.taskStatus;
            task.fireEvent( 'task-status-change', task.status );
         }

         if ( task.status === 'FAILED' ) {
            task.fireEvent( 'task-failed', "Task failed." );
         } else if ( task.status === 'COMPLETED' ) {
            getTaskResult(); // result or exception
         }
         // if (task.status === 'CANCELLING') ???
      }

      function getTaskResult() {
         TaskCompletionController.checkResult( task.taskId, {
            callback : function processResult( taskResult ) {
               task.fireEvent( 'task-completed', taskResult );
            },
            errorHandler : function handlerError( error ) {
               task.fireEvent( 'task-failed', error );
            }
         } );
      }
   }
} );
