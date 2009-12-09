<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Manage groups</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/manageGroups.js' useRandomParam='false' />
</head>
<body>
	<h2>
		Manage groups
	</h2>

	<p>
		A 'Group' is a set of Gemma users who have a common set of permissions. This page allows you to see what Groups you
		belong to, create groups, and change who is in groups you control.
	</p>
	<p>
		For additional controls on which groups can view or edit your data sets, visit the
		<a href="<c:url value="/expressionExperiment/showAllExpressionExperimentLinkSummaries.html" />">Data Manager</a>.
	</p>


	<div id='manageGroups-div'>
	</div>
	<div id='errorMessage' style='width: 500px; margin-bottom: 1em;'></div>
</body>