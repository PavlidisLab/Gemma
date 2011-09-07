
<html>
  <head>
    <title>Search Analyses Results</title>
	<meta http-equiv="keywords" content="coexpression,differential expression">
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js'
		useRandomParam="false" />
	<jwr:script src='/scripts/app/eeDataFetch.js' useRandomParam="false" />

</head>
		 
	<script>
	Ext.namespace('Gemma');
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	Ext.onReady( function() {
		Ext.QuickTips.init();
		
		new Gemma.GemmaViewPort({
		 	centerPanelConfig: new Gemma.AnalysisResultsSearch()
		});
	});
	
</script>

</html>
