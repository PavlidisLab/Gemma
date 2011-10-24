<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>
<head>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/arrayDesign.js' />
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

<h1>
	Platforms
</h1>
<title>Platforms</title>

