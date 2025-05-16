/**
 * Shows submitted jobs. Work in progress.
 * 
 * @author paul
 * 
 */
Ext.onReady( function() {

   var v = new Ext.grid.GridPanel( {
      renderTo : 'submittedTasks',
      id : 'submittedTaskGrid',
      height : 500,
      autoScroll : true,
      width : 1300,
      loadMask : true,
      viewConfig : {
         forceFit : true
      },
      columns : [
                 {
                    header : "Submitted",
                    dataIndex : "submissionTime",
                    renderer : Ext.util.Format.dateRenderer( 'g:i:s l' ),
                    sortable : true,
                    width : 100
                 },
                 {
                    header : "Type",
                    dataIndex : "taskType",
                    sortable : true,
                    width : 200,
                    renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                       return value.replace( /.*\./, '' ).replace( /Impl$/, '' );
                    }
                 },
                 {
                    header : "TaskId",
                    dataIndex : "taskId",
                    width : 40
                 },
                 {
                    header : "Started",
                    dataIndex : "startTime",
                    renderer : Ext.util.Format.dateRenderer( 'g:i:s l' ),
                    width : 100
                 },
                 {
                    header : "Runtime (s)",
                    tooltip : "How long the job has been running",
                    dataIndex : "startTime",
                    renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                       if ( record.get( "startTime" ) && record.get( "finishTime" ) ) {
                          return (record.get( "finishTime" ) - record.get( "startTime" )) / 1000;
                       } else if ( record.get( "startTime" ) ) {
                          return (new Date() - record.get( "startTime" )) / 1000;
                       } else {
                          return "Queued for " + (new Date() - record.get( "submissionTime" )) / 1000;
                       }
                    },
                    width : 100
                 },
                 {
                    header : "Finished",
                    dataIndex : "finishTime",
                    renderer : Ext.util.Format.dateRenderer( 'g:i:s l' ),
                    width : 100
                 },
                 {
                    header : "Status",
                    dataIndex : "taskStatus"
                 },
                 {
                    header : "Submitter",
                    dataIndex : "submitter"
                 },
                 {
                    header : "Remote?",
                    dataIndex : "runningRemotely"
                 },
                 {
                    header : "Actions",
                    dataIndex : "taskId",
                    renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                       var actions = '';
                       var completed = record.get( "done" );
                       var emailAlert = record.get( "emailAlert" );
                       if ( !completed ) {
                          actions += '<span class="link" onClick="Ext.getCmp(\'submittedTaskGrid\').cancelTask(\''
                             + value + '\')"><img src="' + Gemma.CONTEXT_PATH + '/images/icons/cross.png" /></span>';
                       }
                       if ( !emailAlert && !completed ) {
                          actions += '<span class="link" onClick="Ext.getCmp(\'submittedTaskGrid\').addEmailAlert(\''
                             + value + '\')"><img src="' + Gemma.CONTEXT_PATH + '/images/icons/email.png" /></span>';
                       }

                       return actions;
                    }
                 } ],

      cancelTask : function( taskId ) {
         Ext.Msg.show( {
            title : 'Are you sure?',
            msg : 'Are you sure you want to cancel this task?',
            buttons : Ext.Msg.YESNO,
            fn : function( btn, text ) {
               if ( btn === 'yes' ) {
                  ProgressStatusService.cancelJob( taskId, function( successfullyCancelled ) {
                     if ( !successfullyCancelled ) {
                        Ext.Msg.alert( "Couldn't cancel",
                           "Sorry, the job couldn't be cancelled; perhaps it finished or was cancelled already?" );
                     } else {
                        v.store.load();
                     }
                  } );
               }
            },
            scope : this
         } );
      },

      addEmailAlert : function( taskId ) {
         Ext.Msg.show( {
            title : 'Add email notification.',
            msg : 'This will send email notification to the task submitter once task is completed.',
            buttons : Ext.Msg.YESNO,
            fn : function( btn, text ) {
               if ( btn === 'yes' ) {
                  ProgressStatusService.addEmailAlert( taskId, function() {
                     v.store.load();
                  } );
               }
            },
            scope : this
         } );
      },

      store : new Ext.data.Store( {
         proxy : new Ext.data.DWRProxy( {
            apiActionToHandlerMap : {
               read : {
                  dwrFunction : ProgressStatusService.getSubmittedTasks
               }
            }
         } ),
         autoLoad : true,
         reader : new Ext.data.ListRangeReader( {
            id : 'taskId',
            record : Ext.data.Record.create( [ {
               name : "taskId",
               type : "string"
            }, {
               name : "submissionTime",
               type : "date"
            }, {
               name : "startTime",
               type : "date"
            }, {
               name : "finishTime",
               type : "date"
            }, {
               name : "taskStatus",
               type : "string"
            }, {
               name : "submitter",
               type : "string"
            }, {
               name : "taskType",
               type : "string"
            }, {
               name : "done",
               type : "boolean"
            }, {
               name : "emailAlert",
               type : "boolean"
            }, {
               name : "runningRemotely",
               type : "boolean"
            } ] )
         } )
      } ),
      buttons : [ {
         text : "Refresh",
         handler : function() {
            v.store.load();
         }
      } ]
   } );

} );