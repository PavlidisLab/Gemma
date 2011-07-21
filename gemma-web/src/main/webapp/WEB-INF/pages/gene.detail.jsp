<%@ include file="/common/taglibs.jsp"%>
<head>
<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />

<script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();
	
	new Gemma.GenePage( {
			renderTo : 'newGenePageWidget',
			geneId: Ext.get("gene").getValue()
		});
});
</script>

	<title><c:if test="${not empty gene.officialSymbol}">
			${gene.officialSymbol}
		</c:if> <fmt:message key="gene.details" />
	</title>
</head>

<body>

	<input type="hidden" name="gene" id="gene" value="${gene.id}" />
	<input type="hidden" name="geneName" id="geneName" value="${gene.name}" />
	<input type="hidden" name="taxon" id="taxon" value="${gene.taxon.id}" />
	
	<div id="newGenePageWidget"></div>