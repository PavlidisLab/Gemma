<%@ include file="/common/taglibs.jsp"%>
<%-- DEPRECATED, please use form-page-embedded ajaxified progress bar instead; see loadExpressionExperiment.js for some pointers. --%>

<title><fmt:message key="processProgress.title" />
</title>
<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />

<h1>
	<fmt:message key="processProgress.title" />
</h1>
<script type="text/javascript">
	 
		Ext.onReady(function () {
		var p = new progressbar();
	 	p.createIndeterminateProgressBar();
		p.on('fail', function(error){ alert(error); });
		p.on('cancel',function(cancel){alert(cancel); });
	 	p.startProgress(); });
		</script>

<input type='hidden' name='taskId' id='taskId' value='<c:out value="${taskid}"/>' />
<div id="messages" style="margin: 10px; width: 400px"></div>
<div id="progress-area" style="padding: 5px;"></div>






