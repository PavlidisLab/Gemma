<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<head>
<title><fmt:message key="login.title"/></title>
<content tag="heading"><fmt:message key="login.heading"/></content>
</head>
<body id="login"/>

<%-- programmatically enforces SSL security --%>
<%--<Gemma:secure mode="secured"/>--%>

<c:import url="/loginMenu.jsp"/>

<p><fmt:message key="welcome.message"/></p>

<%-- Include the login form --%>
<c:import url="/WEB-INF/pages/loginForm.jsp"/>

<p><fmt:message key="login.passwordHint"/></p>
</body>

