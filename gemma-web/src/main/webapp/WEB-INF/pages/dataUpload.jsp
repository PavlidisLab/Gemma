<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression data upload</title>

	<jwr:script src='/scripts/ajax/util/FileUploadForm.js' useRandomParam="false" />

	<authz:authorize ifAnyGranted="user,admin">
		<jwr:script src='/scripts/app/UserExpressionDataUpload.js' useRandomParam="false" />
	</authz:authorize>
</head>

<body>

	<authz:authorize ifAnyGranted="user,admin">
		<div id="messages"></div>
		<div id="form"></div>
		<div id="progress-area" style="margin: 20px; padding: 5px;"></div>
	</authz:authorize>

	<authz:authorize ifNotGranted="user,admin">
		<p>
			Sorry, to upload data you must
			<a href="/Gemma/login.jsp">login</a> or
			<a href="<c:url value="/Gemma/register.html" />">register</a>.
		</p>
	</authz:authorize>

</body>
