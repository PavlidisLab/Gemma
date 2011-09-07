
<%@ include file="/common/taglibs.jsp"%>
<%-- 

Display table of expression experiments.
$Id$ 
--%>
<head>
	<title><fmt:message key="expressionExperiments.title" />
	</title>
</head>

<div id="messages" style="margin: 10px; width: 400px">
	${message}
</div>
<div id="taskId" style="display: none;"></div>
<div id="progress-area" style="padding: 15px;"></div>

<script type="text/javascript">
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider( ));
	Ext.onReady(function(){
		Ext.QuickTips.init();
		//new Gemma.ExperimentPagingGrid({renderTo:'eepage'});
		new Gemma.GemmaViewPort({
		 	centerPanelConfig: new Gemma.ExperimentPagingGrid() 
		});
	});


</script>
<input type="hidden" id="dontReloadOnLogout" value="true">
<div id="eepage"></div>