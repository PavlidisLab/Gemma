<%@ include file="/common/taglibs.jsp"%>

<html>
	<head>
		<title>Wait...</title>
		<script type='text/javascript'
			src='/Gemma/dwr/interface/HttpProgressMonitor.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
		<script type='text/javascript' src="<c:url value="scripts/indeterminateProgress.js"/>"></script>
		<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
	</head>
	<body>

		<h1>
			Please wait...
		</h1>

		<div id="progressBar" style="display:none;">
			<div id="theMeter">
				<div id="progressBarText">
					<textarea id="progressTextArea" name="" rows=5 cols=60 readonly=true> </textarea>
				</div>
				<div id="progressBarBox">
					<div id="progressBarBoxContent"></div>
				</div>
			</div>
		</div>

		<form>
			<input type="hidden" name="taskId" />
		</form>

		<form method="post" action="<c:url value="/processDelete.html"/>">			
			<input type="submit" value="cancel" />
		</form>

<script type="text/javascript">
	startProgress();
</script>



	</body>
</html>




