<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Expression Experiments with QC Issues</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/ajax/entities/experiment/ExpressionExperimentQCGrid.js' />
</head>
<body>

	<input type="hidden" id="reloadOnLogout" value="true">
	<input type="hidden" id="reloadOnLogin" value="true"/>

	<script type="text/javascript">
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
			
			Ext.onReady(function() {
			
				Ext.QuickTips.init();
				
				new Gemma.GemmaViewPort( {
					centerPanelConfig : new Gemma.ExpressionExperimentQCGrid({
							header: true,
							experimentNameAsLink: true
						}) 
				});
			
			});
	</script>
		
	<div id='errorMessage' style='width: 500px; margin-bottom: 1em;'></div>

</body>