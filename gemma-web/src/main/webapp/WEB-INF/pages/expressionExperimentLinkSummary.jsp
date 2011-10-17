<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression data manager</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/ExpressionExperimentManage.js' />

</head>

<body>
	<div id="messages" style="margin: 10px; width: 400px"></div>
	<p>
		Use this page to view and manage expression experiments you have access to.
	</p>
	<security:authorize access="hasRole('GROUP_ADMIN')">
		<div id="updateAllReports-area"></div>
	</security:authorize>
	<div id="controls"></div>
	<div id="eemanage"></div>
	<div id="taskId" style="display: none;"></div>
	<div id="progress-area" style="padding: 15px;"></div>

</body>