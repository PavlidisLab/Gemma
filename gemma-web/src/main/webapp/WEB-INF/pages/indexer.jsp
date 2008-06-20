<%@ include file="/common/taglibs.jsp"%>

<head>
	<title> Index Gemma </title>

	<script type="text/javascript" src="<c:url value='/scripts/progressbar.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/CustomCompassIndexController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/TaskCompletionController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ProgressStatusService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	
	<script type="text/javascript" src="<c:url value='/scripts/ajax/indexer.js'/>"></script>
	
	<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/progressbar.css'/>" />

</head>


<body>

<p>
Choose the indexing options that are appropriate and then click index.
</p>

	<authz:authorize ifAnyGranted="admin">

		<div id="index-form"></div>
		<div id="messages" style="margin: 10px; width: 400px"></div>
		<div id="taskId" style="display: none;"></div>
		<div id="progress-area" style="padding: 5px;"></div>
		<br />


	</authz:authorize>
</body>



