<%@ include file="/common/taglibs.jsp"%>
<%-- DEPRECATED, please use form-page-embedded ajaxified progress bar instead; see loadExpressionExperiment.js for some pointers. --%>

<title><fmt:message key="processProgress.title" /></title>
<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>

	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'> </script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/scripts/progressbar.js'></script>
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />

<h1>
	<fmt:message key="processProgress.title" />
</h1>
<script type="text/javascript">
	 
		Ext.onReady(function () {
		var p = new progressbar();
	 	p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('cancel', reset);
	 	p.startProgress(); });
		</script>

<input type = 'hidden' name='taskId' id='taskId' value= '${taskid }'/>
<div id="messages" style="margin:10px;width:400px"></div>
<div id="progress-area" style="padding:5px;"></div>






