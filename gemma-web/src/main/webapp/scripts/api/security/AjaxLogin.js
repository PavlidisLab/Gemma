/*
 * Widget for Ajax style login
 * 
 */

Ext.namespace( 'Gemma.Application', 'Gemma.AjaxLogin' );

/**
 * Gemma.Application.currentUser is a global variable for keeping track of user logging in and out. Components should
 * listen to this variable's event and update themselves accordingly.
 * 
 * When a user logs in, the "logIn" event is fired with two parameters: userName (string) and isAdmin (boolean). These
 * parameters are also stored in the variable as fields: Gemma.Application.currentUser.userName and
 * Gemma.Application.currentUser.isAdmin.
 * 
 * When a user logs out, no parameters are passed with the "logOut" event
 * 
 * Event firing is triggered by the components responsible for logging users in and out. These components set field
 * values too.
 * 
 * (this last task could be done by Gemma.Application.currentUser but adding listeners to the observable object wasn't
 * working...)
 * 
 */
Gemma.Application.currentUser = new Ext.util.Observable();
Gemma.Application.currentUser.addEvents( 'logIn', 'logOut' );

Gemma.AjaxLogin.logoutFn = function() {
   Ext.Ajax.request( {
      url : Gemma.CONTEXT_PATH + '/j_spring_security_logout',
      method : 'GET',

      /**
       * @memberOf Gemma.AjaxLogin
       */
      success : function( response, options ) {
         var hasuser = Ext.get( 'hasUser' );
         if ( hasuser ) {
            hasuser.value = '';
         }
         var hasadmin = Ext.get( 'hasAdmin' );
         if ( hasadmin ) {
            hasadmin.value = '';
         }

         var hasname = Ext.get( 'username-logged-in' );
         if ( hasname ) {
            hasname.value = '';
         }

         Gemma.Application.currentUser.fireEvent( 'logOut' );

         /*
          * if the user is on a private page and logs out, the page should change to hide the private content right now,
          * the only way to ensure this happens is to refresh the page also, since there isn't an easy way to tell which
          * pages are private (yet) we need to refresh all pages on logout
          * 
          * this is the default behaviour unless otherwise specified in the page's jsp
          */
         var reloadOnLogout = Ext.getDom( 'reloadOnLogout' );
         if ( reloadOnLogout == undefined || reloadOnLogout.getValue() === "true" ) {
            Ext.getBody().mask( "Logging you out" );
            window.location.reload();
         }

      },
      failure : function( response, options ) {
         alert( 'Failed to log you out of Gemma.' );
      },
      scope : this,
      disableCaching : true
   } );
};

/**
 * 
 * @param {Object}
 *           reloadPageOnLogin pass in true if you want the page to be reloaded after the user logs in
 */
Gemma.AjaxLogin.loginWindow = null;
Gemma.AjaxLogin.showLoginWindowFn = function( reloadPageOnLogin ) {
   /*
    * overlapping ids make the ajax login break if it's opened from a page where the classic login form exists (ex:
    * login.jsp)
    * 
    * this if statement prevents the ajax login from popping up if the classic login exists on the page pretty hacky way
    * to deal with this bug, but ok for now(?)
    */

   if ( Ext.get( '_login' ) ) {
      return;
   }
   if ( Gemma.AjaxLogin.loginWindow === null ) {
      // create
      Gemma.AjaxLogin.loginWindow = new Gemma.AjaxLogin.AjaxLoginWindow();
   }
   // show
   if ( reloadPageOnLogin ) {
      Ext.apply( Gemma.AjaxLogin.loginWindow, {
         reloadPageOnLogin : reloadPageOnLogin
      } );
   }
   Ext.getBody().mask();
   Gemma.AjaxLogin.loginWindow.show();
};

Gemma.AjaxLogin.AjaxLoginWindow = Ext.extend( Ext.Window, {
   id : '_ajaxLogin',
   width : 360,
   shadow : true,
   resizable : false,
   closeAction : 'hide',
   reloadPageOnLogin : false,
   extraText : new Ext.Panel( {
      border : false,
      html : ''
   } ),
   // convenient for these to be listeners so window can respond to child form panel
   listeners : {
      "login_success" : function( userName, isAdmin ) {
         Ext.getBody().unmask();

         Gemma.Application.currentUser.userName = userName;
         Gemma.Application.currentUser.isAdmin = isAdmin;

         this.hide();
         /*
          * FIX FOR NOW if user was on a page with an error message, potentially caused by trying to access a private
          * entity's page, we should refresh the page when they login, in case this fixes the problem or allows them to
          * see the entity
          */
         var myReloadPageOnLogin = Ext.getDom( 'reloadOnLogin' );
         if ( myReloadPageOnLogin && myReloadPageOnLogin.value === "true" ) {
            this.reloadPageOnLogin = true;
         }
         if ( this.reloadPageOnLogin ) {
            Ext.getBody().mask( "Logging you in" );
            window.location.reload();
         } else {
            Gemma.Application.currentUser.fireEvent( 'logIn', userName, isAdmin );
         }

      },
      "register_requested" : function() {
         Ext.getBody().unmask();
         this.hide();
         this.launchRegisterWidget();
      },
      "login_cancelled" : function() {
         this.hide();
         Ext.getBody().unmask();

      },
      "show" : function() {
         Ext.getBody().mask();// ('Processing ...');
      },
      "hide" : function() {
         Ext.getCmp( "_loginForm" ).getForm().reset();
         Ext.getBody().unmask();
      },
      "close" : function() {
         Ext.getCmp( "_loginForm" ).getForm().reset();
         Ext.getBody().unmask();
      }
   },
   launch : function() {
      /*
       * overlapping ids make the ajax login break if it's opened from a page where the classic login form exists (ex:
       * login.jsp)
       * 
       * this prevents the ajax login from popping up if the classic login exists on the page not the best way to deal
       * with this bug, but since we're phasing the login page out anyway I think this is fine for now (Thea)
       */
      var onLoginPage = Ext.get( '_loginForm' );
      if ( !onLoginPage ) {
         this.targetElement = Ext.getBody();
         this.show();
      }
   },
   launchRegisterWidget : function() {
      if ( this.ajaxRegister === null || !this.ajaxRegister ) {

         // Check to see if another register widget is open (rare case but possible)
         var otherOpenRegister = Ext.getCmp( '_ajaxRegister' );

         // if another register widget is open, fire its event to close it and destroy it before launching this one
         if ( otherOpenRegister && otherOpenRegister !== null ) {
            otherOpenRegister.fireEvent( "register_cancelled" );
         }
         this.ajaxRegister = new Gemma.AjaxLogin.AjaxRegister( {
            name : 'ajaxRegister',
            closable : false,
            closeAction : 'hide',
            title : 'Please Register'
         } );

         this.ajaxRegister.on( "register_cancelled", function() {
            this.ajaxRegister.close();
            this.ajaxRegister = null;
            Ext.getBody().unmask();
         }, this );

         this.ajaxRegister.on( "register_success", function() {
            this.ajaxRegister.close();
            this.ajaxRegister = null;
            Ext.getBody().unmask();
         }, this );
      }
      Ext.getBody().mask();
      this.ajaxRegister.show();
   },

   initComponent : function() {

      Ext.apply( this, {

         items : [
                  this.extraText,
                  new Ext.FormPanel(
                     {
                        labelWidth : 90,
                        id : '_loginForm',
                        frame : true,
                        bodyStyle : 'padding:5px 5px 0',
                        iconCls : 'user-suit',
                        width : 350,
                        monitorValid : true,
                        keys : [ {
                           key : Ext.EventObject.ENTER,
                           fn : this.submitHandler
                        } ],
                        defaults : {},
                        defaultType : 'textfield',
                        items : [
                                 {
                                    fieldLabel : 'Username',
                                    name : 'j_username',
                                    id : 'j_username',
                                    allowBlank : false
                                 },
                                 {
                                    fieldLabel : 'Password',
                                    name : 'j_password',
                                    id : 'j_password',
                                    allowBlank : false,
                                    inputType : 'password'
                                 },
                                 {
                                    fieldLabel : 'Remember Me',
                                    boxLabel : 'rememberMe',
                                    // defined in AbstractRememberMeServices.
                                    id : '_spring_security_remember_me',
                                    name : '_spring_security_remember_me',
                                    inputType : 'checkbox'
                                 },
                                 {
                                    html : '<a href="' + Gemma.CONTEXT_PATH + '/passwordHint.html">'
                                       + Gemma.HelpText.WidgetDefaults.AjaxLogin_AjaxLoginWindow.passwordHintLink
                                       + '</a>',
                                    name : 'passwordHint',
                                    id : 'passwordHint',
                                    xtype : 'label',
                                    hidden : false
                                 }, {
                                    id : 'ajaxLoginTrue',
                                    name : 'ajaxLoginTrue',
                                    hidden : true,
                                    value : 'true'

                                 } ],

                        buttons : [ {
                           text : Gemma.HelpText.WidgetDefaults.AjaxLogin_AjaxLoginWindow.registerButton,
                           minWidth : 75,
                           handler : this.registerHandler,
                           scope : this
                        }, {
                           text : "Cancel",
                           handler : this.cancel,
                           scope : this
                        }, {
                           text : 'Login',
                           formBind : true,
                           type : 'submit',
                           method : 'POST',
                           minWidth : 75,
                           handler : this.submitHandler

                        } ]
                     } ) ], // end of items for outer panel.
         bbar : new Ext.ux.StatusBar( {
            id : 'my-status_ajax',
            text : '',
            iconCls : 'default-icon',
            busyText : 'Logging you in...',
            items : [ '<div id="ajax-error" style="color: red; vertical-align: top; padding-right: 5px;"><br/></div>' ]
         } )

      } );

      this.addEvents( 'login_success', 'login_failure', 'register_requested', 'login_cancelled' );

      Gemma.AjaxLogin.AjaxLoginWindow.superclass.initComponent.call( this );

   },// end initComponent
   submitHandler : function() {
      var errordiv = Ext.get( 'ajax-error' );
      Ext.DomHelper.overwrite( errordiv, "" );

      var sb = Ext.getCmp( 'my-status_ajax' );
      sb.showBusy();
      Ext.getCmp( "_loginForm" ).getForm().submit( {
         url : Gemma.CONTEXT_PATH + '/j_spring_security_check',
         success : function( form, action ) {

            var sb = Ext.getCmp( 'my-status_ajax' );
            if ( sb ) {
               sb.clearStatus();
            }

            var link = Ext.getDom( 'footer-login-link' );
            if ( link ) {
               link.href = Gemma.CONTEXT_PATH + "/j_spring_security_logout";
               link.innerHTML = "Logout";
            }

            var dataMsg = Ext.util.JSON.decode( action.response.responseText );

            var loggedInAs = Ext.getDom( 'username-logged-in' );
            if ( loggedInAs ) {
               loggedInAs.value = dataMsg.user;
            }

            var hasuser = Ext.getDom( 'hasUser' );
            if ( hasuser ) {
               hasuser.value = true;
            }
            var hasadmin = Ext.getDom( 'hasAdmin' );
            if ( hasadmin && dataMsg.isAdmin ) {
               hasadmin.value = true;
            }

            // fire event instad of method call because actions need to be taken at window scope
            var loginWidget = Ext.getCmp( "_ajaxLogin" );
            if ( loginWidget ) {
               loginWidget.fireEvent( "login_success", dataMsg.user, dataMsg.isAdmin );
            }

         },
         failure : function( form, action ) {
            var sb = Ext.getCmp( 'my-status_ajax' );
            if ( sb ) {
               sb.clearStatus();
            }

            var erdiv = Ext.get( 'ajax-error' );
            if ( erdiv ) {
               Ext.DomHelper.overwrite( erdiv, Gemma.HelpText.WidgetDefaults.AjaxLogin_AjaxLoginWindow.invalidLogin );
            }

            var loginWidget = Ext.getCmp( "_ajaxLogin" );
            if ( loginWidget ) {
               loginWidget.fireEvent( "login_failure" );
            }

            loggedInAs = Ext.getDom( 'username-logged-in' );
            if ( loggedInAs ) {
               loggedInAs.value = "";
            }
         }

      } );

   },

   registerHandler : function() {

      this.fireEvent( "register_requested" );

   },

   cancel : function() {

      this.fireEvent( 'login_cancelled' );
   }

} );
