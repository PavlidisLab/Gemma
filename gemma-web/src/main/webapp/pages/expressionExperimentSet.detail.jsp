<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>${eeSetName} Details</title>
	<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />
</head>
		 
	<script>
	Ext.namespace('Gemma');
	Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
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

<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true"/>

<div spellcheck="false">
	<div id="messages"></div>
</div>
