<%@ include file="/common/taglibs.jsp"%>
<head>
<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />

<script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();

	new Gemma.GemmaViewPort({
		 	centerPanelConfig: new Gemma.GenePage({
		 		geneId:  Ext.get("geneId").getValue()
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
	
	<div id="newGenePageWidget"></div>