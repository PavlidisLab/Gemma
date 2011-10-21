Ext.namespace('Gemma');

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {

	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	
	var phenotypesPanel = new Gemma.PhenotypesPanel({
		useGemmaDefaults: true
	});

	new Gemma.GemmaViewPort({
	 	centerPanelConfig: phenotypesPanel 
	});
});
