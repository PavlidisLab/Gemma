<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>My Expression Experiments</title>
	<jwr:script src='/scripts/api/ext/data/DwrProxy.js'
		useRandomParam="false" />

</head>
	<script>
	Ext.namespace('Gemma');
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	Ext.onReady( function() {
		Ext.QuickTips.init();

		// need wrapper panel because tabPanels can't have title headers (it's used for tabs)
		new Gemma.GemmaViewPort({
		 	centerPanelConfig: new Ext.Panel({
		 		items:[new Gemma.MyDatasetsPanel()],
			 	layout:'fit'
			 })
		});
	});
	
</script>
<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true"/>
