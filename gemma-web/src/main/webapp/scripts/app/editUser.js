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

	/**
	 * 
	 */
	var editUser = new Ext.FormPanel({
		renderTo : 'editUser',
		labelWidth : 75, // label settings here cascade unless overridden
		url : 'editUser.html',
		frame : true,
		title : 'Edit User',
		monitorValid : true, // use with formBind in Button for client side validation
		bodyStyle : 'padding:5px 5px 0',
		width : 350,

		keys : [{
			key : Ext.EventObject.ENTER,
			fn : function() {
				editUser.getForm().submit({
					url : this.url,
					method : 'POST',
					success : function() {
						var target = 'mainMenu.html';
						window.location = target;
					},
					failure : function(form, action) {
						var errMsg = '';
						errMsg = "<font color='red'>" + action.result.message + "</font>";
						Element.update('errorMessage', errMsg);

						editUser.getForm().reset();
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
			disabled:true,
			allowBlank : false,
			vtype : 'alphanum'
		}, {
			fieldLabel : 'Email',
			name : 'email',
			allowBlank : false,
			vtype : 'email'
		}, {
			fieldLabel : 'Password',
			id : 'password',
			name : 'password',
			allowBlank : false,
			maxLength : 10,
			inputType : 'password',
			// vtype : 'password',
			validator : function(value) {
				return ((value.match(/[0-9!@#\$%\^&\*\(\)\-_=\+]+/i) && (value.length >= 5)) || "Passwords must be at least 5 characters, containing either a number, or a valid special character (!@#$%^&*()-_=+)")
			}
		}, {
			fieldLabel : 'Confirm Password',
			id : 'passwordConfirm',
			name : 'passwordConfirm',
			allowBlank : false,
			maxLength : 10,
			inputType : 'password',
			// vtype : 'password',
			validator : function(value) {
				return (value == document.getElementById("password").value) || "Your passwords do not match";
			}

		}],

		buttons : [

		{
			text : 'Cancel',
			type : 'cancel',
			minWidth : 75,
			handler : function() {
				window.location = 'mainMenu.html';
			}
		}, {
			text : 'Submit',
			formBind : true, // use with monitorValid in Ext.FormPanel for client side validation
			handler : function() {

				editUser.getForm().submit({
					url : this.url,
					method : 'POST',
					success : function() {
						var target = 'mainMenu.html';
						window.location = target;
					},
					failure : function(form, action) {
						var errMsg = '';
						errMsg = "<font color='red'>" + action.result.message + "</font>";
						Element.update('errorMessage', errMsg);

						editUser.getForm().reset();
						Ext.getCmp('my-status').clearStatus();
					}
				});

				var sb = Ext.getCmp('my-status');
				sb.showBusy();

			}
		}],

		bbar : new Ext.StatusBar({
			id : 'my-status',
			text : 'Ready',
			iconCls : 'default-icon',
			busyText : 'Validating...'
		})

	})

	editUser.form.load({
		url : 'loadUser.html',
		waitMsg : 'Loading'
	})
});