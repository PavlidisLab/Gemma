<%@ include file="/common/taglibs.jsp"%>

<html>
	<head>
		<title>Wait...</title>
		<script type='text/javascript'
			src='/Gemma/dwr/interface/HttpProgressMonitor.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
		<script type='text/javascript'
			src="<c:url value="scripts/indeterminateProgress.js"/>"></script>
		<style type="text/css"> 
			#progressBar { padding-top: 5px; }
			#progressBarBox { width: 350px; height: 20px; border: 1px inset; background: #EEEEEE;}
			#progressBarBoxContent { width: 0; height: 20px; border-right: 1px solid #444444; background: #9ACB34; } 

</style>
	</head>
	<body>

		<h1>
			Please wait...
		</h1>

		<div id="progressBar" style="display:none;">
			<div id="theMeter">
				<div id="progressBarText">
					<textarea id="progressTextArea" name="" rows=5 cols=60
						readonly=true> </textarea
				</div>
				<div id="progressBarBox">
					<div id="progressBarBoxContent"></div>
				</div>
			</div>
		</div>


		<script type="text/javascript">
	startProgress();
</script>



	</body>
</html>




