<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>${eeSetName} Details</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' useRandomParam="false" />
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
		 		items:[
		 			new Gemma.ExpressionExperimentSetPage( {
						eeSetId: Ext.get("eeSetId").getValue()
				})],
			 	layout:'fit', 
			 	title: Ext.get("eeSetName").getValue()
			 })
		});
	});
	
</script>

<input id="eeSetId" type="hidden" value="${eeSetId}" />
<input id="eeSetName" type="hidden" value="${eeSetName}" />

<input type="hidden" id="dontReloadOnLogout" value="false">
<input type="hidden" id="reloadOnLogin" value="true"/>

<div spellcheck="false">
	<div id="messages"></div>
</div>
