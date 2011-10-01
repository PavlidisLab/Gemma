<%@ include file="/common/taglibs.jsp"%>
<head>
<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />

<script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();
	// need wrapper panel because tabPanels can't have title headers (it's used for tabs)
	new Gemma.GemmaViewPort({
		 	centerPanelConfig: new Ext.Panel({
		 		items:[
		 			new Gemma.GenePage({
		 				geneId:  Ext.get("geneId").getValue(),
		 				geneSymbol: Ext.get("geneSymbol").getValue() 
			 	})],
			 	layout:'fit', 
			 	title:Ext.get("geneSymbol").getValue()
			 })
		 });
});
</script>

	<title><c:if test="${not empty geneOfficialSymbol}">
			${geneOfficialSymbol}
		</c:if> <fmt:message key="gene.details" />
	</title>
</head>

<body>

	<input type="hidden" name="geneId" id="geneId" value="${geneId}" />
	<input type="hidden" name="geneSymbol" id="geneSymbol" value="${geneOfficialSymbol}" />
	
	<div id="newGenePageWidget"></div>