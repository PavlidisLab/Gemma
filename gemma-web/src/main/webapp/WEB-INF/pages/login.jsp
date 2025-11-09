<%@ include file="/WEB-INF/common/taglibs.jsp"%>
<%-- 
author: keshav

--%>


<html>
<head>
<title>Login</title>

<script type="text/javascript">
   Ext.namespace( 'Gemma' );
   Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';

   Ext.onReady( function() {
      Ext.QuickTips.init();

      var error = Ext.get( 'login_error_msg' ) ? Ext.get( 'login_error_msg' ).getValue() : "";

      var login = new Ext.Panel( {
         frame : false,
         title : 'Login',
         width : 350,
         items : [ new Ext.FormPanel( {
            labelWidth : 90,
            url : '${pageContext.request.contextPath}/j_spring_security_check',
            method : 'POST',
            id : '_loginForm',
            standardSubmit : true,
            frame : true,
            bodyStyle : 'padding:5px 5px 0',
            iconCls : 'user-suit',
            width : 350,
            monitorValid : true,
            keys : [ {
               key : Ext.EventObject.ENTER,
               fn : function() {
                  var sb = Ext.getCmp( 'my-status' );
                  sb.showBusy();
                  Ext.getCmp( "_loginForm" ).getForm().submit();
               }
            } ],
            defaults : {
            //enter defaults here
            //width :230 

            },
            defaultType : 'textfield',
            items : [ {
               fieldLabel : 'Username',
               name : 'j_username',
               id : 'j_username',
               allowBlank : false,
               listeners : {
                  afterrender : function( field ) {
                     field.focus();
                  }
               }
            }, {
               fieldLabel : 'Password',
               name : 'j_password',
               id : 'j_password',
               allowBlank : false,
               inputType : 'password'
            }, {
               fieldLabel : '<a href="${pageContext.request.contextPath}/passwordHint.html">Forgot your password?</a>',
               name : 'passwordHint',
               id : 'passwordHint',
               labelSeparator : '',
               hidden : true
            }, {
               fieldLabel : 'Remember Me',
               boxLabel : 'rememberMe',
               // defined in AbstractRememberMeServices.
               id : '_spring_security_remember_me',
               name : '_spring_security_remember_me',
               inputType : 'checkbox'
            }

            ],

            buttons : [ {
               text : 'Login',
               formBind : true,
               type : 'submit',
               minWidth : 75,
               handler : function() {
                  var sb = Ext.getCmp( 'my-status' );
                  sb.showBusy();
                  Ext.getCmp( "_loginForm" ).getForm().submit();
               }
            } ]
         } ) ], // end of items for outer panel.

         bbar : new Ext.ux.StatusBar( {
            id : 'my-status',
            text : '',
            iconCls : 'default-icon',
            busyText : 'Logging you in...',
            items : [ '<div style="color: red; vertical-align: top; padding-right: 5px;">' + error + '<br/></div>' ]
         } )
      } );
      login.render( document.getElementById( '_login' ) );

   } );
</script>
</head>
<body>
	<c:if test='${not empty sessionScope["SPRING_SECURITY_LAST_EXCEPTION"]}'>
		<input id="login_error_msg" type="hidden" value='Error: ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}' />
	</c:if>

	<p style='margin-left: 200px; width: 500; padding: 10px'>
		Users do not need to log on or register for many uses of Gemma. An account is only needed if you want to take
		advantage of data upload or 'favorite search' and similar functionality. <strong>Need an account? <a
			href="<c:url  value='register.html' />">Register</a>
		</strong>
	</p>

	<div id="login-mask" style=""></div>
	<div align="center" id="login">
		<div id="_login" class="login-indicator"></div>
	</div>



</body>
</html>