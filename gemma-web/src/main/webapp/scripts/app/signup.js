Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * @author keshav
 * @version $Id$
 */
Ext.onReady( function() {
   Ext.QuickTips.init();

   // control how/where validation errors show up.
   Ext.form.Field.prototype.msgTarget = 'side';

   /**
    * Submit the signup form
    */
   submit = function() {

      Element.update( 'errorMessage', "" );

      if ( !signup.getForm().isValid() ) {
         Element.update( 'errorMessage', "Form is not valid, check entries before clicking 'submit'" );
         return;
      }

      signup.getForm().submit( {
         url : this.url,
         method : 'POST',
         success : function() {
            var target = 'home.html';
            window.location = target;
         },
         failure : function( form, action ) {
            var msg;
            if ( action.failureType === 'client' ) {
               msg = "Invalid form";
            } else {

               var errMsg = Ext.util.JSON.decode( action.response.responseText );
               msg = "<font color='red'>" + errMsg.message + "</font>";
            }
            Element.update( 'errorMessage', msg );

            Ext.getCmp( 'signup.captcha' ).reset();
            Ext.getCmp( 'signup.password.field' ).reset();
            Ext.getCmp( 'signup.passwordConfirm.field' ).reset();

            Ext.getCmp( 'my-status' ).clearStatus();
         }
      } );

      var sb = Ext.getCmp( 'my-status' );
      sb.showBusy();
   };

   new Ext.FormPanel( {
      labelWidth : 140, // label settings here cascade unless overridden
      url : '/Gemma/signup.html',
      renderTo : 'signup',
      frame : true,
      monitorValid : true, // use with formBind in Button for client side validation
      bodyStyle : 'padding:5px 5px 0',
      width : 540,
      keys : [ {
         key : Ext.EventObject.ENTER,
         formBind : true,
         handler : submit
      } ],
      defaults : {
         width : 300
      },
      defaultType : 'textfield',

      items : [
               {
                  fieldLabel : 'Username',
                  name : 'signup.username.field',
                  allowBlank : false,
                  vtype : 'alphanum'
               },
               {
                  fieldLabel : 'Email',
                  id : 'signup.email.field',
                  name : 'email',
                  allowBlank : false,
                  vtype : 'email',
                  validationDelay : 1500,
                  invalidText : "A valid email address is required"
               },
               {
                  fieldLabel : 'Confirm Email',
                  id : 'signup.emailConfirm.field',
                  name : 'emailConfirm',
                  allowBlank : false,
                  vtype : 'email',
                  validator : function( value ) {
                     return value == document.getElementById( "signup.email.field" ).value
                        || "Your email addresses do not match";
                  }
               }, {
                  fieldLabel : 'Password',
                  id : 'signup.password.field',
                  name : 'password',
                  allowBlank : false,
                  maxLength : 16,
                  minLength : 6,
                  inputType : 'password'
               }, {
                  fieldLabel : 'Confirm password',
                  id : 'signup.passwordConfirm.field',
                  name : 'passwordConfirm',
                  inputType : 'password',
                  vtype : 'password',
                  allowBlank : false,
                  initialPassField : 'signup.password.field'

               }, {
                  xtype : 'recaptcha',
                  name : 'recaptcha',
                  id : 'signup.captcha',
                  // FIXME don't hardcode!
                  publickey : '6Lf4KAkAAAAAADFjpOSiyfHhlQ1pkznapAnmIvyr',
                  theme : 'white',
                  lang : 'en',
                  allowBlank : false

               } ],

      buttons : [ {
         text : 'Submit',
         formBind : true, // use with monitorValid in Ext.FormPanel for client side validation
         handler : submit
      } ],
      bbar : new Ext.ux.StatusBar( {
         id : 'my-status',
         text : '',
         iconCls : 'default-icon',
         busyText : 'Validating...'
      } )
   } );

} );

/**
 * See http://www.extjs.com/forum/showthread.php?p=398496
 * 
 * @cfg {String} publickey The key to generate your recaptcha
 * @cfg {String} theme The name of the theme
 * @cfg {string} lang The language (e.g., 'en')
 * 
 * @class Ext.ux.Recaptcha
 * @extends Ext.form.Field
 */
Ext.ux.Recaptcha = Ext.extend( Ext.form.Field, {

   lang : 'en',

   fieldLabel : "Prove you are human",

   theme : 'white',

   width : 310,
   fieldClass : '',

   reset : function() {
      Recaptcha.reload();
   },

   destroy : function() {
      Ext.ux.Recaptcha.superclass.destroy.call( this );
      Recaptcha.destroy();
   },

   validateValue : function() {
      if ( Ext.get( 'recaptcha_response_field' ).getValue().length > 0 ) {
         return true;
      }
      this.markInvalid( "Recaptcha must be filled in" );
      return false;
   },

   filterValidation : function( e ) {
      if ( !e.isNavKeyPress() ) {
         this.validationTask.delay( this.validationDelay );
      }
   },

   allowBlank : false,

   onRender : function( ct, position ) {

      if ( !this.el ) {

         this.el = document.createElement( 'div' );
         this.el.id = this.getId();

         Recaptcha.create( this.publickey, this.el, {
            theme : this.theme,
            lang : this.lang,
            callback : Recaptcha.focus_response_field

         } );

      }

      Ext.ux.Recaptcha.superclass.onRender.call( this, ct, position );

   },
   initComponent : function() {
      Ext.ux.Recaptcha.superclass.initComponent.call( this );
      this.addEvents( 'keyup' );
   },

   initEvents : function() {
      Ext.ux.Recaptcha.superclass.initEvents.call( this );
      this.validationTask = new Ext.util.DelayedTask( this.validate, this );
      this.mon( this.el, 'keyup', this.filterValidation, this );
   }

} );

Ext.reg( 'recaptcha', Ext.ux.Recaptcha );

/**
 * 
 */
Ext.apply( Ext.form.VTypes, {

   password : function( val, field ) {
      if ( field.initialPassField ) {
         var pwd = Ext.getCmp( field.initialPassField );
         return (val == pwd.getValue());
      }
      return true;
   },

   passwordText : 'Passwords do not match'
} );
