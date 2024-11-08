<%@ include file="/common/taglibs.jsp"%>
<head>
<title>Meta-analysis Manager</title>

<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />
<Gemma:script src='/scripts/app/eeDataFetch.js' />
</head>

<script type="text/javascript">
   Ext.namespace('Gemma');
   Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
   Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

   Ext.onReady(function() {
      Ext.QuickTips.init();
      Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

      new Gemma.GemmaViewPort({ centerPanelConfig : new Gemma.MetaAnalysisManagerGridPanel() });
   });
</script>


<input type="hidden" id="reloadOnLogin" value="false" />
<input type="hidden" id="reloadOnLogout" value="false" />

<%-- The function fetchDiffExpressionData(analysisId) in eeDataFetch.js requires "messages" to be defined. --%>
<div id="messages"></div>
