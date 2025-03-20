/*
 * Widget for Ajax styleregister
 * 
 */
Ext.namespace('Gemma', 'Gemma.AjaxLogin');

Gemma.AjaxLogin.AjaxRegister = Ext
    .extend(
        Ext.Window,
        {

            id: '_ajaxRegister',

            title: 'register',

            closeAction: 'hide',
            resizable: false,

            /**
             * @memberOf Gemma.AjaxLogin.AjaxRegister
             */
            initComponent: function () {

                Ext
                    .apply(
                        this,
                        {
                            items: [new Ext.FormPanel(
                                {
                                    id: '_registerForm',
                                    labelWidth: 140, // label settings here cascade unless overridden
                                    url: Gemma.CONTEXT_PATH + '/signup.html',
                                    frame: true,
                                    monitorValid: true, // use with formBind in Button for client side validation
                                    bodyStyle: 'padding:5px 5px 0',
                                    width: 540,
                                    keys: [{
                                        key: Ext.EventObject.ENTER,
                                        formBind: true,
                                        handler: this.submitHandler
                                    }],
                                    defaults: {
                                        width: 300
                                    },
                                    defaultType: 'textfield',

                                    items: [
                                        {
                                            fieldLabel: 'Username',
                                            name: 'username',
                                            allowBlank: false,
                                            vtype: 'alphanum'
                                        },
                                        {
                                            fieldLabel: 'Email',
                                            id: 'email',
                                            name: 'email',
                                            allowBlank: false,
                                            vtype: 'email',
                                            validationDelay: 1500,
                                            invalidText: "A valid email address is required"
                                        },
                                        {
                                            fieldLabel: 'Confirm Email',
                                            id: 'emailConfirm',
                                            name: 'emailConfirm',
                                            allowBlank: false,
                                            vtype: 'email',
                                            validator: function (value) {
                                                return (value == document.getElementById("email").value)
                                                    || "Your email addresses do not match";
                                            }
                                        }, {
                                            fieldLabel: 'Password',
                                            id: 'password',
                                            name: 'password',
                                            allowBlank: false,
                                            maxLength: 16,
                                            minLength: 6,
                                            inputType: 'password'
                                        }, {
                                            fieldLabel: 'Confirm password',
                                            id: 'passwordConfirm',
                                            name: 'passwordConfirm',
                                            inputType: 'password',
                                            vtype: 'password',
                                            allowBlank: false,
                                            initialPassField: 'password'

                                        }, {
                                            xtype: 'recaptcha',
                                            name: 'recaptcha',
                                            id: 'captcha',
                                            publickey: Gemma.RECAPTCHA_PUBLIC_KEY,
                                            theme: 'white',
                                            lang: 'en',
                                            allowBlank: false

                                        }, {
                                            id: 'ajaxRegisterTrue',
                                            name: 'ajaxRegisterTrue',
                                            hidden: true,
                                            value: 'true'

                                        }],

                                    buttons: [{
                                        text: "Cancel",
                                        handler: this.cancel,
                                        scope: this
                                    }, {
                                        text: 'Submit',
                                        formBind: true, // use with monitorValid in Ext.FormPanel for client side validation
                                        handler: this.submitHandler
                                    }],
                                    bbar: new Ext.ux.StatusBar(
                                        {
                                            id: 'my-status_ajaxRegister',
                                            text: '',
                                            iconCls: 'default-icon',
                                            busyText: 'Validating...',
                                            items: ['<div id="ajax-error_ajaxRegister" style="color: red; vertical-align: top; padding-right: 5px;"><br/></div>']
                                        })
                                })]
                            // end items

                        });// end Ext.apply

                this.addEvents('register_cancelled', 'register_success');

                Gemma.AjaxLogin.AjaxRegister.superclass.initComponent.call(this, arguments);
            },

            submitHandler: function () {

                var errordiv = Ext.get('ajax-error_ajaxRegister');
                Ext.DomHelper.overwrite(errordiv, "");

                signup = Ext.getCmp("_registerForm");

                if (!signup.getForm().isValid()) {
                    var erdiv = Ext.get('ajax-error_ajaxRegister');
                    Ext.DomHelper.overwrite(erdiv, Gemma.HelpText.CommonErrors.InvalidForm.text);
                    return;
                }

                signup.getForm().submit(
                    {
                        url: this.url,
                        method: 'POST',
                        success: function () {

                            var sb = Ext.getCmp('my-status_ajaxRegister');
                            sb.clearStatus();

                            Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.AjaxLogin_AjaxRegister.successTitle,
                                Gemma.HelpText.WidgetDefaults.AjaxLogin_AjaxRegister.successText);

                            var registerWidget = Ext.getCmp("_ajaxRegister");
                            registerWidget.fireEvent("register_success");

                        },
                        failure: function (form, action) {
                            var msg;
                            if (action.failureType === 'client') {
                                msg = "Invalid form";
                            } else {

                                var errMsg = Ext.util.JSON.decode(action.response.responseText);
                                msg = errMsg.message;
                            }

                            var erdiv = Ext.get('ajax-error_ajaxRegister');
                            Ext.DomHelper.overwrite(erdiv, msg);

                            Ext.getCmp('captcha').reset();
                            Ext.getCmp('password').reset();
                            Ext.getCmp('passwordConfirm').reset();

                            Ext.getCmp('my-status_ajaxRegister').clearStatus();
                        }
                    });

                var sb = Ext.getCmp('my-status_ajaxRegister');
                sb.showBusy();

            },

            cancel: function () {
                this.fireEvent('register_cancelled');

            }

        });

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
Ext.ux.Recaptcha = Ext.extend(Ext.form.Field, {

    lang: 'en',

    fieldLabel: "Prove you are human",

    theme: 'white',

    width: 310,
    height: '100%',
    fieldClass: '',

    /**
     * @memberOf Ext.ux.Recaptcha
     */
    reset: function () {
        Recaptcha.reload();
    },

    destroy: function () {
        Ext.ux.Recaptcha.superclass.destroy.call(this);
        Recaptcha.destroy();
    },

    validateValue: function () {
        var response = Ext.get('g-recaptcha-response');
        if (response != null && response.getValue().length > 0) {
            return true;
        }
        this.markInvalid("Recaptcha must be filled in");
        return false;
    },

    filterValidation: function (e) {
        if (!e.isNavKeyPress()) {
            this.validationTask.delay(this.validationDelay);
        }
    },

    allowBlank: false,

    onRender: function (ct, position) {

        if (!this.el) {

            this.el = document.createElement('div');
            this.el.id = this.getId();

            grecaptcha.render(this.el, {
                'sitekey': this.publickey,
                'theme': this.theme
            });

        }

        Ext.ux.Recaptcha.superclass.onRender.call(this, ct, position);

    },
    initComponent: function () {
        Ext.ux.Recaptcha.superclass.initComponent.call(this);
        this.addEvents('keyup');
    },

    initEvents: function () {
        Ext.ux.Recaptcha.superclass.initEvents.call(this);
        this.validationTask = new Ext.util.DelayedTask(this.validate, this);
        this.mon(this.el, 'keyup', this.filterValidation, this);
    }

});

Ext.reg('recaptcha', Ext.ux.Recaptcha);


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
