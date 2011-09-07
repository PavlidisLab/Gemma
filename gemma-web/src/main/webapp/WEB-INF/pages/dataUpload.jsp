<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression data upload</title>

	<jwr:script src='/scripts/ajax/util/FileUploadForm.js' useRandomParam="false" />

	<security:authorize access="hasAnyRole('GROUP_USER','GROUP_ADMIN')">
		<jwr:script src='/scripts/app/UserExpressionDataUpload.js' useRandomParam="false" />
	</security:authorize>
</head>

<body>

	<security:authorize access="hasAnyRole('GROUP_USER','GROUP_ADMIN')">
		<div id="messages"></div>
		<div id="form"></div>
		<div id="progress-area" style="margin: 20px; padding: 5px;"></div>
	</security:authorize>

	<security:authorize access="!hasAnyRole('GROUP_USER','GROUP_ADMIN')">
		<script type="text/javascript">
			Gemma.AjaxLogin.showLoginWindowFn(true);
		</script>
		<p>
			Sorry, to upload data you must
			<a href="/Gemma/login.jsp">login</a> or
			<a href="<c:url value="/Gemma/register.html" />">register</a>.
		</p>
	</security:authorize>

</body>
