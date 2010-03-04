<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression Experiment Link Summary</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/ExpressionExperimentManage.js' />

</head>

<body>
	<div id="messages" style="margin: 10px; width: 400px"></div>
	<div id="controls"></div>
	<div id="eemanage"></div>
	<div id="taskId" style="display: none;"></div>
	<div id="progress-area" style="padding: 15px;"></div>
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<div id="updateAllReports-area"></div>
	</security:authorize>
	
</body>