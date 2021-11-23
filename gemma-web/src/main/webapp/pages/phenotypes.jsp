<%@ include file="/common/taglibs.jsp"%>
<head>
    <title><fmt:message key="phenotypes.title" /></title>

	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
	
	<script type="text/javascript">
		Ext.namespace('Gemma');
	
		Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
	
		Ext.onReady(function() {
	
			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
	
			new Gemma.GemmaViewPort({
			 	centerPanelConfig: new Gemma.PhenotypePanel() 
			});
		});
	</script>
</head>

<body>
	<input type="hidden" name="phenotypeUrlId" id="phenotypeUrlId" value="<c:out value="${phenotypeUrlId}"/>" />
	<input type="hidden" name="geneId" id="geneId" value="<c:out value="${geneId}"/>" />
	<input type="hidden" name="ncbiId" id="ncbiId" value="<c:out value="${ncbiId}"/>" />
	<input type="hidden" id="reloadOnLogin" value="false"/>  
	<input type="hidden" id="reloadOnLogout" value="false" />
</body>