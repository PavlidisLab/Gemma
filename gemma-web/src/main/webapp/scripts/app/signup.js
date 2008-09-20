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
			fn : function() {
				signup.getForm().submit({
					url : this.url,
					method : 'POST',
					success : function() {
						var target = 'mainMenu.html';
						window.location = target;
					},
					failure : function(form, action) {
						var errMsg;
						if (action.failureType == 'server') {
							obj = Ext.util.JSON.decode(action.response.responseText);
							errMsg = "<font color='red'>Signup Failed</font>"
							Element.update('errorMessage', errMsg);
						} else {
							errMsg = "<font color='red'>Warning!, Authentication server is unreachable</font>"
							Element.update('errorMessage', errMsg);
						}
						signup.getForm().reset();
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
			name : 'email',
			allowBlank : false,
			vtype : 'email',
			validationDelay : 1500,
			invalidText : "A valid email address is required"
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
						var errMsg;
						if (action.failureType == 'server') {
							obj = Ext.util.JSON.decode(action.response.responseText);
							errMsg = "<font color='red'>Signup Failed</font>"
							Element.update('errorMessage', errMsg);
						} else {
							errMsg = "<font color='red'>Warning!, Authentication server is unreachable</font>"
							Element.update('errorMessage', errMsg);
						}
						signup.getForm().reset();
					}
				});

				var sb = Ext.getCmp('my-status');
				sb.showBusy();

			}
		}]
	})

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

	})
});
