Ext.namespace('Gemma');

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {

	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	new Gemma.GemmaViewPort({
	 	centerPanelConfig: new Gemma.PhenotypePanel() 
	});
});
