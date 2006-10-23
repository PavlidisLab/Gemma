<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="login.title" />
	</title>
	<content tag="heading">
	<fmt:message key="login.heading" />
	</content>
	<meta name="menu" content="Login" />
</head>
<body id="login" />

	<p>Note that logging in is not necessary for regular users. Return to the <a href="<c:url value="/mainMenu.html"/>">Main menu</a> to start using Gemma.</p>

	<%-- Include the login form --%>
	<c:import url="/WEB-INF/pages/loginForm.jsp" />
	<%-- Password hint disabled
	<p>
		<fmt:message key="login.passwordHint" />
	</p>
	--%>
</body>


