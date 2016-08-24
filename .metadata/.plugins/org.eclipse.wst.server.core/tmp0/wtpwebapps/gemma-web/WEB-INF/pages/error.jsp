<%@ include file="/common/taglibs.jsp"%>

<%@ page language="java" isErrorPage="true"%>

<title><fmt:message key="errorPage.title" /></title>
<input type="hidden" id="reloadOnLogin" value="true" />

<div style="padding: 20px;">
	<h2>
		<fmt:message key="errorPage.heading" />
	</h2>

	<c:choose>
		<c:when test="${not empty param.exception}">
			<p>${param.exception.message}</p>

			<p>Possible next steps:
			<ul>
				<li>Go back and try what you were doing again</li>
				<li>Do you need to log in?</li>
				<li><a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
			</ul>

			</p>

			<security:authorize access="hasRole('GROUP_ADMIN')">
				<Gemma:exception exception="${exception}" />
			</security:authorize>
		</c:when>
		<c:when test="${not empty requestScope['javax.servlet.error.exception']}">

			<p>${requestScope['javax.servlet.error.exception'].message}</p>

			<p>Possible next steps:
			<ul>
				<li>Go back and try what you were doing again</li>
				<li>Do you need to log in?</li>
				<li><a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
			</ul>
			</p>

			<security:authorize access="hasRole('GROUP_ADMIN')">
				<%-- this is causing stackoverflow errors ... no idea why, since upgrading to spring 3.2 from 3.0.7 --%>
				<Gemma:exception exception="${requestScope['javax.servlet.error.exception']}" />
			</security:authorize>
		</c:when>

		<c:when test="${not empty requestScope['exception']}">
			<p>${requestScope['exception'].message}</p>

			<p>Possible next steps:
			<ul>
				<li>Go back and try what you were doing again</li>
				<li>Do you need to log in?</li>
				<li><a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
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

			<p>Possible next steps:
			<ul>
				<li>Go back and try what you were doing again</li>
				<li>Do you need to log in?</li>
				<li><a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
			</ul>
			</p>


		</c:otherwise>

	</c:choose>
</div>
