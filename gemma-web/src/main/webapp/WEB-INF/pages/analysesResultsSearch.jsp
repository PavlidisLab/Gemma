
<html>
  <head>
    <title>Search Analyses Results</title>
	<meta http-equiv="keywords" content="coexpression,differential expression">
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js'
		useRandomParam="false" />
	<jwr:script src='/scripts/app/eeDataFetch.js' useRandomParam="false" />

		
				
		<!-- JSON support for IE (needed to use JS API) -->
		<script type="text/javascript" src="/Gemma/scripts/cytoscapev8/js/min/json2.min.js"></script>
		        
		<!-- Flash embedding utility (needed to embed Cytoscape Web) -->
		<script type="text/javascript" src="/Gemma/scripts/cytoscapev8/js/min/AC_OETags.min.js"></script>
		        
		<!-- Cytoscape Web JS API (needed to reference org.cytoscapeweb.Visualization) -->
		<script type="text/javascript" src="/Gemma/scripts/cytoscapev8/js/min/cytoscapeweb.min.js"></script>
		
		

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

<div id="browser-warnings"></div>
</html>
