Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * @author keshav
 * @version $Id$
 */
Ext.onReady(function() {

	Ext.QuickTips.init();

	// turn on validation errors beside the field globally
	Ext.form.Field.prototype.msgTarget = 'side';

	var bd = Ext.getBody();

	var signup = new Ext.FormPanel({
		labelWidth : 75, // label settings here cascade unless overridden
		url : 'signup.html',
		frame : true,
		monitorValid : true, // use with formBind in Button for client side validation
		bodyStyle : 'padding:5px 5px 0',
		width : 390,
		keys : [{
			key : Ext.EventObject.ENTER,
			formBind : true,
			handler : function() {

				signup.getForm().submit({
					url : this.url,
					method : 'POST',
					success : function() {
						var target = 'mainMenu.html';
						window.location = target;
					},
					failure : function(form, action) {
						var errMsg = '';
						errMsg = Ext.util.JSON.decode(action.response.responseText);
						var fontMsg = "<font color='red'>" + errMsg.message + "</font>";
						Element.update('errorMessage', fontMsg);

						signup.getForm().reset();
						Ext.getCmp('my-status').clearStatus();
					}
				});

				var sb = Ext.getCmp('my-status');
				sb.showBusy();

			}
		}],
		defaults : {
			width : 230
		},
		defaultType : 'textfield',

		items : [{
			fieldLabel : 'Username',
			name : 'username',
			allowBlank : false,
			vtype : 'alphanum'
		}, {
			fieldLabel : 'Email',
			id : 'email',
			name : 'email',
			allowBlank : false,
			vtype : 'email',
			validationDelay : 1500,
			invalidText : "A valid email address is required"
		}, {
			fieldLabel : 'Confirm Email',
			id : 'emailConfirm',
			name : 'emailConfirm',
			allowBlank : false,
			vtype : 'email',
			validator : function(value) {
				return (value == document.getElementById("email").value) || "Your email addresses do not match";
			}
		}],

		buttons : [{
			text : 'Submit',
			formBind : true, // use with monitorValid in Ext.FormPanel for client side validation
			handler : function() {

				signup.getForm().submit({
					url : this.url,
					method : 'POST',
					success : function() {
						var target = 'mainMenu.html';
						window.location = target;
					},
					failure : function(form, action) {
						var errMsg = '';
						errMsg = Ext.util.JSON.decode(action.response.responseText);
						var fontMsg = "<font color='red'>" + errMsg.message + "</font>";
						Element.update('errorMessage', fontMsg);

						signup.getForm().reset();
						Ext.getCmp('my-status').clearStatus();
					}
				});

				var sb = Ext.getCmp('my-status');
				sb.showBusy();

			}
		}]
	});

	/**
	 * 
	 */
	var panel = new Ext.Panel({
		width : 390,
		title : 'Create a new account',
		frame : false,
		renderTo : 'signup',
		items : [signup],

		bbar : new Ext.StatusBar({
			id : 'my-status',
			text : '',
			iconCls : 'default-icon',
			busyText : 'Validating...'
		})

	});
});
