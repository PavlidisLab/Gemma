<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression data upload</title>

	<Gemma:script src='/scripts/api/util/FileUploadForm.js' />

	<security:authorize access="hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
		<Gemma:script src='/scripts/app/UserExpressionDataUpload.js' />
	</security:authorize>
</head>

<body>

	<security:authorize access="hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
		<div id="messages"></div>
		<div id="form"></div>
		<div id="progress-area" style="margin: 20px; padding: 5px;"></div>
	</security:authorize>

	<security:authorize access="!hasAnyAuthority('GROUP_USER','GROUP_ADMIN')">
		<script type="text/javascript">
			Gemma.AjaxLogin.showLoginWindowFn(true);
		</script>
		<p>
			Sorry, to upload data you must
			<a href="${pageContext.request.contextPath}/login.jsp">login</a> or
			<a href="${pageContext.request.contextPath}/register.html">register</a>.
		</p>
	</security:authorize>

</body>
