/**
 * Simple support for uploading a file.
 * 
 * @author Paul
 * 
 */
Ext.namespace( "Gemma" );
Gemma.FileUploadForm = Ext.extend( Ext.Panel, {

   width : 500,
   autoHeight : true,

   reset : function() {
      Ext.getCmp( 'form-file' ).reset();
      Ext.getCmp( 'messages' ).setStatus( '' );
   },

   /**
    * @memberOf Gemma.FileUploadForm
    */
   initComponent : function() {
      Ext.apply( this, {
         width : 500,
         // frame: false,
         frame : true,

         items : [ new Ext.form.FormPanel( {
            id : 'uploadform',
            labelWidth : 50,
            fileUpload : true,
            header : false,
            method : 'POST',
            frame : true,
            url : Gemma.CONTEXT_PATH + '/uploadFile.html', // FileUploadController
            timeout : 15000,
            defaults : {
               anchor : '95%',
               allowBlank : false,
               msgTarget : 'side'
            },
            items : [ {
               xtype : 'fileuploadfield',
               id : 'form-file',
               emptyText : 'Select a file',
               fieldLabel : 'File',
               name : 'file-path',
               listeners : {
                  'fileselected' : function( field, value ) {
                     Ext.getCmp( 'file-upload-button' ).enable();
                  }.createDelegate( this )

               },
               buttonCfg : {
                  text : '',
                  iconCls : 'upload-icon'

               }
            } ],
            buttons : [ {
               text : 'Upload',
               id : 'file-upload-button',
               disabled : true,
               handler : function() {
                  var form = Ext.getCmp( 'uploadform' ).getForm();
                  if ( form.isValid() ) {
                     form.submit( {
                        success : function( form, a ) {
                           var m = a.result;
                           Ext.getCmp( 'messages' ).setText(
                              "File uploaded: " + m.originalFile + "; " + m.size + " bytes" );
                           this.fireEvent( 'finish', m );

                        }.createDelegate( this ),
                        failure : function( form, a ) {
                           Ext.Msg
                              .alert( 'Failure', 'Problem with processing of file on the server: ' + a.result.error );

                           this.fireEvent( 'fail', a.result );
                        }.createDelegate( this ),
                        scope : this
                     } );
                     this.startMonitor();
                  }
               },
               scope : this
            } ]
         } ) ],
         bbar : new Ext.ux.StatusBar( {
            id : 'messages'
         } )
      } );

      Gemma.FileUploadForm.superclass.initComponent.call( this );

      this.addEvents( {
         finish : true,
         fail : true,
         start : true,
         cancel : true
      } );

   },

   processProgressInfo : function( data ) {
      if ( data ) {
         if ( data.status === 'done' ) {
            window.clearInterval( this.timeoutid );
         } else {
            Ext.getCmp( 'messages' ).setStatus( data.bytesRead + "/" + data.totalSize + " bytes read" );
         }
      }
   },

   startMonitor : function() {
      /*
       * Start monitoring progress.
       */
      this.timeoutid = window.setInterval( this.refreshProgress.createDelegate( this ), 2000 );
      this.fireEvent( 'start' );
      this.on( 'finish', function() {
         window.clearInterval( this.timeoutid );
      } );
      this.on( 'fail', function() {
         window.clearInterval( this.timeoutid );
      } );
   },

   refreshProgress : function() {
      var callback = this.processProgressInfo.createDelegate( this );
      var errorHandler = function( e ) {
         window.clearInterval( this.timeoutid );
      };

      FileUploadController.getUploadStatus( {
         callback : callback,
         errorHandler : errorHandler
      } );
   }

} );
