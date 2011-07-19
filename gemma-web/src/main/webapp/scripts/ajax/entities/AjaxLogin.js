
/*
 * Widget for Ajax style login
 *  
 */
Ext.namespace('Gemma');


Gemma.AjaxLogin = Ext.extend(Ext.Window, {
	id: '_ajaxLogin',
			
	width : 350,
	shadow : true,
	
	

	initComponent : function() {
	
	
		Ext.apply(this, {
			
			items:[new Ext.FormPanel(
		{
			labelWidth :90,			
			id :'_loginForm',			
			frame : true,
			bodyStyle :'padding:5px 5px 0',
			iconCls :'user-suit',
			width :350,
			monitorValid:true,
			keys:
			[
				{
					key: Ext.EventObject.ENTER,
					fn: this.submitHandler
				}
			],
			defaults :
		    {
				
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
					fieldLabel : '<a href="/Gemma/passwordHint.html">Forgot your password?</a>',
					name :'passwordHint',
					id :'passwordHint',
					labelSeparator:'',
					hidden : true
				},{
					fieldLabel: 'Remember Me',
					boxLabel : 'rememberMe',
					// defined in AbstractRememberMeServices.
					id : '_spring_security_remember_me',
					name : '_spring_security_remember_me',
					inputType: 'checkbox'
				},
				{
					id : 'ajaxLoginTrue',
					name : 'ajaxLoginTrue',
					hidden : true,
					value:'true'
					
				}
					
			],
			
			buttons :
					[ 
						{
							text :'Need an account? Register',							
							minWidth: 75,
							handler : this.registerHandler,
							scope :this
						},{
							text: "Cancel",
							handler: this.cancel,
							scope: this
						},
						{
							text :'Login',
							formBind:true,
							type :'submit',
							method : 'POST',
							minWidth: 75,
							handler : this.submitHandler
							
						}
						
					]
		   })], // end of items for outer panel.
					
		   bbar: new Ext.ux.StatusBar(
			{
				id: 'my-status',
			    text: '',
			    iconCls: 'default-icon',
			    busyText: 'Logging you in...',
			    items:
					[
						'<div id="ajax-error" style="color: red; vertical-align: top; padding-right: 5px;"><br/></div>'
					]
			}),
			
			
		});        
		
		
		this.addEvents('login_success', 'login_failure', 'register_requested', 'login_cancelled');

		Gemma.AjaxLogin.superclass.initComponent.call(this);

		

	},// end initComponent
	
	submitHandler : function(){
		var errordiv = Ext.get('ajax-error');
		Ext.DomHelper.overwrite(errordiv, "");
		
		
		var sb = Ext.getCmp('my-status');
		sb.showBusy();
		Ext.getCmp("_loginForm").getForm().submit(
		{
			url :'/Gemma/j_spring_security_check',
			success: function(form, action){						
						var sb = Ext.getCmp('my-status');
						sb.clearStatus();
						
						
						var link = Ext.getDom('footer-login-link');
						link.href="/Gemma/j_spring_security_logout";
						link.innerHTML="Logout";
						
						var dataMsg = Ext.util.JSON.decode(action.response.responseText);
						
						var loggedInAs = Ext.getDom('footer-login-status');
						loggedInAs.innerHTML="Logged in as: "+dataMsg.user;
						
						var hasuser = Ext.getDom('hasUser');						
						hasuser.value= true;						
						
						var loginWidget = Ext.getCmp("_ajaxLogin");
						loginWidget.fireEvent("login_success");						
						
						var dataMsg = Ext.util.JSON.decode(action.response.responseText);							
						
						
						
	
					},
			failure: function(form, action){
					var sb = Ext.getCmp('my-status');
					sb.clearStatus();
					
					var erdiv = Ext.get('ajax-error');
					Ext.DomHelper.overwrite(erdiv,'Invalid Username/Password');
					
					var loginWidget = Ext.getCmp("_ajaxLogin");
					loginWidget.fireEvent("login_failure");
					
	
	
			},
			
		}
		
		
		
		);
		
		
		
	},
	
	registerHandler : function(){
				
		this.fireEvent("register_requested");
		
		
	},
	
	cancel : function(){
		
		this.fireEvent('login_cancelled');
	}

	
	
});


