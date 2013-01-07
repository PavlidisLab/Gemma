<%@ include file="/common/taglibs.jsp"%>
<head>
    <title>Meta-analysis Manager</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	
	<%-- To let users download all differential expression data for an analysis,
		 the function fetchDiffExpressionData(analysisId) in eeDataFetch.js is required. --%>
	<jwr:script src='/scripts/app/eeDataFetch.js' />
	
	<script type="text/javascript">
		Ext.namespace('Gemma');
	
		Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	
		Ext.onReady(function() {
			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
	
			new Gemma.GemmaViewPort({
				centerPanelConfig: new Gemma.MetaAnalysisManagerGridPanel()
			});
		});
	</script>
</head>

<body>
	<input type="hidden" id="reloadOnLogin" value="false"/>  
	<input type="hidden" id="reloadOnLogout" value="false" />
	
	<%-- The function fetchDiffExpressionData(analysisId) in eeDataFetch.js requires "messages" to be defined. --%>
	<div id="messages"></div>	
</body>