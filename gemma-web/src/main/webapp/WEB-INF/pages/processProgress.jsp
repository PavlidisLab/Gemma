<%@ include file="/common/taglibs.jsp"%>


		<title> <fmt:message key="processProgress.title" /> </title>
		
		<script type='text/javascript' src='/Gemma/dwr/interface/HttpProgressMonitor.js'> </script>
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
		<script type='text/javascript' src='/Gemma/scripts/progressbar.js'></script>
		<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />

		<h1>
			<fmt:message key="processProgress.title" />
		</h1>
		<script type="text/javascript">
			createIndeterminateProgressBar();
			startProgress();
		</script>

		

		<input type = "hidden" name="taskId" id="taskId" value= "${taskId}"/> 
		<form method="post" action="<c:url value='/processDelete.html?taskId=' + ${taskId} />">			
			<input type="submit" value="cancel" />
		</form>





