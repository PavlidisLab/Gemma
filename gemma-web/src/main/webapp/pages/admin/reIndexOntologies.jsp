<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Index Gemma</title>
	<jwr:script src='/scripts/app/ontologyReIndexer.js' />
</head>


<body>
<div style="padding-left:20px">

	<security:authorize access="hasAuthority('GROUP_ADMIN')">
		<p>
			Click below to reinitialize Gemma's Jena database and all its ontology indicies.
		</p>

		<div id="reinitializeOntologyIndices-form"></div>
		<div id="messages" style="margin: 10px; width: 400px"></div>
		<div id="taskId" style="display: none;"></div>
		<div id="progress-area" style="padding: 5px;"></div>
		<br />

	</security:authorize>
</div>
</body>



