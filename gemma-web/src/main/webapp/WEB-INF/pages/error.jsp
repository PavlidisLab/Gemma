<%@ include file="/common/taglibs.jsp"%>

<%@ page language="java" isErrorPage="true"%>
<%--     This line causes an error in some versions of tomcat (versions previous to tomcat 5.5  --%>

<title><fmt:message key="errorPage.title" /></title>
<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />

<h1>
	<fmt:message key="errorPage.heading" />
</h1>

<%@ include file="/common/messages.jsp"%>


<c:choose>
	<c:when test="${not empty param.exception}">
		<p>
			${param.exception.message}
		</p>

		<p>
			Possible next steps: Try what you were doing again; Go to the
			<a href='/Gemma/'>home page</a>;
			<a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.
		</p>

		<security:authorize ifAllGranted="GROUP_ADMIN">
			<Gemma:exception exception="${exception}" />
		</security:authorize>
	</c:when>
	<c:when test="${not empty requestScope['javax.servlet.error.exception']}">

		<p>
			${requestScope['javax.servlet.error.exception'].message}
		</p>

		<p>
			Possible next steps: Try what you were doing again; Go to the
			<a href='/Gemma/'>home page</a>;
			<a href="mailto:gemma@chibi.ubc.ca?subject=${requestScope['javax.servlet.error.exception'].message}">Email us</a>
			about the problem.
		</p>


		<security:authorize ifAllGranted="GROUP_ADMIN">
			<Gemma:exception exception="${requestScope['javax.servlet.error.exception']}" />
		</security:authorize>
	</c:when>

	<c:when test="${not empty requestScope['exception']}">
		<p>
			${requestScope['exception'].message}
		</p>

		<p>
			Possible next steps: Try what you were doing again; Go to the
			<a href='/Gemma/'>home page</a>;
			<a href="mailto:gemma@chibi.ubc.ca?subject=${requestScope['exception'].message}">Email us</a> about the problem.
		</p>


		<security:authorize ifAllGranted="GROUP_ADMIN">
			<Gemma:exception exception="${requestScope['exception']}" />
		</security:authorize>
	</c:when>
	<c:otherwise>
		<p>
			<fmt:message key="errorPage.info.missing" />
		</p>

		<p>
			Possible next steps: Try what you were doing again; Go to the
			<a href='/Gemma/'>home page</a>;
			<a href="mailto:gemma@chibi.ubc.ca?subject=Unknown%20error">Email us</a> about the problem.
		</p>
	</c:otherwise>

</c:choose>

