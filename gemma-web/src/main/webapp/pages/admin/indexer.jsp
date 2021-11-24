<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Index Gemma</title>

	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/indexer.js' />

</head>


<body>

	<security:authorize access="hasRole('GROUP_ADMIN')">

		<p>
			Choose the indexing options that are appropriate and then click
			index.
		</p>

		<div id="index-form"></div>
		<div id="messages" style="margin: 10px; width: 400px"></div>
		<div id="taskId" style="display: none;"></div>
		<div id="progress-area" style="padding: 5px;"></div>
		<br />

	</security:authorize>

</body>



