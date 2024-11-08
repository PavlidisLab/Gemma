<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>${geneSetName} Details</title>
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
		 			new Gemma.GeneSetPage( {
						geneSetId: Ext.get("geneSetId").getValue()
				})],
			 	layout:'fit', 
			 	title: 'Gene Group: \"'+Ext.get("geneSetName").getValue()+'\"'
			 })
		});
	});
	
</script>

<input id="geneSetId" type="hidden" value="${geneSetId}" />
<input id="geneSetName" type="hidden" value="${geneSetName}" />


<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true"/>

<div spellcheck="false">
	<div id="messages"></div>
</div>
