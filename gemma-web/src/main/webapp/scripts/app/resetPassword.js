Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

/**
 * @author keshav
 * 
 */
Ext.onReady( function() {

   Ext.QuickTips.init();

   // turn on validation errors beside the field globally
   Ext.form.Field.prototype.msgTarget = 'side';

   var resetPassword = new Ext.FormPanel( {
      labelWidth : 75, // label settings here cascade unless overridden
      url : 'resetPassword.html',
      frame : true,
      monitorValid : true, // use with formBind in Button for client side validation
      bodyStyle : 'padding:5px 5px 0',
      width : 390,
      keys : [ {
         key : Ext.EventObject.ENTER,
         formBind : true,
         handler : function() {

            resetPassword.getForm().submit( {
               url : this.url,
               method : 'POST',
               success : function() {
                  var target = 'home.html';
                  window.location = target;
               },
               failure : function( form, action ) {
                  var errMsg = '';
                  errMsg = Ext.util.JSON.decode( action.response.responseText );
                  var fontMsg = "<font color='red'>" + errMsg.message + "</font>";
                  Ext.DomHelper.overwrite( 'errorMessage', fontMsg );

                  resetPassword.getForm().reset();
                  Ext.getCmp( 'my-status' ).clearStatus();
               }
            } );

            var sb = Ext.getCmp( 'my-status' );
            sb.showBusy();

         }
      } ],
      defaults : {
         width : 230
      },
      defaultType : 'textfield',

      items : [ {
         fieldLabel : 'Username',
         name : 'username',
         enableKeyEvents : true,
         allowBlank : false,
         vtype : 'alphanum',
         listeners : {
            'keyup' : function() {
               Ext.DomHelper.overwrite( 'errorMessage', "" );
            }
         }
      }, {
         fieldLabel : 'Email',
         id : 'user.email',
         name : 'email',
         allowBlank : false,
         vtype : 'email',
         validationDelay : 1500,
         invalidText : "A valid email address is required"
      } ],

      buttons : [ {
         text : 'Submit',
         formBind : true, // use with monitorValid in Ext.FormPanel for client side
         // validation
         handler : function() {

            resetPassword.getForm().submit( {
               url : this.url,
               method : 'POST',
               success : function() {
                  var target = 'home.html';
                  window.location = target;
               },
               failure : function( form, action ) {
                  var errMsg = '';
                  errMsg = Ext.util.JSON.decode( action.response.responseText );
                  var fontMsg = "<span style='font-color:red;'>" + errMsg.message + "</span>";
                  Ext.DomHelper.overwrite( 'errorMessage', fontMsg );
                  Ext.getCmp( 'my-status' ).clearStatus();
               }
            } );

            var sb = Ext.getCmp( 'my-status' );
            sb.showBusy();

         }
      } ]
   } );

   /**
    * 
    */
   new Ext.Panel( {
      width : 390,
      title : 'Reset Password',
      frame : false,
      renderTo : 'resetPassword',
      items : [ resetPassword ],

      bbar : new Ext.ux.StatusBar( {
         id : 'my-status',
         text : '',
         iconCls : 'default-icon',
         busyText : 'Validating...'
      } )

   } );
} );