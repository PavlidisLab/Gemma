<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="login.title" /></title>
	<content tag="heading">
	<fmt:message key="login.heading" />
	</content>
	<meta name="menu" content="Login" />
	<link rel="stylesheet" type="text/css" media="all"
		href="<c:url value='/styles/${appConfig["theme"]}/layout-2col.css'/>" />
</head>
<body id="login" />

	<%-- programmatically enforces SSL security --%>
	<%--<Gemma:secure mode="secured"/>--%>

	<%-- Include the login form --%>
	<c:import url="/WEB-INF/pages/loginForm.jsp" />
	<p>
		<fmt:message key="login.passwordHint" />
	</p>
</body>


