<%@ include file="/common/taglibs.jsp"%>

<html>
	<head>
		<title>Wait...</title>
		<script type='text/javascript' src='/Gemma/dwr/interface/HttpProgressMonitor.js'> </script>
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
		<script type='text/javascript' src='/Gemma/scripts/progressbar.js'></script>
		<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />
	</head>
	<body>

		<h1>
			Please wait...
		</h1>
		<script type="text/javascript">
			createIndeterminateProgressBar();
			startProgress();
		</script>


		<form method="post" action="<c:url value='/processDelete.html'/>">			
			<input type="submit" value="cancel" />
		</form>



	</body>
</html>




