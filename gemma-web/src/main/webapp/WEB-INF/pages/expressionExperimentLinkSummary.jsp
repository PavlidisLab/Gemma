<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression Experiment Link Summary</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/ExpressionExperimentManage.js' />

</head>

<body>
	<security:authorize ifAnyGranted="admin,user">
		<div id="messages" style="margin: 10px; width: 400px"></div>

		<div id="eemanage"></div>
		<div id="taskId" style="display: none;"></div>
		<div id="progress-area" style="padding: 15px;"></div>
	</security:authorize>
	<security:authorize ifAnyGranted="admin">
		<div id="updateAllReports-area"></div>
	</security:authorize>
	
<security:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</security:authorize>

	
</body>