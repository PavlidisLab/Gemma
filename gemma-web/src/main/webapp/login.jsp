<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%-- 
author: keshav
version: $Id$
--%>

<%@ include file="/common/taglibs.jsp"%>
<%@ page
	import="org.springframework.security.context.SecurityContextHolder"%>
<%@ page import="org.springframework.security.context.SecurityContext"%>

<html>
	<head>
		<title>Login</title>
	</head>
	<body style="overflow: hidden">

		<script>
		
	Ext.namespace('Gemma');
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

	Ext.onReady( function()
	{
		Ext.QuickTips.init();
		
		var login = new Ext.FormPanel(
		{
			labelWidth :90,
			url :'<%=request.getContextPath()%>/j_security_check',
			method :'POST',
			id :'_loginForm',
			standardSubmit :true,
			frame :true,
			title :'Login',
			bodyStyle :'padding:5px 5px 0',
			iconCls :'user-suit',
			width :350,
			monitorValid:true,
			keys:
			[
				{
					key: Ext.EventObject.ENTER,
					fn: function()
			        {
						var sb = Ext.getCmp('my-status');
						sb.showBusy();
						document.getElementsByTagName("form")[1].action = "<%=request.getContextPath()%>/j_security_check";
						document.getElementsByTagName("form")[1].submit();
			        }
				}
			],
			defaults :
		    {
				<%-- 
					enter defaults here
					width :230 
				--%>
			},
			defaultType :'textfield',
			items :
			[
				{
					fieldLabel :'Username',
					name :'j_username',
					id :'j_username',
					allowBlank :false
				},
				{
					fieldLabel :'Password',
					name :'j_password',
					id :'j_password',
					allowBlank :false,
					inputType :'password'
				},{
					fieldLabel: 'Remember Me',
					boxLabel : 'rememberMe',
					name : 'rememberMe',
					inputType: 'checkbox'
				}
					
			],
			
			buttons :
					[
						{
							text :'Register',
							type :'register',
							minWidth: 75,
							handler :function()
							{	
								var sb = Ext.getCmp('my-status');
								sb.showBusy();
								document.getElementsByTagName("form")[1].action = "<%=request.getContextPath()%>/register.html";
								document.getElementsByTagName("form")[1].submit();
							}
						},
						{
							text :'Login',
							formBind:true,
							type :'submit',
							minWidth: 75,
							handler :function()
			        		{
								var sb = Ext.getCmp('my-status');
								sb.showBusy();
								document.getElementsByTagName("form")[1].action = "<%=request.getContextPath()%>/j_security_check";
								document.getElementsByTagName("form")[1].submit();
			        		}					
						}
					],
					
		   bbar: new Ext.StatusBar(
			{
				id: 'my-status',
			    text: 'Ready',
			    iconCls: 'default-icon',
			    busyText: 'Validating...'
		<%
			if (!(request.getParameter("login_error") == null))
			{
			    String errorMsg = request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION").toString();
			    errorMsg = errorMsg.substring(errorMsg.indexOf(':') + 1, errorMsg.length());
		%>
			    ,items:
				[
					'<div style=\'color: red; vertical-align: top; padding-right: 5px;\'><%=errorMsg%><br/></div>'
				]
		<%	} %>
		    })
		});
		login.render(document.getElementById('_login'));
	});

</script>
		<div id="login-mask" style=""></div>
		<div id="login">
			<div id="_login" class="login-indicator"></div>
		</div>
	</body>
</html>