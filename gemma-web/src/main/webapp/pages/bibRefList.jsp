<%@ include file="/common/taglibs.jsp"%>
<head>
<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />

<script type="text/javascript">
Ext.namespace('Gemma');
Ext.onReady(function() {
	Ext.QuickTips.init();
	new Gemma.GemmaViewPort({
		 	centerPanelConfig: new Gemma.BibliographicReference.Browser()
		 });
});
</script>
	<title>Bibliographic References</title>
</head>
