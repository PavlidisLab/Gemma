
<%@ include file="/common/taglibs.jsp"%>
<head>
	<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />
</head>


<script type="text/javascript">
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider( ));
	Ext.onReady(function(){
		Ext.QuickTips.init();
		new Gemma.GemmaViewPort({
		 	centerPanelConfig: new Gemma.ArrayDesignsNonPagingGrid()
		});
	});


</script>
<input type="hidden" id="reloadOnLogout" value="false">
<input type="hidden" id="reloadOnLogin" value="true"/>

<title>Platforms</title>

