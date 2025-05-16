Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

/**
 * @author keshav
 *
 */
Ext.onReady(function () {

    Ext.QuickTips.init();

    // turn on validation errors beside the field globally
    Ext.form.Field.prototype.msgTarget = 'side';

    /**
     *
     */
    var editUser = new Ext.FormPanel({
        renderTo: 'editUser',
        labelWidth: 165, // label settings here cascade unless overridden
        url: 'editUser.html',
        frame: true,
        // title : 'Edit User',
        monitorValid: true, // use with formBind in Button for client side validation
        bodyStyle: 'padding:5px 5px 0',
        width: 450,

        keys: [{
            key: Ext.EventObject.ENTER,
            handler: function (e) {
                e.getTarget().getForm().submit({
                    url: this.url,
                    method: 'POST',
                    success: function () {
                        var target = 'home.html';
                        window.location = target;
                    },
                    failure: function (form, action) {
                        var errMsg = '';
                        errMsg = "<font color='red'>" + action.result.message + "</font>";
                        Element.update('errorMessage', errMsg);

                        // editUser.getForm().reset();
                        Ext.getCmp('my-status').clearStatus();
                    }
                });

                var sb = Ext.getCmp('my-status');
                sb.showBusy();
            }
        }],
        defaults: {
            width: 230
        },
        defaultType: 'textfield',

        items: [{
            xtype: 'hidden',
            fieldLabel: 'Username',
            name: 'username'
        }, {
            fieldLabel: 'Email',
            name: 'email',
            allowBlank: false,
            vtype: 'email'
        }, {
            fieldLabel: 'Current password *',
            maxLength: 20,
            inputType: 'password',
            allowBlank: false,
            id: 'oldPassword',
            name: 'oldPassword'
        }, {
            fieldLabel: 'New password',
            id: 'password',
            name: 'password',
            allowBlank: true,
            maxLength: 16,
            minLength: 5,
            inputType: 'password'
        }, {
            fieldLabel: 'Confirm new password',
            id: 'passwordConfirm',
            name: 'passwordConfirm',
            allowBlank: true,
            inputType: 'password',
            vtype: 'password',
            initialPassField: 'password'
        }],

        buttons: [

            {
                text: 'Cancel',
                type: 'cancel',
                minWidth: 75,
                handler: function () {
                    window.location = 'home.html';
                }
            }, {
                text: 'Submit',
                formBind: true, // use with monitorValid in Ext.FormPanel for client side
                // validation
                handler: function (e) {
                    editUser.getForm().submit({
                        url: this.url,
                        method: 'POST',
                        success: function () {
                            var target = 'home.html';
                            window.location = target;
                        },
                        failure: function (form, action) {
                            var errMsg = '';
                            errMsg = "<font color='red'>" + action.result.message + "</font>";
                            Element.update('errorMessage', errMsg);

                            e.getTarget().getForm().reset();
                            Ext.getCmp('my-status').clearStatus();
                        }
                    });

                    var sb = Ext.getCmp('my-status');
                    sb.showBusy();

                }
            }],

        bbar: new Ext.ux.StatusBar({
            id: 'my-status',
            text: 'Ready',
            iconCls: 'default-icon',
            busyText: 'Validating...'
        })

    });

    editUser.form.load({
        url: 'loadUser.html',
        waitMsg: 'Loading',
        success: function (f, action) {
            // in case we need it.
        }
    });
});


Ext.apply(Ext.form.VTypes, {

    password: function (val, field) {
        if (field.initialPassField) {
            var pwd = Ext.getCmp(field.initialPassField);
            return (val == pwd.getValue());
        }
        return true;
    },

    passwordText: 'Passwords do not match'
});
