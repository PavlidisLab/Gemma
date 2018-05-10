Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = ctxBasePath + '/images/default/s.gif';

/**
 * @author keshav
 *
 */
Ext.onReady(function () {

    this.ajaxRegister = new Gemma.AjaxLogin.AjaxRegister( {
        name : 'ajaxRegister',
        closable : false,
        closeAction : 'hide',
        title : 'Please Register'
    } );

    this.ajaxRegister.show();

});
