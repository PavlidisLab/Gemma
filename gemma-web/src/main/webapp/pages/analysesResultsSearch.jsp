<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Search Analyses Results</title>
<meta name="keywords" content="coexpression,differential expression">
<Gemma:script src='/scripts/app/eeDataFetch.js' />
</head>

<div id="browser-warnings"></div>

<script>
Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

Ext.onReady( function() {
   Ext.QuickTips.init();

   new Gemma.GemmaViewPort( {
      centerPanelConfig : new Gemma.AnalysisResultsSearch()
   } );
} );
</script>
