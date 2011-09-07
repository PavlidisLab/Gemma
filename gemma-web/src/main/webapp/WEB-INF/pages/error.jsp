<%@ include file="/common/taglibs.jsp"%>

<%@ page language="java" isErrorPage="true"%>
<%--     This line causes an error in some versions of tomcat (versions previous to tomcat 5.5  --%>

<title><fmt:message key="errorPage.title" /></title>
<link rel="stylesheet" type="text/css" media="all" href="<c:url value='/styles/${appConfig["theme"]}/theme.css'/>" />

<h1>
	<fmt:message key="errorPage.heading" />
</h1>

<%@ include file="/common/messages.jsp"%>

<input type="hidden" id="reloadOnLogin" value="true"/>

<c:choose>
	<c:when test="${not empty param.exception}">
		<p>
			${param.exception.message}
		</p>

		<p>
			Possible next steps: 
			<ul>
			<li>Try what you were doing again</li>
			<li>Log in</li>
			<li>Go to the <a href='/Gemma/'>home page</a></li>
			<li><a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
			</ul>
			
		</p>

		<security:authorize access="hasRole('GROUP_ADMIN')">
			<Gemma:exception exception="${exception}" />
		</security:authorize>
	</c:when>
	<c:when test="${not empty requestScope['javax.servlet.error.exception']}">

		<p>
			${requestScope['javax.servlet.error.exception'].message}
		</p>

		<p>
			Possible next steps: 
			<ul>
			<li>- Try what you were doing again</li>
			<li>- Log in</li>
			<li>- Go to the <a href='/Gemma/'>home page</a></li>
			<li>- <a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
			</ul>
		</p>


		<security:authorize access="hasRole('GROUP_ADMIN')">
			<Gemma:exception exception="${requestScope['javax.servlet.error.exception']}" />
		</security:authorize>
	</c:when>

	<c:when test="${not empty requestScope['exception']}">
		<p>
			${requestScope['exception'].message}
		</p>

		<p>
			Possible next steps: 
			<ul>
			<li>- Try what you were doing again</li>
			<li>- Log in</li>
			<li>- Go to the <a href='/Gemma/'>home page</a></li>
			<li>- <a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
			</ul>	
		</p>


		<security:authorize access="hasRole('GROUP_ADMIN')">
			<Gemma:exception exception="${requestScope['exception']}" />
		</security:authorize>
	</c:when>
	<c:otherwise>
		<p>
			<fmt:message key="errorPage.info.missing" />
		</p>

		<p>
			Possible next steps: 
			<ul>
			<li>- Try what you were doing again</li>
			<li>- Log in</li>
			<li>- Go to the <a href='/Gemma/'>home page</a></li>
			<li>- <a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
			</ul>
		</p>
	</c:otherwise>

</c:choose>

