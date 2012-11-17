<%@ include file="/common/taglibs.jsp"%>
<head>
    <title>External Databases Statistics</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	
	<script type="text/javascript">
	Ext.namespace('Gemma');
	
		Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	
		Ext.onReady(function() {
	
			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
	
			new Gemma.GemmaViewPort({
				centerPanelConfig: new Gemma.ExternalDatabasesStatistics()
			});
		});
	</script>
</head>

<body>
	<input type="hidden" id="reloadOnLogin" value="true"/>  
	<input type="hidden" id="reloadOnLogout" value="true" />
</body>